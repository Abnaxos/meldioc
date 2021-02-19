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

[#macro actions predef=[] void=true nonvoid=true ret_var="R"]
  [#local void_variants = []]
  [#if nonvoid]
    [#local void_variants = [false]+void_variants]
  [/#if]
  [#if void]
    [#local void_variants = void_variants+[true]]
  [/#if]
  [#local predef_params = predef?map(param -> {"type": param?word_list[0], "name": param?word_list[1]})]
  [#local predef_params = predef_params?map(x -> x+{"full": x.type+" "+x.name})]
  [#list (predef?size)..p.pcount as full_count]
    [#local vararg_count = full_count-(predef?size)]
    [#list void_variants as void_variant]
      [#local name = "Action"+full_count?string+void_variant?then("Void", "")]
      [#local vararg_params = (1..*vararg_count)?map(x -> {"type": "P"+x?string, "name": "p"+x?string})]
      [#local vararg_params = vararg_params?map(x -> x+{"full": x.type+" "+x.name})]
      [#local all_params = predef_params+vararg_params]
      [#local info = {
        "void": void_variant,
        "name": name,
        "type": name+c.tvars(all_params?map(x -> x.type) + void_variant?then([], ["R"])),
        "arg_type": name+c.tvars(all_params?map(x -> "? super "+x.type) + void_variant?then([], ["? extends ${ret_var}"] )),
        "predef_params": predef_params,
        "vararg_params": vararg_params,
        "all_params": all_params
      }]
      [#nested info]
    [/#list]
  [/#list]
[/#macro]

[#macro action_literals name indent forward=true]
  [@actions; v]
    [@c.indent -4+indent]
      [#local has_type_params = !v.void || v.all_params?has_content]
        [#if has_type_params && !forward]@SuppressWarnings({"unchecked", "overloads"})[#else]@SuppressWarnings("overloads")[/#if]
        public static ${c.tvars(v.all_params?map(x -> x.type)+v.void?then([], ["R"]))} ${v.type} ${name}(${v.arg_type} action) {
          [#if forward]
          return Actions.of(action);
          [#else]
          return [#if has_type_params](${v.type}) [/#if]action;
          [/#if]
        }

      [#if v.void]
        [@c.indent -1]
          @SuppressWarnings("overloads")
          public static ${c.tvars(v.all_params?map(x -> x.type)+["R"])} Action${v.all_params?size}${c.tvars(v.all_params?map(x -> x.type)+["R"])} ${name}(R retval, ${v.arg_type} action) {
            [#if forward]
            return Actions.of(retval, action);
            [#else]
            return (${v.all_params?map(x -> x.name)?join(", ")}) -> {
              action.perform(${v.all_params?map(x -> x.name)?join(", ")});
              return retval;
            };
            [/#if]
          }
        [/@c.indent]

      [/#if]
    [/@c.indent]

  [/@actions]
[/#macro]

[#macro import_actions include_container=false]
  [@c.indent -2]
    [#if include_container]import ch.raffael.meldioc.library.http.server.undertow.routing.Actions;[/#if]
    import ch.raffael.meldioc.library.http.server.undertow.routing.Actions.*;
  [/@c.indent]
[/#macro]
