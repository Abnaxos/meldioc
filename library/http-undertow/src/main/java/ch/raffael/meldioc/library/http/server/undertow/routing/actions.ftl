[#-- Copyright (c) 2021 Raffael Herzog

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to
deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.
--]

[#import "/parameters.ftl" as p]
[#import "/codegen.ftl" as c]
[#import "actions-old.ftl" as a]

[#macro dispatch_methods in_body out_body]
  [#if in_body]
    [#local param_counts = 1..p.pcount]
  [#else]
    [#local param_counts = 0..p.pcount]
  [/#if]
  [#list param_counts as i]

    [#if in_body]
      [#local first_type_param = "B"]
      [#local np=i-1]
    [#else]
      [#local first_type_param = ""]
      [#local np=i]
    [/#if]

    [#local args = []]
    [#if in_body]
      [#local args = args+["b"]]
    [/#if]
    [#local args = args + (1..*np)?map(i -> "p"+(i?string)+".get(x)")]

    [#list [false, true] as ctx]
      [#if ctx]
        [#local args_head = ["c"]]
      [#else]
        [#local args_head = []]
      [/#if]
    /**
     * @deprecated use {@code map()} instead
     */
    @Deprecated(forRemoval = true)
    public ${c.tvars(1..*np, "P#")} void apply(${c.joinf(1..*np, "Capture<? extends P#> p#", "\n        #,")}
        ${a.action_t(i, ctx, out_body, true, first_type_param)} action) {
      conclude((x, c, b) -> [#if !out_body]{[/#if]
        action.perform(${(args_head+args)?join(", ")})[#if !out_body];
        return EmptyBody.empty();
      }[/#if]);
    }

    public ${c.tvars(1..*np, "P#")} void map(${c.joinf(1..*np, "Capture<? extends P#> p#", "\n        #,")}
        ${a.action_t(i, ctx, out_body, true, first_type_param)} action) {
      conclude((x, c, b) -> [#if !out_body]{[/#if]
        action.perform(${(args_head+args)?join(", ")})[#if !out_body];
        return EmptyBody.empty();
      }[/#if]);
    }

    [/#list]

  [/#list]
[/#macro]

[#macro actions ctx_type="C" ctx_name="ctx" body_type="B" body_name="body"]
  [#list ["", "C", "B", "CB"] as suffix]
    [#list 0..p.pcount as count]
      [#compress]
        [#local predef_tvars = []]
        [#local predef_params = []]
        [#local params = []]
        [#if suffix?contains("C")]
          [#local predef_tvars += [ctx_type]]
          [#local predef_params += ["${ctx_type} ${ctx_name}"]]
          [#local params += [{"type": ctx_type, "name": ctx_name}]]
        [/#if]
        [#if suffix?contains("B")]
          [#local predef_tvars += [body_type]]
          [#local predef_params += ["${body_type} ${body_name}"]]
          [#local params += [{"type": body_type, "name": body_name}]]
        [/#if]
        [#list 1..*count as i]
          [#local params += [{"type": "P"+i?string, "name": "p"+i?string}]]
        [/#list]
        [#local params = params?map(x -> x+{"full": x.type+" "+x.name})]
      [/#compress]
      [#list [false, true] as ret]
        [#compress]
          [#local fullSuffix = suffix + count?string + ret?then("R", "")]
          [#local name = "Action"+fullSuffix]
          [#local variant = {
            "suffix": fullSuffix,
            "name": name,
            "type": name+c.tvars(params?map(x -> x.type) + ret?then(["R"], [])),
            "argType": name+c.tvars(params?map(x -> "? super "+x.type) + ret?then(["? extends R"], [])),
            "ctx": suffix?contains("C"),
            "body": suffix?contains("B"),
            "ret": ret
          }]
        [/#compress]
        [#nested variant, params]
      [/#list]
    [/#list]
  [/#list]
[/#macro]

[#macro import_actions]
  [@c.indent -2]
    import ch.raffael.meldioc.library.http.server.undertow.routing.Actions.*;
  [/@c.indent]
[/#macro]
