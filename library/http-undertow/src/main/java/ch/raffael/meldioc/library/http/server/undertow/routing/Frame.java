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

import ch.raffael.meldioc.library.http.server.undertow.codec.HttpDecoder;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpEncoder;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpObjectCodecFactory;
import ch.raffael.meldioc.library.http.server.undertow.codec.TextCodec;
import ch.raffael.meldioc.library.http.server.undertow.handler.AccessCheckHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.HttpMethodHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.PathSegmentHandler;
import ch.raffael.meldioc.library.http.server.undertow.routing.EndpointBuilder.LazyActionHandler;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Represents a stack frame in the routing DSL.
 */
final class Frame<C> {

  private final RoutingDefinition<C> routingDefinition;
  final Option<Frame<C>> parent;
  final RequestContextCapture<C> requestContext;

  private Map<String, Frame<C>> segments = HashMap.empty();
  private Map<HttpMethodHandler.Method, LazyActionHandler<C, ?, ?>> actions = HashMap.empty();
  private Option<Tuple2<Seq<Capture.Attachment<?>>, Frame<C>>> captures = none();
  Option<AccessCheckHandler.AccessRestriction> restriction = none();
  Option<HttpObjectCodecFactory<? super C>> objectCodecFactory = none();

  final StandardEncoders enc = new StandardEncoders();
  final StandardDecoders dec = new StandardDecoders();

  Frame(RoutingDefinition<C> routingDefinition, Option<Frame<C>> parent) {
    this.routingDefinition = routingDefinition;
    this.parent = parent;
    this.requestContext = parent.map(p -> p.requestContext).getOrElse(RequestContextCapture::new);
  }

  Frame<C> pathChild(String path) {
    var frame = this;
    for (var seg : Paths.segments(path)) {
      var child = frame.segments.get(seg);
      if (child.isEmpty()) {
        child = some(new Frame<>(frame.routingDefinition, some(frame)));
        frame.segments = frame.segments.put(seg, child.get());
      }
      frame = child.get();
    }
    return frame;
  }

  Frame<C> captureChild(Capture.Attachment<?> capture) {
    return captureChild(List.of(capture));
  }

  Frame<C> captureChild(Seq<Capture.Attachment<?>> capture) {
    captures = captures.orElse(some(Tuple.of(List.empty(), new Frame<>(routingDefinition, some(this)))))
        .map(t -> t.map1(s -> s.appendAll(capture)));
    return captures.get()._2;
  }

  private Frame<C> child(String segment) {
    var child = segments.get(segment);
    if (child.isEmpty()) {
      child = some(new Frame<>(routingDefinition, some(this)));
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

  void action(HttpMethodHandler.Method method, LazyActionHandler<C, ?, ?> handler) {
    if (actions.containsKey(method)) {
      throw new RoutingDefinitionException("Duplicate handler for method " + method);
    }
    actions = actions.put(method, handler);
  }

  HttpHandler deploy(Function<? super HttpServerExchange, ? extends C> contextFactory) {
    var routing = PathSegmentHandler.builder();
    actions.foldLeft(Option.<HttpMethodHandler>none(),
        (h, a) -> h.orElse(some(HttpMethodHandler.of(HashMap.empty())))
            .map(h2 -> h2.add(a._1, a._2.handler(contextFactory, this))))
        .forEach(routing::hereHandler);
    requestContext.deploy(contextFactory);
    segments.forEach(seg -> routing.exactSegment(seg._1, seg._2.deploy(contextFactory)));
    captures.forEach(cap -> routing.capture(cap._1.map(c -> c::capture), cap._2.deploy(contextFactory)));
    return restriction
        .map(r -> (HttpHandler) new AuthenticationConstraintHandler(new AuthenticationCallHandler(
            new AccessCheckHandler(r, routing.build()))))
        .getOrElse(routing::build);
  }

  void merge(Frame<? super C> that) {
    if (that.objectCodecFactory.isDefined()) {
      this.objectCodecFactory = some(that.objectCodecFactory.get());
    }
    that.actions.forEach(a -> action(a._1, a._2.covariant()));
    that.captures.forEach(thatCaps -> captureChild(thatCaps._1).merge(thatCaps._2));
    that.segments.forEach(thatSegs -> pathChild(thatSegs._1).merge(thatSegs._2));
  }

  private <T> Option<T> find(Function<? super Frame<C>, Option<T>> getter) {
    Option<T> current = getter.apply(this);
    return current.orElse(() -> parent.flatMap(p -> p.find(getter)));
  }

  public final class StandardDecoders {
    Option<HttpDecoder<? super C, ? super String>> plainText = none();

    private StandardDecoders() {
    }

    public HttpDecoder<? super C, ? super String> plainText() {
      return Frame.this.find(t -> t.dec.plainText)
          .getOrElse(TextCodec::plainText);
    }

    public <T> HttpDecoder<? super C, ? extends T> object(Class<T> type) {
      return Frame.this.<HttpDecoder<? super C, ? extends T>>find(
          f -> f.objectCodecFactory.flatMap(ocf -> ocf.decoder(type)))
          .getOrElseThrow(() -> new IllegalStateException("No object decoder for " + type));
    }
  }

  public final class StandardEncoders {
    Option<HttpEncoder<? super C, CharSequence>> plainText = none();
    Option<HttpEncoder<? super C, CharSequence>> html = none();
    Option<HttpEncoder<? super C, Object>> object = none();

    private StandardEncoders() {
    }

    public HttpEncoder<? super C, CharSequence> plainText() {
      return find(t -> t.enc.plainText)
          .getOrElse(TextCodec::plainText);
    }

    public HttpEncoder<? super C, CharSequence> html() {
      return Frame.this.find(t -> t.enc.html)
          .getOrElse(TextCodec::html);
    }

    public <T> HttpEncoder<? super C, ? super T> object(Class<T> type) {
      return Frame.this.<HttpEncoder<? super C, ? super T>>find(
          f -> f.objectCodecFactory.flatMap(ocf -> ocf.encoder(type)))
          .getOrElseThrow(() -> new IllegalStateException("No object decoder for " + type));
    }

  }

  private static final class RequestContextCapture<C> extends Capture<C> {
    private final AtomicReference<Function<? super HttpServerExchange, ? extends C>> context = new AtomicReference<>(null);

    private RequestContextCapture() {
      super("request-context");
    }

    @SuppressWarnings("ObjectEquality")
    private void deploy(Function<? super HttpServerExchange, ? extends C> context) {
      this.context.updateAndGet(v -> {
        if (v != null && v != context) {
          throw new IllegalStateException(this + " has already been deployed");
        }
        return context;
      });
    }

    @Override
    C get(HttpServerExchange exchange) {
      var context = this.context.get();
      if (context == null) {
        throw new IllegalStateException(this + " has not been deployed");
      }
      return context.apply(exchange);
    }
  }
}
