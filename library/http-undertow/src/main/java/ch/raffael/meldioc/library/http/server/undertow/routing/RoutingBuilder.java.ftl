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
[#compress]
  [#import "/parameters.ftl" as p]
  [#import "/codegen.ftl" as c]

  [#function nextFragment n nextType typevar="T#"]
    [#local prev = ""]
    [#if n>0]
      [#local prev = c.joinf(1..*n typevar)+", "]
    [/#if]
    [#return "Fragment"+(n+1)+"<"+prev+nextType+">"]
  [/#function]
[/#compress]

[#list 0..p.pcount as i]
import ch.raffael.meldioc.library.http.server.undertow.routing.Blocks.Block${i};
[/#list]
[#list 1..p.pcount as i]
import ch.raffael.meldioc.library.http.server.undertow.routing.Blocks.Curry${i};
[/#list]
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

  [#list 0..p.pcount as i]
  public final class Fragment${i}${c.tvars(1..*i)} extends FollowupFragment {
    [#if i == 0]
    private Fragment${i}(AbstractFragment parent, String path) {
      super(parent, path);
    }

    private Fragment${i}(AbstractFragment parent, Capture.Attachment<?> capture) {
      super(parent, capture);
    }
    [#else]
    private final Curry${c.ntype(1..*i ">:T#")} curry;

    private Fragment${i}(AbstractFragment parent, Curry${c.ntype(1..*i ">:T#")} curry, String path) {
      super(parent, path);
      this.curry = curry;
    }

    private Fragment${i}(AbstractFragment parent, [#if i>1]Curry${c.ntype(1..*(i-1) ">:T#")} curry, [/#if]Capture.Attachment< ? extends T${i}> capture) {
      super(parent, capture);
      [#-- noinspection ResultOfMethodCallIgnored --]
      this.curry = [#if i>1]curry.append(capture)[#else]Blocks.curry(capture)[/#if];
    }
    [/#if]

    public Fragment${i}${c.tvars(1..*i)} path(String path) {
      return Paths.empty(path) ? this : new Fragment${i}[#if i>0]<>[/#if](this, [#if i>0]curry, [/#if]path);
    }

    [#if ! i?is_last]

    public ${nextFragment(i "String")} captureString() {
      return capture(STRING_NAME, Converter.asString());
    }

    public ${nextFragment(i "String")} captureString(String name) {
      return capture(name, Converter.asString());
    }

    public ${nextFragment(i "Integer")} captureInt() {
      return capture(STRING_NAME, Converter.asInt());
    }

    public ${nextFragment(i "Integer")} captureInt(String name) {
      return capture(name, Converter.asInt());
    }

    public <TC> ${nextFragment(i "TC")} capture(Converter<? extends TC> converter) {
      return capture(captureName(converter), converter);
    }

    public <TC> ${nextFragment(i "TC")} capture(String name, Converter<? extends TC> converter) {
      return new Fragment${i+1}<>(this, [#if i+1>1]curry, [/#if]new Capture.Attachment<>(name, converter));
    }
    [/#if]

    public void route(Block${c.ntype(1..*i)} block) {
      [#if i == 0]
      resolve().run(block);
      [#else]
      resolve().run(curry.runnable(block));
      [/#if]
    }

    [#if i == 0]
    public void merge(RoutingDefinition<? super C> that) {
      resolve().merge(that.rootFrame);
    }
    [/#if]
  }

  [/#list]
}
