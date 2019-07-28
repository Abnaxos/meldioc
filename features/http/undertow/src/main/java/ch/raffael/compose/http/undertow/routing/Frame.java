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

package ch.raffael.compose.http.undertow.routing;

import ch.raffael.compose.http.undertow.codec.Decoder;
import ch.raffael.compose.http.undertow.codec.Encoder;
import ch.raffael.compose.http.undertow.codec.ObjectCodecFactory;
import ch.raffael.compose.http.undertow.codec.StringCodec;
import ch.raffael.compose.http.undertow.handler.HttpMethodHandler;
import ch.raffael.compose.http.undertow.handler.PathSegmentHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import java.util.function.Function;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
final class Frame<C> {

  private final RoutingDefinition<C> routingDefinition;
  final Option<Frame<C>> parent;

  private Map<HttpMethodHandler.Method, Function<Function<? super HttpServerExchange, ? extends C>, ? extends HttpHandler>> actions = Map();
  private Map<String, Frame<C>> segments = Map();
  private Option<Tuple2<Seq<Capture.Attachment<?>>, Frame<C>>> captures = None();

  Function<? super HttpHandler, ? extends HttpHandler> handlerWrapper = Function.identity();

  Option<ObjectCodecFactory<? super C>> objectCodecFactory = None();
  final StandardEncoders enc = new StandardEncoders();
  final StandardDecoders dec = new StandardDecoders();

  Frame(RoutingDefinition<C> routingDefinition, Option<Frame<C>> parent) {
    this.routingDefinition = routingDefinition;
    this.parent = parent;
  }

  Frame<C> pathChild(String path) {
    var frame = this;
    for (var seg : Paths.segments(path)) {
      var child = frame.segments.get(seg);
      if (child.isEmpty()) {
        child = Some(new Frame<>(frame.routingDefinition, Some(frame)));
        frame.segments = frame.segments.put(seg, child.get());
      }
      frame = child.get();
    }
    return frame;
  }

  Frame<C> captureChild(Capture.Attachment<?> capture) {
    captures = captures.orElse(Some(Tuple(Seq(), new Frame<>(routingDefinition, Some(this)))))
        .map(t -> t.map1(s -> s.append(capture)));
    return captures.get()._2;
  }

  private Frame<C> child(String segment) {
    var child = segments.get(segment);
    if (child.isEmpty()) {
      child = Some(new Frame<>(routingDefinition, Some(this)));
      segments = segments.put(segment, child.get());
    }
    return child.get();
  }

  void run(Blocks.Block0 block) {
    var prev = routingDefinition.currentFrame;
    try {
      routingDefinition.currentFrame = this;
      block.run();
    } finally {
      routingDefinition.currentFrame = prev;
    }
  }

  <T1> void run(Capture<? extends T1> p1,
                Blocks.Block1<? super T1> block) {
    var prev = routingDefinition.currentFrame;
    try {
      routingDefinition.currentFrame = this;
      block.run(p1);
    } finally {
      routingDefinition.currentFrame = prev;
    }
  }

  <T1, T2> void run(Capture<? extends T1> p1, Capture<? extends T2> p2,
                    Blocks.Block2<? super T1, ? super T2> block) {
    var prev = routingDefinition.currentFrame;
    try {
      routingDefinition.currentFrame = this;
      block.run(p1, p2);
    } finally {
      routingDefinition.currentFrame = prev;
    }
  }

  <T1, T2, T3> void run(Capture<? extends T1> p1, Capture<? extends T2> p2, Capture<? extends T3> p3,
                        Blocks.Block3<? super T1, ? super T2, ? super T3> block) {
    var prev = routingDefinition.currentFrame;
    try {
      routingDefinition.currentFrame = this;
      block.run(p1, p2, p3);
    } finally {
      routingDefinition.currentFrame = prev;
    }
  }

  void action(HttpMethodHandler.Method method,
              Function<Function<? super HttpServerExchange, ? extends C>, ? extends HttpHandler> handler) {
    if (actions.containsKey(method)) {
      throw new RoutingDefinitionException("Duplicate handler for method " + method);
    }
    actions = actions.put(method, handler);
  }

  HttpHandler handler(Function<? super HttpServerExchange, ? extends C> contextFactory) {
    var routing = PathSegmentHandler.builder();
    actions.foldLeft(Option.<HttpMethodHandler>none(),
        (h, a) -> h.orElse(Some(HttpMethodHandler.of(Map()))).map(h2 -> h2.add(a._1, a._2.apply(contextFactory))))
        .forEach(routing::hereHandler);
    segments.forEach(seg -> routing.exactSegment(seg._1, seg._2.handler(contextFactory)));
    captures.forEach(cap -> routing.capture(cap._1.map(c -> c::capture), cap._2.handler(contextFactory)));
    return routing.build();
  }

  private <T> Option<T> find(Function<? super Frame<C>, Option<T>> getter) {
    Option<T> current = getter.apply(this);
    return current.orElse(() -> parent.flatMap(p -> p.find(getter)));
  }

  public final class StandardEncoders {
    Option<Encoder<? super C, CharSequence>> plainText = None();
    Option<Encoder<? super C, CharSequence>> html = None();
    Option<Encoder<? super C, Object>> object = None();

    private StandardEncoders() {
    }

    public Encoder<? super C, CharSequence> plainText() {
      return find(t -> t.enc.plainText)
          .getOrElse(StringCodec::plainText);
    }

    public Encoder<? super C, CharSequence> html() {
      return Frame.this.find(t -> t.enc.html)
          .getOrElse(StringCodec::html);
    }

    public <T> Encoder<? super C, ? super T> object(Class<T> type) {
      return Frame.this.<Encoder<? super C, ? super T>>find(
          f -> f.objectCodecFactory.flatMap(ocf -> ocf.encoder(type)))
          .getOrElseThrow(() -> new IllegalStateException("No object decoder for " + type));
    }

  }

  public final class StandardDecoders {
    Option<Decoder<C, CharSequence>> plainText = None();

    private StandardDecoders() {
    }

    public Decoder<C, CharSequence> plainText() {
      return Frame.this.find(t -> t.dec.plainText)
          .getOrElseThrow(() -> new IllegalStateException("No plain text decoder"));
    }

    public <T> Decoder<? super C, ? extends T> object(Class<T> type) {
      return Frame.this.<Decoder<? super C, ? extends T>>find(
          f -> f.objectCodecFactory.flatMap(ocf -> ocf.decoder(type)))
          .getOrElseThrow(() -> new IllegalStateException("No object decoder for " + type));
    }

  }
}
