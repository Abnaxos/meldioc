/*
 *  Copyright (c) 2022 Raffael Herzog
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

// Freemarker: 29 Oct 2021, 17:45:58 ch/raffael/meldioc/library/http/server/undertow/routing/RoutingBuilder.java.ftl
package ch.raffael.meldioc.library.http.server.undertow.routing;

import io.vavr.control.Either;

///> "// vagen ${new Date()}"
/**
 * Builder for child DSL frames (path segments and captures).
 */
/// filename RoutingBuilder.java
/// = `$RoutingBuilder
///   --> `RoutingBuilder
/// = `$Capture
///   --> `Capture
/// = `@$.Public
///   --> `public
// TODO FIXME (2021-10-31) find a better name, e.g. PathCaptureBuilder, PathFragmentBuilder
class $RoutingBuilder<C> {

  private static final String STRING_NAME = "string";
  private static final String INT_NAME = "int";

  private final Frame<C> initiatingFrame;

  private $RoutingBuilder(Frame<C> initiatingFrame) {
    this.initiatingFrame = initiatingFrame;
  }

  static <C> $RoutingBuilder<C>.InitialFragment begin(Frame<C> frame) {
    return new $RoutingBuilder<>(frame).new InitialFragment();
  }

  private static String captureName(Converter<?> converter) {
    return "capture";
  }

  abstract class AbstractFragment {

    abstract Frame<C> resolve();

    String name() {
      StringBuilder buf = new StringBuilder();
      name(buf);
      return buf.toString();
    }

