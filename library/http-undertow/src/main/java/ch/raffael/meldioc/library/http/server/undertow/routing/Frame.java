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

import java.util.function.Function;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Represents a stack frame in the routing DSL.
 */
final class Frame {

  private static final Logger LOG = Logging.logger(RoutingDefinition.class);

  private final RoutingDefinition routingDefinition;
  final Option<Frame> parent;
  DslTrace trace;

  private Map<String, Frame> pathSegments = HashMap.empty();
  private Seq<Capture.Attachment<?>> pathCaptures = List.empty();
  private final Lazy<Frame> pathCaptureFrame;
  private Map<HttpMethod, EndpointBuilder<?, ?>> endpoints = LinkedHashMap.empty();
  Option<AccessCheckHandler.AccessRestriction> restriction = none();
  Option<HttpObjectCodecFactory> objectCodecFactory = none();

  private Seq<Function<? super HttpHandler, ? extends HttpHandler>> handlers = List.empty();

  final StandardEncoders enc = new StandardEncoders();
  final StandardDecoders dec = new StandardDecoders();

  Frame(RoutingDefinition routingDefinition, DslTrace trace, Option<Frame> parent) {
    this.routingDefinition = routingDefinition;
    this.parent = parent;
    this.trace = trace;
    pathCaptureFrame = Lazy.of(
        () -> new Frame(routingDefinition, captureTrace(none()), some(this)));
  }

  Frame pathChild(String path) {
    var frame = this;
    for (var seg : Paths.segments(path)) {
      var child = frame.pathSegments.get(seg);
      if (child.isEmpty()) {
        child = some(new Frame(frame.routingDefinition, new DslTrace(trace, DslTrace.Kind.FRAME, seg), some(frame)));
        frame.pathSegments = frame.pathSegments.put(seg, child.get());
      }
      frame = child.get();
    }
    return frame;
  }

  Frame captureChild(Capture.Attachment<?> capture) {
    var f = pathCaptureFrame.get();
    pathCaptures = pathCaptures.append(capture);
    f.trace = captureTrace(some(capture));
    return f;
  }

