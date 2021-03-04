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

package ch.raffael.meldioc.library.http.server.undertow.routing;

import io.vavr.control.Option;

import javax.annotation.Nullable;
import java.lang.StackWalker.StackFrame;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.vavr.control.Option.some;

final class DslTrace {

  private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
  private static final String INTERNAL_PREFIX = DslTrace.class.getPackageName() + ".";
  private static final Predicate<StackFrame> IS_INTERNAL =
      f -> f.getDeclaringClass().getName().startsWith(INTERNAL_PREFIX);
  private static final Predicate<StackFrame> IS_NOT_INTERNAL = IS_INTERNAL.negate();

  private final Option<DslTrace> parent;
  private final Kind kind;
  private final String path;
  private final Option<StackFrame> location;

  DslTrace(Option<DslTrace> parent, Kind kind) {
    this(parent, kind, "");
  }

  DslTrace(DslTrace parent, Kind kind) {
    this(some(parent), kind, "");
  }

  DslTrace(DslTrace parent, Kind kind, String path) {
    this(some(parent), kind, path);
  }

  DslTrace(Option<DslTrace> parent, Kind kind, String path) {
    this(parent, kind, path, locationFromCurrentStack());
  }

  DslTrace(DslTrace parent, Kind kind, String path, Option<StackFrame> location) {
    this(some(parent), kind, path, location);
  }

  DslTrace(Option<DslTrace> parent, Kind kind, String path, Option<StackFrame> location) {
    this.parent = parent;
    this.kind = kind;
    this.path = path;
    this.location = location;
  }

  private static Option<StackFrame> locationFromCurrentStack() {
    return Option.ofOptional(STACK_WALKER.walk(s -> s
        .dropWhile(IS_NOT_INTERNAL)
        .dropWhile(IS_INTERNAL)
        .filter(f -> RoutingDefinition.class.isAssignableFrom(f.getDeclaringClass()))
        .findFirst()));
  }

  Option<DslTrace> parent() {
    return parent;
  }

  DslTrace root() {
    return parent.map(DslTrace::root).getOrElse(this);
  }

  DslTrace reroot(DslTrace newRoot) {
    if (parent.isEmpty()) {
      return new DslTrace(newRoot, kind, path, location);
    } else {
      return new DslTrace(parent.get().reroot(newRoot), kind, path, location);
    }
  }

  Kind kind() {
    return kind;
  }

  String path() {
    return path;
  }

  Option<StackFrame> location() {
    return location;
  }

  String description() {
    StringBuilder buf = new StringBuilder();
    description(true, buf);
    return buf.toString();
  }

  private StringBuilder description(boolean first, StringBuilder buf) {
    if (first) {
      buf.append(kind.prefix());
    }
    if (parent.isDefined()) {
      buf = parent.get().description(false, buf);
    }
    if (!path.isEmpty()) {
      buf.append('/').append(path);
    }
    return buf;
  }

  String stackTrace() {
    StringBuilder buf = new StringBuilder();
    buf = description(true, buf);
    var current = this;
    var previous = (DslTrace) null;
    while(current != null) {
      if (includeInStackTrace(current, previous)) {
        buf.append(System.lineSeparator()).append("  - ");
        var tag = current.kind.tag();
        if (!tag.isEmpty()) {
          buf.append('<').append(tag).append("> ");
        }
        buf.append(current.location().get());
        previous = current;
      }
      current = current.parent().getOrNull();
    }
    return buf.toString();
  }

  private boolean includeInStackTrace(DslTrace current, @Nullable DslTrace previous) {
    if (previous == null) {
      return true;
    } else if (!current.location().isDefined()) {
      return false;
    } else if (current.kind() == Kind.MERGE) {
      return true;
    } else if (previous.location().isEmpty()) {
      return true;
    } else {
      return !(locationEquals(current, previous, StackFrame::getDeclaringClass)
          && locationEquals(current, previous, StackFrame::getMethodName)
          && locationEquals(current, previous, StackFrame::getDescriptor));
    }
  }

  private boolean locationEquals(@Nullable DslTrace a, @Nullable DslTrace b,
                                 Function<? super StackFrame, ?> value) {
    if (a == null || b == null || a.location.isEmpty() || b.location.isEmpty()) {
      return false;
    } else {
      return Objects.equals(value.apply(a.location.get()), value.apply(b.location.get()));
    }
  }

  @Override
  public String toString() {
    return "DslTrace[" + kind + "," + path + "," + location + "]";
  }

  @SuppressWarnings("SwitchStatementWithTooFewBranches")
  enum Kind {
    FRAME, MERGE, ENDPOINT;

    String prefix() {
      switch (this) {
        case ENDPOINT:
          return "";
        default:
          return name() + ":";
      }
    }

    String tag() {
      switch (this) {
        case FRAME:
          return "";
        default:
          return name().toLowerCase(Locale.ROOT);
      }
    }
  }
}
