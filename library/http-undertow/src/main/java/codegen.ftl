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

[#function joinf elements content around="#" separator=", " insert="#"]
  [#if elements?size == 0]
    [#return ""]
  [#else]
    [#return around?replace(insert, elements?map(e -> content?replace(insert, e))?join(separator))]
  [/#if]
[/#function]

[#function expand_tvar tvar]
  [#if tvar?starts_with(">:")]
    [#return "? extends "+tvar?substring(2)]
  [#elseif tvar?starts_with("<:")]
    [#return "? super "+tvar?substring(2)]
  [#else]
    [#return tvar]
  [/#if]
[/#function]

[#function tvars vars format="T#" around="<#>"]
  [#local result = ""]
  [#list vars as v]
    [#if !v?is_first]
      [#local result = result+", "]
    [/#if]
    [#if v?is_number]
      [#local result = result+expand_tvar(format?replace("#", v))]
    [#else]
      [#local result = result+expand_tvar(v?string)]
    [/#if]
  [/#list]
  [#if result?length > 0]
    [#return around?replace("#", result)]
  [#else]
    [#return result]
  [/#if]
[/#function]

[#function ntype vars format="T#"]
  [#return vars?size?string+tvars(vars format)]
[/#function]

[#macro indent offset]
  [#if offset == 0]
    [#nested]
  [#else]
    [#local lines][#nested][/#local]
    [#local lines = lines?split("\r?\n|\r", "r")]
    [#if offset > 0]
      [#local indent = ""]
      [#list 0..*offset as _][#local indent += "  "][/#list]
      [#list lines as l]
${indent}${l}
      [/#list]
    [#elseif offset < 0]
      [#list lines as l]
${l?replace("^(  ){0,"+(-offset)?string+"}", "", "r")}
      [/#list]
    [/#if]
  [/#if]
[/#macro]

[#macro verbose indent_offset=-1]
[#if p.verbose][@indent indent_offset]
[#nested]
[/@indent][/#if]
[/#macro]

[#macro squash][#local content][#nested][/#local]${content?string?trim?replace("\\s+", " ", "rm")}[/#macro]