  private DslTrace captureTrace(Option<Capture<?>> capture) {
    return new DslTrace(trace, DslTrace.Kind.FRAME,
        "{" + capture.map(Capture::name).getOrElse("") + "}");
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

  EndpointBuilder.Method endpoint(Set<HttpMethod> initialMethods) {
    initialMethods.forEach(HttpMethod::checkUserImplementable);
    var ep = new EndpointBuilder.Method(new DslTrace(trace, DslTrace.Kind.ENDPOINT),
        this::endpointUpdate, initialMethods);
    addEndpoint(ep);
    return ep;
  }

  EndpointBuilder.Method endpoint(String path, Set<HttpMethod> initialMethods) {
    return pathChild(path).endpoint(initialMethods);
  }

  private void addEndpoint(EndpointBuilder<?, ?> ep) {
    ep.methods.filter(endpoints::containsKey).headOption().forEach(m -> {
      throw duplicateEndpointException(m, ep);
    });
    ep.methods.forEach(m -> endpoints = endpoints.put(m, ep));
  }

  private void endpointUpdate(EndpointBuilder<?, ?> prev, EndpointBuilder<?, ?> current) {
    prev.methods.diff(current.methods).forEach(m -> endpoints = endpoints.remove(m));
    current.methods.forEach(m -> {
      if (!endpoints.get(m).map(p -> p.equals(prev)).getOrElse(true)) {
        throw duplicateEndpointException(m, current);
      }
      endpoints = endpoints.put(m, current);
    });
  }

  void handler(Function<? super HttpHandler, ? extends HttpHandler> handler) {
    handlers = handlers.append(handler);
  }

  private RoutingDefinitionException duplicateEndpointException(HttpMethod m, EndpointBuilder<?, ?> ep) {
    return new RoutingDefinitionException("Duplicate endpoint: " + endpointTrace(m, ep)
        + endpoints.get(m).map(p -> "\nPrevious endpoint: " + endpointTrace(m, p)).getOrElse(""));
  }

  HttpHandler materialize() {
    var routing = PathSegmentHandler.builder();
    endpoints.foldLeft(Option.<HttpMethodHandler>none(),
        (h, a) -> h.orElse(some(HttpMethodHandler.of(HashMap.empty())))
            .map(h2 -> {
              if (LOG.isDebugEnabled()) {
                LOG.debug("Materializing endpoint: {}", endpointTrace(a));
              }
              return h2.add(a._1,
                  a._2.handler(this)
                      .fallbackEncoder(() -> find(f -> f.objectCodecFactory)
                          .flatMap(f -> f.encoder(Object.class))
                          .getOrElseThrow(() ->
                              new RoutingDefinitionException("No object codec set: " + endpointTrace(a)))));
            }))
        .forEach(routing::hereHandler);
    pathSegments.forEach(seg -> routing.exactSegment(seg._1, seg._2.materialize()));
    if (!pathCaptures.isEmpty()) {
      routing.capture(pathCaptures.map(c -> c::capture), pathCaptureFrame.get().materialize());
    }
    return handlers.foldLeft(restriction
                .map(r -> (HttpHandler) new AuthenticationConstraintHandler(new AuthenticationCallHandler(
                      new AccessCheckHandler(r, routing.build()))))
                .getOrElse(routing::build),
          (p, n) -> n.apply(p));
  }

  private String endpointTrace(Tuple2<HttpMethod, ? extends EndpointBuilder<?, ?>> ep) {
    return endpointTrace(ep._1(), ep._2());
  }

  private String endpointTrace(HttpMethod method, EndpointBuilder<?, ?> ep) {
    return method + " " + ep.trace.stackTrace();
  }

  void merge(Frame that) {
    merge(new DslTrace(trace, DslTrace.Kind.MERGE), that);
  }

  private void merge(DslTrace mergeTrace, Frame that) {
    if (that.objectCodecFactory.isDefined()) {
      this.objectCodecFactory = some(that.objectCodecFactory.get());
    }
    // for loop instead of forEach to keep the stack trace clean for DslTrace:
    for (EndpointBuilder<?, ?> ep : that.endpoints.values().distinct()) {
      addEndpoint(ep.fork(ep.trace.reroot(mergeTrace), this::endpointUpdate));
    }
    if (!that.pathCaptures.isEmpty()) {
      pathCaptures = pathCaptures.appendAll(that.pathCaptures);
      pathCaptureFrame.get().merge(mergeTrace, that.pathCaptureFrame.get());
    }
    that.pathSegments.forEach(thatSegs -> pathChild(thatSegs._1).merge(mergeTrace, thatSegs._2));
    this.handlers = this.handlers.appendAll(that.handlers);
  }

  <T> Option<T> find(Function<? super Frame, Option<T>> getter) {
    Option<T> current = getter.apply(this);
    return current.orElse(() -> parent.flatMap(p -> p.find(getter)));
  }

  public final class StandardDecoders {
    Option<HttpDecoder<? extends String>> plainText = none();

    private StandardDecoders() {
    }

    public HttpDecoder<? extends String> plainText() {
      return Frame.this.find(t -> t.dec.plainText)
          .getOrElse(() -> TextCodec.plainText());
    }

    public <T> HttpDecoder<? extends T> object(Class<T> type) {
      return Frame.this.find(
          f -> f.objectCodecFactory.flatMap(ocf -> ocf.decoder(type)))
          .getOrElseThrow(() -> new IllegalStateException("No object decoder for " + type));
    }
  }

  public final class StandardEncoders {
    Option<HttpEncoder<CharSequence>> plainText = none();
    Option<HttpEncoder<CharSequence>> html = none();

    private StandardEncoders() {
    }

    public HttpEncoder<CharSequence> plainText() {
      return find(t -> t.enc.plainText)
          .getOrElse(() -> TextCodec.plainText());
    }

    public HttpEncoder<CharSequence> html() {
      return Frame.this.find(t -> t.enc.html)
          .getOrElse(() -> TextCodec.html());
    }

    public <T> HttpEncoder<? super T> object(Class<T> type) {
      return Frame.this.find(
          f -> f.objectCodecFactory.flatMap(ocf -> ocf.encoder(type)))
          .getOrElseThrow(() -> new IllegalStateException("No object decoder for " + type));
    }

  }
}
