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
import ch.raffael.meldioc.library.http.server.undertow.util.HttpMethod;
import ch.raffael.meldioc.logging.Logging;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.vavr.Lazy;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Represents a stack frame in the routing DSL.
 */
final class Frame<C> {

  private static final Logger LOG = Logging.logger(RoutingDefinition.class);

  private final RoutingDefinition<C> routingDefinition;
  final Option<Frame<C>> parent;
  final DslTrace trace;
  final RequestContextCapture<C> requestContext;

  private Map<String, Frame<C>> pathSegments = HashMap.empty();
  private Seq<Capture.Attachment<?>> pathCaptures = List.empty();
  private final Lazy<Frame<C>> pathCaptureFrame;
  private Map<HttpMethod, EndpointBuilder<C, ?, ?>> endpoints = LinkedHashMap.empty();
  Option<AccessCheckHandler.AccessRestriction> restriction = none();
  Option<HttpObjectCodecFactory<? super C>> objectCodecFactory = none();

  final StandardEncoders enc = new StandardEncoders();
  final StandardDecoders dec = new StandardDecoders();

  Frame(RoutingDefinition<C> routingDefinition, DslTrace trace, Option<Frame<C>> parent) {
    this.routingDefinition = routingDefinition;
    this.parent = parent;
    this.trace = trace;
    this.requestContext = parent.map(p -> p.requestContext).getOrElse(RequestContextCapture::new);
    pathCaptureFrame = Lazy.of(
        () -> new Frame<>(routingDefinition, new DslTrace(trace, DslTrace.Kind.FRAME, "{}"), some(this)));
  }

  Frame<C> pathChild(String path) {
    var frame = this;
    for (var seg : Paths.segments(path)) {
      var child = frame.pathSegments.get(seg);
      if (child.isEmpty()) {
        child = some(new Frame<>(frame.routingDefinition, new DslTrace(trace, DslTrace.Kind.FRAME, seg), some(frame)));
        frame.pathSegments = frame.pathSegments.put(seg, child.get());
      }
      frame = child.get();
    }
    return frame;
  }

  Frame<C> captureChild(Capture.Attachment<?> capture) {
    return captureChild(List.of(capture));
  }

  Frame<C> captureChild(Seq<Capture.Attachment<?>> captures) {
    pathCaptures = pathCaptures.appendAll(captures);
    return pathCaptureFrame.get();
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

  EndpointBuilder.Method<C> endpoint(Set<HttpMethod> initialMethods) {
    initialMethods.forEach(HttpMethod::checkUserImplementable);
    var ep = new EndpointBuilder.Method<>(new DslTrace(trace, DslTrace.Kind.ENDPOINT),
        this::endpointUpdate, initialMethods);
    addEndpoint(ep);
    return ep;
  }

  EndpointBuilder.Method<C> endpoint(String path, Set<HttpMethod> initialMethods) {
    return pathChild(path).endpoint(initialMethods);
  }

  private void addEndpoint(EndpointBuilder<C, ?, ?> ep) {
    ep.methods.filter(endpoints::containsKey).headOption().forEach(m -> {
      throw duplicateEndpointException(m, ep);
    });
    ep.methods.forEach(m -> endpoints = endpoints.put(m, ep));
  }

  private void endpointUpdate(EndpointBuilder<C, ?, ?> prev, EndpointBuilder<C, ?, ?> current) {
    prev.methods.diff(current.methods).forEach(m -> endpoints = endpoints.remove(m));
    current.methods.forEach(m -> {
      if (!endpoints.get(m).map(p -> p.equals(prev)).getOrElse(true)) {
        throw duplicateEndpointException(m, current);
      }
      endpoints = endpoints.put(m, current);
    });
  }

  @Nonnull
  private RoutingDefinitionException duplicateEndpointException(HttpMethod m, EndpointBuilder<?, ?, ?> ep) {
    return new RoutingDefinitionException("Duplicate endpoint: " + endpointTrace(m, ep)
        + endpoints.get(m).map(p -> "\nPrevious endpoint: " + endpointTrace(m, p)).getOrElse(""));
  }

  HttpHandler materialize(Function<? super HttpServerExchange, ? extends C> contextFactory) {
    var routing = PathSegmentHandler.builder();
    endpoints.foldLeft(Option.<HttpMethodHandler>none(),
        (h, a) -> h.orElse(some(HttpMethodHandler.of(HashMap.empty())))
            .map(h2 -> {
              if (LOG.isDebugEnabled()) {
                LOG.debug("Materializing endpoint: {}", endpointTrace(a));
              }
              return h2.add(a._1,
                  a._2.handler(this, contextFactory)
                      .fallbackEncoder(() -> find(f -> f.objectCodecFactory)
                          .flatMap(f -> f.encoder(Object.class))
                          .getOrElseThrow(() ->
                              new RoutingDefinitionException("No object codec set: " + endpointTrace(a)))));
            }))
        .forEach(routing::hereHandler);
    requestContext.materialize(contextFactory);
    pathSegments.forEach(seg -> routing.exactSegment(seg._1, seg._2.materialize(contextFactory)));
    if (!pathCaptures.isEmpty()) {
      routing.capture(pathCaptures.map(c -> c::capture), pathCaptureFrame.get().materialize(contextFactory));
    }
    return restriction
        .map(r -> (HttpHandler) new AuthenticationConstraintHandler(new AuthenticationCallHandler(
            new AccessCheckHandler(r, routing.build()))))
        .getOrElse(routing::build);
  }

  private String endpointTrace(Tuple2<HttpMethod, ? extends EndpointBuilder<?, ?, ?>> ep) {
    return endpointTrace(ep._1(), ep._2());
  }

  private String endpointTrace(HttpMethod method, EndpointBuilder<?, ?, ?> ep) {
    return method + " " + ep.trace.stackTrace();
  }

  void merge(Frame<? super C> that) {
    merge(new DslTrace(trace, DslTrace.Kind.MERGE), that);
  }

  private void merge(DslTrace mergeTrace, Frame<? super C> that) {
    if (that.objectCodecFactory.isDefined()) {
      this.objectCodecFactory = some(that.objectCodecFactory.get());
    }
    // for loop instead of forEach to keep the stack trace clean for DslTrace:
    for (EndpointBuilder<? super C, ?, ?> ep : that.endpoints.values()) {
      addEndpoint(ep.fork(ep.trace.reroot(mergeTrace), this::endpointUpdate));
    }
    if (!that.pathCaptures.isEmpty()) {
      pathCaptures = pathCaptures.appendAll(that.pathCaptures);
      pathCaptureFrame.get().merge(mergeTrace, that.pathCaptureFrame.get());
    }
    that.pathSegments.forEach(thatSegs -> pathChild(thatSegs._1).merge(mergeTrace, thatSegs._2));
  }

  <T> Option<T> find(Function<? super Frame<C>, Option<T>> getter) {
    Option<T> current = getter.apply(this);
    return current.orElse(() -> parent.flatMap(p -> p.find(getter)));
  }

  public final class StandardDecoders {
    Option<HttpDecoder<? super C, ? extends String>> plainText = none();

    private StandardDecoders() {
    }

    public HttpDecoder<? super C, ? extends String> plainText() {
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
    private void materialize(Function<? super HttpServerExchange, ? extends C> context) {
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
