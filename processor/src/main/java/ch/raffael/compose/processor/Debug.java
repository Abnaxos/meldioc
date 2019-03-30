/*
 *  Copyright (c) 2019 Raffael Herzog
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

package ch.raffael.compose.processor;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * TODO javadoc
 */
public class Debug {

  public static final boolean DEVEL_MODE = false;
  public static final Consumer<Runnable> ON_DEVEL_MODE = r -> {
    if (DEVEL_MODE) {
      r.run();
    }
  };
  public static final boolean FAILSAFE_GEN = false;

  private static final Path WORK = Paths.get("target", "debug");

  public static void main(String[] args) throws Exception {
    String sourceSet = args[0];
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

    String classOut = prepareOutput("classes");
    String srcOut = prepareOutput("gensrc");
    List<String> options = Arrays.asList(
        "-d", classOut,
        "-s", srcOut,
        "-cp", Files.lines(WORK.resolve(sourceSet + "-classpath"))
            .map(String::trim)
            .filter(l -> !l.isEmpty())
            .collect(Collectors.joining(System.getProperty("path.separator")))
    );
    Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(
        Files.lines(WORK.resolve(sourceSet + "-sources"))
            .map(String::trim)
            .filter(l -> !l.isEmpty())
            .map(File::new)
            .collect(Collectors.toList()));
    compiler.getTask(null, fileManager, null, options, null, compilationUnits).call();

    fileManager.close();
  }

  private static String prepareOutput(String n) throws IOException {
    Path p = WORK.resolve(n);
    if (Files.exists(p)) {
      Files.walkFileTree(p, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return super.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return super.postVisitDirectory(dir, exc);
        }
      });
    }
    Files.createDirectories(p);
    return p.toString();
  }


}