    abstract void name(StringBuilder buf);

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder("RoutingBuilder[");
      name(buf);
      return buf.append("]").toString();
    }
  }

  ///<<<
  public final class InitialFragment extends AbstractFragment {
    private InitialFragment() {
    }
    ///<<< false
    private class Fragment0 {Fragment0(Object a1, Object a2){}}
    private class Fragment1<T> {Fragment1(Object a1, Object a2){}}
    ///>>>

    public Fragment0 path(String path) {
      return new Fragment0(this, path);
    }

    public Fragment1<String> captureString() {
      return capture(STRING_NAME, Converter.asString());
    }

    public Fragment1<String> captureString(String name) {
      return capture(name, Converter.asString());
    }

    public Fragment1<Integer> captureInt() {
      return capture(INT_NAME, Converter.asInt());
    }

    public Fragment1<Integer> captureInt(String name) {
      return capture(name, Converter.asInt());
    }

    public <T> Fragment1<T> capture(Converter<? extends T> converter) {
      return capture(captureName(converter), converter);
    }

    public <T> Fragment1<T> capture(String name, Converter<? extends T> converter) {
      return new Fragment1<>(this, new $Capture.Attachment<>(name, converter));
    }

    @Override
    Frame<C> resolve() {
      return initiatingFrame;
    }

    @Override
    void name(StringBuilder buf) {
      buf.append("/");
    }
  }
  ///>>>

  abstract class FollowupFragment extends AbstractFragment {
    private final AbstractFragment parent;
    private final Either<String, $Capture.Attachment<?>> segment;

    private FollowupFragment(AbstractFragment parent, String path) {
      this.parent = parent;
      this.segment = Either.left(path);
    }

    private FollowupFragment(AbstractFragment parent, $Capture.Attachment<?> capture) {
      this.parent = parent;
      this.segment = Either.right(capture);
    }

    @Override
    Frame<C> resolve() {
      var frame = parent.resolve();
      ///<<<
      /// = `$.x()
      ///   --> `frame::captureChild
      return segment.fold(frame::pathChild, $.x());
      ///>>>
    }

    @Override
    void name(StringBuilder buf) {
      buf.append("/");
      segment.fold(
          buf::append,
          c -> buf.append("{").append(c.name()).append("}"));
    }
  }

  ///<<</ n: 0..count
  ///
  /// = `FragmentN
  ///   --> "Fragment$n"
  /// = `$Blocks.CurryN
  ///   --> "Blocks.Curry$n"
  /// ~ `Blocks.Curry(0|-1).*curry,\s
  ///   -->
  ///
  /// ~ `<(\? extends )?Tall>
  ///   --> fwd 1..n collect {"${_1}T$it"} join ', ' emptyOr {"<$it>"}
  public final class FragmentN<Tall> extends FollowupFragment {
    ///<<< n > 0
    private final $Blocks.CurryN<? extends Tall> curry;
    ///>>>

    private FragmentN(AbstractFragment parent, $Blocks.CurryN<? extends Tall> curry, String path) {
      super(parent, path);
      ///<<< n > 0
      this.curry = curry;
      ///>>>
    }

    ///<<<
    /// ! n=n-1
    /// = `$Blocks.CurryN
    /// --> "Blocks.Curry$n"
    ///
    /// ~ `<(\? extends )?Tall>
    ///   --> fwd 1..n collect {"${_1}T$it"} join ', ' emptyOr {"<$it>"}
    /// = `Attachment<?>
    ///   --> if (n>=0) "Attachment<? extends T${n+1}>" else _0
    private FragmentN(AbstractFragment parent, $Blocks.CurryN<? extends Tall> curry, $Capture.Attachment<?> capture) {
      super(parent, capture);
      ///<<< n>=0
      /// = `$.x()
      ///   --> n == 0 ? 'Blocks.curry(capture)' : 'curry.append(capture)'
      this.curry = $.x();
      ///>>>
    }
    ///>>>

    ///<<<
    /// = `curry, `
    ///   --> if (n==0) '' else _0
    /// = `new FragmentN<>
    ///   --> if(n==0) _0.rm '<>' else _0
    public FragmentN<Tall> path(String path) {
      return Paths.empty(path) ? this : new FragmentN<>(this, curry, path);
    }
    ///>>>

    ///<<< n < count
    ///<<< false
    class $FragNext<T> {$FragNext(Object arg1, Object arg2){}}
    ///>>>
    /// ! t = 'next type'
    /// = `FragmentN
    ///   --> "Fragment${n+1}"
    /// = `<Tall>
    ///   --> "<${fwd 1..n collect {"T$it"} join ', '}, $t>"
    /// ~ `<,\s+
    ///   --> `<
    /// ~ `curry,\s+
    ///    --> if(n==0) '' else _0
    /// ! t = 'String'
    public FragmentN<Tall> captureString() {
      return capture(STRING_NAME, Converter.asString());
    }

    public FragmentN<Tall> captureString(String name) {
      return capture(name, Converter.asString());
    }

    /// ! t = 'Integer'
    public FragmentN<Tall> captureInt() {
      return capture(STRING_NAME, Converter.asInt());
    }

    public FragmentN<Tall> captureInt(String name) {
      return capture(name, Converter.asInt());
    }

    /// ! t = 'TC'
    public <TC> FragmentN<Tall> capture(Converter<? extends TC> converter) {
      return capture(captureName(converter), converter);
    }

    /// = `$.x()
    ///   --> `new Capture.Attachment<>(name, converter)
    public <TC> FragmentN<Tall> capture(String name, Converter<? extends TC> converter) {
      return new FragmentN<>(this, curry, new $Capture.Attachment<>(name, converter));
    }
    ///>>>

    ///<<<
    /// = `$.X
    ///   --> "Blocks.Block$n<${fwd 1..n collect {"? super T$it"} join ', '}>".rm '<>'
    /// = `$.x(block)
    ///   --> if (n==0) 'block' else 'curry.runnable(block)'
    public void route($.X block) {
      resolve().run($.x(block));
    }
    ///>>>

    public void merge(RoutingDefinition<? super C> that) {
      resolve().merge(that.rootFrame);
    }
  }
  ///>>>
}
