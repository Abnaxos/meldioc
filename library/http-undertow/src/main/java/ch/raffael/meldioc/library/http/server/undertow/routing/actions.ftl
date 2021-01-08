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

[#function action_t n ctx ret wildcards=false first_type_param=""]
  [#if n==0]
    [#local first_type_param = ""]
  [/#if]
  [#if first_type_param?length > 0]
    [#local np = n-1]
  [#else]
    [#local np = n]
  [/#if]
  [#local name = "Action"]
  [#local typevars = []]

  [#if ctx]
    [#local name = name+"C"]
    [#if wildcards]
      [#local typevars = typevars+["? super C"]]
    [#else]
      [#local typevars = typevars+["C"]]
    [/#if]
  [/#if]

  [#local name = name+n?string]
  [#if first_type_param?length > 0]
    [#if wildcards]
      [#local typevars = typevars+["? super "+first_type_param]]
    [#else]
      [#local typevars = typevars+[first_type_param]]
    [/#if]
  [/#if]
  [#if np>0]
    [#if wildcards]
      [#local typevars = typevars+[c.joinf(1..np, "? super P#")]]
    [#else]
      [#local typevars = typevars+[c.joinf(1..np, "P#")]]
    [/#if]
  [/#if]

  [#if ret]
    [#local name = name+"R"]
    [#if wildcards]
      [#local typevars = typevars+["? extends R"]]
    [#else]
      [#local typevars = typevars+["R"]]
    [/#if]
  [/#if]

  [#if typevars?size > 0]
    [#local name = name+"<"+(typevars?join(", "))+">"]
  [/#if]

  [#return name]
[/#function]
