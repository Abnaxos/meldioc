/*
 *  Copyright (c) 2021 Raffael Herzog
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to
 *  deal in the Software without restriction, including without limitation the
 *  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 *  sell copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 *  IN THE SOFTWARE.
 */

package ch.raffael.meldioc.tools.dslgen;

import ch.raffael.meldioc.tools.dslgen.tree.AppendableCompositeNode;
import ch.raffael.meldioc.tools.dslgen.tree.BlockNode;
import ch.raffael.meldioc.tools.dslgen.tree.ErrorNode;
import ch.raffael.meldioc.tools.dslgen.tree.EvalNode;
import ch.raffael.meldioc.tools.dslgen.tree.InsertNode;
import ch.raffael.meldioc.tools.dslgen.tree.LineNode;
import ch.raffael.meldioc.tools.dslgen.tree.ListNode;
import ch.raffael.meldioc.tools.dslgen.tree.Node;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;
import static io.vavr.control.Option.when;

public final class Template {

  private Option<Path> filename = none();

  private List<AppendableCompositeNode> stack = List.empty();

  private Option<Match> match = none();

  public Template() {
  }

  public Path outputFile(Path defaultOut) {
    return filename.map(f -> defaultOut.getParent().resolve(f).normalize()).getOrElse(defaultOut);
  }

  public Node parse(Path file, Charset charset) throws IOException {
    return parse(Files.newBufferedReader(file, charset));
  }

