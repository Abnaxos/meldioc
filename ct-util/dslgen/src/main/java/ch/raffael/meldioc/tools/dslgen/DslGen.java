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

import groovy.util.Eval;
import io.vavr.collection.Seq;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class DslGen {

  private final Args args;

  private Listener listener = new Listener() {
    @Override
    public void beginUnit(Object input) {
    }
    @Override
    public void endUnit(Object input, Object output, Seq<String> errors) {
    }
  };

  public DslGen(List<String> args) throws CmdLineException {
    this(parseArgs(args));
  }

  public DslGen(String[] args) throws CmdLineException {
    this(parseArgs(Arrays.asList(args)));
  }

  public DslGen(Args args) {
    this.args = args;
  }

  private static Args parseArgs(List<String> args) throws CmdLineException {
    var target = new Args();
    var cmdLine = new CmdLineParser(target);
    cmdLine.parseArgument(args);
    return target;
  }

  @SuppressWarnings({"CallToSystemExit", "UseOfSystemOutOrSystemErr", "ThrowablePrintedToSystemOut"})
  public static void main(String[] args) {
    try {
      DslGen gen = new DslGen(List.of(args));
      boolean[] success = new boolean[]{true};
      gen.addListener(new Listener() {
        @Override
        public void beginUnit(Object input) {
          System.out.println("DlsGen: " + input);
        }
        @Override
        public void endUnit(Object input, Object output, Seq<String> errors) {
          if (!errors.isEmpty()) {
            System.err.println("ERROR: DslGen: " + errors.length() + " errors (" + input + ")");
            success[0] = false;
          }
        }
      });
      gen.generate();
      System.exit(success[0] ? 0 : 1);
    } catch (CmdLineException e) {
      e.getParser().printUsage(System.err);
      System.exit(1);
    } catch (IOException e) {
      System.err.println(e);
      System.exit(1);
    }
  }

  public void addListener(Listener listener) {
    var prev = this.listener;
    this.listener = new Listener() {
      @Override
      public void beginUnit(Object input) {
        prev.beginUnit(input);
        listener.beginUnit(input);
      }
      @Override
      public void endUnit(Object input, Object output, Seq<String> errors) {
        prev.endUnit(input, output, errors);
        listener.endUnit(input, output, errors);
      }
    };
  }

  public void generate() throws CmdLineException, IOException {
    for (var file : args.files()) {
      file = args.base().resolve(file).normalize();
      listener.beginUnit(file);
      var outFile = args.out().resolve(args.base().relativize(file)).normalize();
      Template template = new Template();
      var node = template.parse(file, args.charset);
      outFile = template.outputFile(outFile);
      Files.createDirectories(outFile.getParent());
      try (var out = Files.newBufferedWriter(outFile, args.charset())) {
        var scope = new Scope();
        args.parameters.forEach(scope.binding()::setVariable);
        var error = node.lines(scope)
            .map(l -> {
              try {
                out.write(l);
                out.write("\n");
                return Optional.<IOException>empty();
              } catch (IOException e) {
                return Optional.of(e);
              }
            })
            .filter(Optional::isPresent).map(Optional::get)
            .headOption().getOrNull();
        if (error != null) {
          throw error;
        }
        listener.endUnit(file, outFile, scope.errors());
      }
    }
  }

  public static class Args {

    private static final Pattern PARAMETER_RE = Pattern.compile("(?>([^=:]+)[=:])?(.*)");

    private Path base = Path.of(System.getProperty("user.dir"));
    private Path out = Path.of(System.getProperty("user.dir"));
    private Charset charset = Charset.defaultCharset();
    private final List<Path> files = new ArrayList<>();
    private final Map<String, Object> parameters = new LinkedHashMap<>();

    public Path base() {
      return base;
    }

    public Path out() {
      return out;
    }

    public List<Path> files() {
      return files;
    }

    public Charset charset() {
      return charset;
    }

    public Map<String, Object> parameters() {
      return parameters;
    }

    @Option(name = "-d")
    public void base(Path path) {
      base = path;
    }

    @Option(name = "-o")
    public void out(Path path) {
      out = path;
    }

    @Option(name = "-e")
    public void charset(String charset) {
      this.charset = Charset.forName(charset);
    }

    @Option(name = "-p")
    public void parameter(String parameter) {
      var m = PARAMETER_RE.matcher(parameter);
      if (!m.matches()) {
        // always matches
        throw new IllegalStateException("'" + parameter + "' does not match" + PARAMETER_RE);
      }
      var name = m.group(1);
      var expression = m.group(2);
      if (name == null) {
        name = expression;
        expression = "true";
      }
      parameter(name, Eval.me(expression));
    }

    @Argument(multiValued = true, required = true)
    public void file(Path path) {
      files.add(path);
    }

    public void parameter(String name, String expression) {
      parameter(name, Eval.me(expression));
    }

    public void parameter(String name, Object value) {
      parameters.put(name, value);
    }

    public void charset(Charset charset) {
      this.charset = charset;
    }
  }

  public interface Listener {
    void beginUnit(Object input);
    void endUnit(Object input, Object output, Seq<String> errors);
  }
}
