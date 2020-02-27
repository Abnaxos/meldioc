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

import ch.raffael.meldioc.library.http.server.undertow.routing.Blocks.Block0;
import ch.raffael.meldioc.library.http.server.undertow.routing.Blocks.Block1;
import ch.raffael.meldioc.library.http.server.undertow.routing.Blocks.Block2;
import ch.raffael.meldioc.library.http.server.undertow.routing.Blocks.Block3;
import ch.raffael.meldioc.library.http.server.undertow.routing.Blocks.Curry1;
import ch.raffael.meldioc.library.http.server.undertow.routing.Blocks.Curry2;
import ch.raffael.meldioc.library.http.server.undertow.routing.Blocks.Curry3;
import io.vavr.control.Either;

/**
 * Builder for child DSL frames (path segments and captures).
 */
public class RoutingBuilder<C> {

  private static final String STRING_NAME = "string";
  private static final String INT_NAME = "int";

  private final Frame<C> initiatingFrame;

  RoutingBuilder(Frame<C> initiatingFrame) {
    this.initiatingFrame = initiatingFrame;
  }

  static <C> RoutingBuilder<C>.InitialFragment begin(Frame<C> frame) {
    return new RoutingBuilder<>(frame).new InitialFragment();
  }

  private static String captureName(Converter<?> converter) {
    return "capture";
  }

  public abstract class AbstractFragment {

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

  public final class InitialFragment extends AbstractFragment {
    private InitialFragment() {
    }

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
      return new Fragment1<>(this, new Capture.Attachment<>(name, converter));
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

  public abstract class FollowupFragment extends AbstractFragment {
    private final AbstractFragment parent;
    private final Either<String, Capture.Attachment<?>> segment;

    private FollowupFragment(AbstractFragment parent, String path) {
      this.parent = parent;
      this.segment = Either.left(path);
    }

    private FollowupFragment(AbstractFragment parent, Capture.Attachment<?> capture) {
      this.parent = parent;
      this.segment = Either.right(capture);
    }

    @Override
    Frame<C> resolve() {
      var frame = parent.resolve();
      return segment.fold(frame::pathChild, frame::captureChild);
    }

    @Override
    void name(StringBuilder buf) {
      buf.append("/");
      segment.fold(
          buf::append,
          c -> buf.append("{").append(c.name()).append("}"));
    }
  }

  public final class Fragment0 extends FollowupFragment {
    public Fragment0(AbstractFragment parent, String path) {
      super(parent, path);
    }

    public Fragment0(AbstractFragment parent, Capture.Attachment<?> capture) {
      super(parent, capture);
    }

    public Fragment0 path(String path) {
      return Paths.empty(path) ? this : new Fragment0(this, path);
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
      return new Fragment1<>(this, new Capture.Attachment<>("{" + name + "}", converter));
    }

    public void route(Block0 block) {
      resolve().run(block);
    }

    public void merge(RoutingDefinition<? super C> that) {
      resolve().merge(that.rootFrame);
    }
  }

  public final class Fragment1<T1> extends FollowupFragment{

    private final Curry1<T1> curry;

    private Fragment1(AbstractFragment parent, Curry1<T1> curry, String path) {
      super(parent, path);
      this.curry = curry;
    }

    private Fragment1(AbstractFragment parent, Capture.Attachment<T1> capture) {
      super(parent, capture);
      this.curry = Blocks.curry(capture);
    }

    public Fragment1<T1> path(String path) {
      return Paths.empty(path) ? this : new Fragment1<>(this, curry, path);
    }

    public Fragment2<T1, String> captureString() {
      return capture(STRING_NAME, Converter.asString());
    }

    public Fragment2<T1, String> captureString(String name) {
      return capture(name, Converter.asString());
    }

    public Fragment2<T1, Integer> captureInt() {
      return capture(INT_NAME, Converter.asInt());
    }

    public Fragment2<T1, Integer> captureInt(String name) {
      return capture(name, Converter.asInt());
    }

    public <T> Fragment2<T1, T> capture(Converter<? extends T> converter) {
      return capture(captureName(converter), converter);
    }

    public <T> Fragment2<T1, T> capture(String name, Converter<? extends T> converter) {
      return new Fragment2<>(this, curry, new Capture.Attachment<>(name, converter));
    }

    public void route(Block1<? super T1> block) {
      resolve().run(curry.runnable(block));
    }
  }

  public final class Fragment2<T1, T2> extends FollowupFragment {

    private final Curry2<T1, T2> curry;

    private Fragment2(AbstractFragment parent, Curry2<T1, T2> curry, String fixed) {
      super(parent, fixed);
      this.curry = curry;
    }

    private Fragment2(AbstractFragment parent, Curry1<T1> curry, Capture.Attachment<T2> capture) {
      super(parent, capture);
      this.curry = curry.append(capture);
    }

    public Fragment2<T1, T2> path(String path) {
      return Paths.empty(path) ? this : new Fragment2<>(this, curry, path);
    }

    public Fragment3<T1, T2, String> captureString() {
      return capture(STRING_NAME, Converter.asString());
    }

    public Fragment3<T1, T2, String> captureString(String name) {
      return capture(name, Converter.asString());
    }

    public Fragment3<T1, T2, Integer> captureInt() {
      return capture(INT_NAME, Converter.asInt());
    }

    public Fragment3<T1, T2, Integer> captureInt(String name) {
      return capture(name, Converter.asInt());
    }

    public <T> Fragment3<T1, T2, T> capture(Converter<? extends T> converter) {
      return capture(captureName(converter), converter);
    }

    public <T> Fragment3<T1, T2, T> capture(String name, Converter<? extends T> converter) {
      return new Fragment3<>(this, curry, new Capture.Attachment<>(name, converter));
    }


    public void route(Block2<? super T1, ? super T2> block) {
      resolve().run(curry.runnable(block));
    }
  }

  public final class Fragment3<T1, T2, T3> extends FollowupFragment {
    private final Curry3<T1, T2, T3> curry;

    private Fragment3(AbstractFragment parent, Curry3<T1, T2, T3> curry, String fixed) {
      super(parent, fixed);
      this.curry = curry;
    }

    private Fragment3(AbstractFragment parent, Curry2<T1, T2> curry, Capture.Attachment<T3> capture) {
      super(parent, capture);
      this.curry = curry.append(capture);
    }

    public Fragment3<T1, T2, T3> path(String path) {
      return Paths.empty(path) ? this : new Fragment3<>(this, curry, path);
    }

    public void route(Block3<? super T1, ? super T2, ? super T3> block) {
      resolve().run(curry.runnable(block));
    }
  }
}
