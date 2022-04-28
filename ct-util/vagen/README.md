VaGen – VarArgs Generator
=========================

VaGen is a very simple templating system that was developed for vararg-like
code. It follows the idea of generating code by example: you give an example
of the code and a set of transformations using regular expressions and
Groovy to be applied to the code, then template engine loops over something
(usually a sequence like `0..9`) and applies the transformations for each
value.

A simple example:

```java
///<<< n: 0..9
/// = `<Pn>
///   --> fwd 1..n collect {"P$it"} join ', ' wrap '<{}>'
/// = `Pn pn
///   --> fwd 1..n collect {"P$it p$it"} join ', '
/// = `pn
///   --> fwd 1..n collect {"p$it"} join ', '
public <Pn> List<Object> doSomething(Pn pn) {
  return List.of(pn);
}
///>>>
```

This will generate methods like this:

```java
public List<Object> doSomething() {
    return List.of();
}
public <P1> List<Object> doSomething(P1 p1) {
    return List.of(p1);
}
// ...
public <P1, P2, P3, P4, P5, P6, P7, P8, P9> List<Object> doSomething(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9) {
    return List.of(p1, p2, p3, p4, p5, p6, p7, p8, p9);
}
```

The idea is that the example itself is readable, self-descriptive and has no
errors, so the IDE can provide full support and the templated code is
maintainable. Upon running VaGen on the template, the code "explodes" to
the mess such code tends to be.

VaGen was written with exactly this use case in mind, hence the name
("varargs generator"). It may be suitable for other scenarios as well. It's
used for the routing DSL in *meld-http*, where the actual code is more
complex. This results in less straight-forward transformation rules, but the
templates themselves are pretty self-explanatory, which was the goal.


The Template Language
---------------------

VaGen is line oriented, i.e. templates are processed line by line. Lines
starting with 3 slashes (`///`, indentation is ignored) are directives and
will be left out in the result. Expressions are Groovy expressions, there
are a few categories applied, see below. Template comments start with '--'.

### Expressions

Most expressions will be converted to a string as in `(expression) as
String`. These expressions have a special syntax non-groovy string values:
If they start with a '\`', they'll be interpreted as a literal string, no
modifications whatsoever will be made. Such string expressions may
optionally end with a '\`'. If they don't, trailing spaces will be trimmed. All occurrences of '\`' will be left as-is except the last one, if present.

### Blocks, Loops and Scopes

Blocks are introduced opened '<<<' and closed with '>>>'. Each block has its
own defined variables. Variables from parent blocks can be referred to. If a
variable defined in a parent block is overridden, it's value will be set in
the current block's scope, i.e. outside the block, the variable's value will
remain unchanged. **Replacements defined in a block also only applied within that block**.

The general syntax is `///<<< [[<ident>:] <expression>]`. If no expression
is given, the block will be inserted unconditionally exactly once. This may
be useful to confine certain replacements to a small portion of the
template. Otherwise, the behaviour depends on the result of the expression:

- If it's `null`, the block won't be inserted
- If it's a boolean (as of `expr as Boolean`), the block will be inserted
  once if `true`, not at all if `false`
- If it's an iterator or iterable, the block will be inserted once for each element.

If an identifier is specified, the current value is available in a variable
with that name within the block.

The block may also be introduced with `///<<</` (added slash at the end), in
which case, an empty line will be inserted between the iterations.

### Replacements

Replacements consist of 2 directives, the match and the replacement
directive. '=' a literal string match, '~' a regular expression match. The
following directive must be a replacement directive starting with '-->' and
containing an expression. In this expression, the groovy variables `_0`,
`_1` etc. are available to refer to capturing groups in the match.

Some examples:

```
/// -- a simple string replacement: replaces 'foo' with 'bar'
/// = `foo
///   --> `bar
///
/// -- replaces 'foo' with at least 2 'o' with 'bar' with as many 'a's 
/// ~ `f(o{2,})
///   --> "b${'a' * _1.length()}r"
```

Replacements will be applied in the order as specified.