  public Node parse(Reader reader) {
    var root = new ListNode(none(), "<root>");
    stack = List.of(root);
    match = none();
    (reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader))
        .lines()
        .map(this::toNode)
        .forEach(n -> stack.head().append(n));
    //if (stack.length() != 1) {
    //  throw new IllegalStateException("Stack inconsistency: " + stack);
    //}
    //root.append(new LineNode(some(root), "// errors: " + errors));
    return root;
  }

  private Node push(AppendableCompositeNode node) {
    var n = Node.nop(some(stack.head()), "PUSH: " + node);
    stack.head().append(node);
    stack = stack.push(node);
    return n;
  }

  private Node pop() {
    var n = Node.nop(some(stack.head()), "POP: " + stack.head());
    stack = stack.pop();
    return n;
  }

  private Option<AppendableCompositeNode> head() {
    return some(stack.head());
  }

  private Node toNode(String line) {
    return Cmd.command(line).map(t -> commandNode(t._1(), t._2())).getOrElse(() -> new LineNode(head(), line));
  }

  private Node commandNode(String command, String indent) {
    var cmd = command.trim();
    if (cmd.isBlank()) {
      return Node.operation(head(), "<blank>", __ -> {});
    }
    if (Cmd.comment(cmd)) {
      return Node.nop(head(), "comment");
    }
    // matching
    Option<? extends Node> node = parseReplacement(cmd);
    if (node.isDefined()) {
      return node.get();
    }
    node = Cmd.matchPlain(cmd)
        .map(p -> {
          match = some(new Match(Substitution.MatchMode.PLAIN, p));
          return Node.nop(head(), "=" + p);
        })
        .orElse(Cmd.matchRegex(cmd)
            .map(p -> {
              match = some(new Match(Substitution.MatchMode.REGEX, p));
              return Node.nop(head(), "~" + p);
            }));
    if (node.isDefined()) {
      return node.get();
    }
    // blocks
    node = Cmd.blockOpen(cmd).map(l -> push(new BlockNode(head(), l)));
    if (node.isDefined()) {
      return node.get();
    }
    if (Cmd.blockClose(cmd)) {
      if (stack.size() <= 1) {
        return error("Unbalanced block close").get();
      }
      pop();
      return Node.nop(head(), ">>>");
    }
    // eval and insert
    node = Cmd.eval(head(), cmd)
        .orElse(Cmd.insert(head(), cmd, indent));
    if (node.isDefined()) {
      return node.get();
    }
    // normalize
    node = Cmd.normalize(head(), cmd);
    if (node.isDefined()) {
      return node.get();
    }
    // filename
    var filename = Cmd.filename(cmd);
    if (filename.isDefined()) {
      this.filename = filename;
      return Node.nop(head(), cmd);
    }
    return error("Unknown command: " + cmd).get();
  }

  private Option<Node> parseReplacement(String cmd) {
    var replacement = Cmd.replacement(cmd);
    if (replacement.isDefined()) {
      if (!match.isDefined()) {
        return error("Replacement without match: " + replacement.get());
      }
      else {
        var m = match.get();
        var e = replacement.get();
        match = none();
        return some(Node.operation(head(), m + "->" + e, s -> {
          s.newSubstitutionGroup();
          s.addSubstitution(m.mode, m.pattern, e);
        }));
      }
    }
    if (match.isDefined()) {
      var m = match.get();
      match = none();
      return error("Match without replacement: " + m);
    } else {
      return none();
    }
  }

  private Option<Node> error(String message) {
    return some(new ErrorNode(head(), message));
  }

  static final class Match {
    final Substitution.MatchMode mode;
    final String pattern;
    Match(Substitution.MatchMode mode, String pattern) {
      this.mode = mode;
      this.pattern = pattern;
    }
    @Override
    public String toString() {
      return "Match[" + mode + ":" + pattern + "]";
    }
  }

  static final class Cmd {

    static final String IDENT = "[a-zA-Z_][a-zA-Z0-9_]*";

    static final Pattern COMMAND = Pattern.compile("(?<indent>\\s*)///(?<cmd>.*)");
    static final Pattern COMMENT = Pattern.compile("--(?:$|[^>].*)");

    static final Pattern MATCH_PLAIN = Pattern.compile("=(?<p>.*)");
    static final Pattern MATCH_REGEX = Pattern.compile("~(?<p>.*)");

    static final Pattern REPLACEMENT = Pattern.compile("--?>(?<expr>.*)");

    static final Pattern EVAL = Pattern.compile("!(?<expr>.*[^\\s].*)");
    static final Pattern INSERT = Pattern.compile(">(?<expr>.*[^\\s].*)");

    static final Pattern BLOCK_OPEN = Pattern.compile("<<<(?<opt>[/]*)\\s*(((?<ident>" + IDENT + ")\\s*:)?(?<expr>.*[^\\s].*))?");
    static final Pattern BLOCK_CLOSE = Pattern.compile(">>>");

    static final Pattern FILENAME = Pattern.compile("filename\\s+(?<f>[^\\s]+)");
    static final Pattern NORMALIZE = Pattern.compile("normali[sz]e(\\s+spaces)+");

    static Option<Tuple2<String, String>> command(String line) {
      var m = COMMAND.matcher(line);
      return when(m.matches(), () -> Tuple.of(m.group("cmd").trim(), m.group("indent")));
    }

    static boolean comment(String cmd) {
      return COMMENT.matcher(cmd).matches();
    }

    static Option<String> matchPlain(String cmd) {
      var m = MATCH_PLAIN.matcher(cmd.trim());
      return when(m.matches(), () -> m.group("p").trim());
    }

    static Option<String> matchRegex(String cmd) {
      var m = MATCH_REGEX.matcher(cmd.trim());
      return when(m.matches(), () -> m.group("p").trim());
    }

    static Option<String> replacement(String cmd) {
      var m = REPLACEMENT.matcher(cmd.trim());
      return when(m.matches(), () -> m.group("expr").trim());
    }

    static Option<Node> eval(Option<? extends Node> parent, String cmd) {
      var m = EVAL.matcher(cmd.trim());
      return when(m.matches(), () -> new EvalNode(parent, m.group("expr").trim()));
    }

    static Option<Node> insert(Option<? extends Node> parent, String cmd, String indent) {
      var m = INSERT.matcher(cmd.trim());
      return when(m.matches(), () -> new InsertNode(parent, m.group("expr").trim(), indent));
    }

    static Option<BlockNode.Loop> blockOpen(String cmd) {
      var m = BLOCK_OPEN.matcher(cmd.trim());
      if (!m.matches()) {
        return none();
      }
      return some(new BlockNode.Loop(
          Option.of(m.group("ident")),
          Option.of(m.group("expr")).map(String::trim).filter(e -> !e.isEmpty()),
          m.group("opt").contains("/")));
    }

    static Option<Path> filename(String cmd) {
      var m = FILENAME.matcher(cmd);
      return m.matches() ? some(Path.of(m.group("f").replace('/', File.separatorChar))) : none();
    }

    static boolean blockClose(String cmd) {
      return BLOCK_CLOSE.matcher(cmd).matches();
    }

    static Option<Node> normalize(Option<? extends Node> parent, String cmd) {
      var m = NORMALIZE.matcher(cmd.trim());
      if (m.matches()) {
        Consumer<Scope> ops = s -> {};
        for (var i = 1; i <= m.groupCount(); i++) {
          //noinspection SwitchStatementWithTooFewBranches
          switch (m.group(i).trim()) {
            case "spaces":
              ops = ops.andThen(s -> {
                s.newSubstitutionGroup();
                s.addSubstitution(Substitution.MatchMode.REGEX, "(?<=[^\\s])\\s{2,}", " ");
                s.newSubstitutionGroup();
                s.addSubstitution(Substitution.MatchMode.REGEX, "\\s+$", "");
                s.newSubstitutionGroup();
                s.addSubstitution(Substitution.MatchMode.REGEX, "(?<=[^\\s])\\s+([,;)])", "! _1");
                s.newSubstitutionGroup();
                s.addSubstitution(Substitution.MatchMode.REGEX, "([(])\\s+", "! _1");
                s.newSubstitutionGroup();
              });
              break;
            default:
              throw new IllegalStateException("Unexpected: " + m.group(i));
          }
        }
        return some(Node.operation(parent, cmd, ops));
      } else {
        return none();
      }
    }
  }
}
