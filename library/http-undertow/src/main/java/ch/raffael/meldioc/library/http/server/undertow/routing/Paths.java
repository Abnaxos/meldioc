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

package ch.raffael.meldioc.library.http.server.undertow.routing;

import io.vavr.collection.Array;

import java.util.regex.Pattern;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
final class Paths {

  private static final Pattern PATH_SPLIT = Pattern.compile("/");

  private Paths() {
  }

  static String normalize(String path) {
    if (empty(path)) {
      return path;
    }
    if (path.contains("//")) {
      throw new RoutingDefinitionException("Illegal path: " + path);
    }
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }

  static String normalizeLeadingSlash(String path) {
    path = normalize(path);
    return !path.startsWith("/") ? "/" + path : path;
  }

  static String normalizeStripLeadingSlash(String path) {
    path = normalize(path);
    return path.startsWith("/") ? path.substring(1) : path;
  }

  static Array<String> segments(String path) {
    return empty(path) ? Array() : Array(PATH_SPLIT.split(normalizeStripLeadingSlash(path)));
  }

  static boolean empty(String path) {
    return path.isEmpty() || path.equals("/");
  }

}
