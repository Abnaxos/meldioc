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

package ch.raffael.compose.http.undertow;

import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.http.undertow.routing.RoutingDefinition;
import ch.raffael.compose.http.undertow.routing.RoutingDefinitions;
import ch.raffael.compose.util.compose.AbstractExtensionPoint;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.vavr.collection.Seq;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static ch.raffael.compose.logging.Logging.logger;
import static io.vavr.API.*;

/**
 * Acceptor for the base Undertow configuration.
 */
@ExtensionPoint.Acceptor
public final class UndertowBlueprint<C> {

  private static final Logger LOG = logger();

  public static final String ADDRESS_ALL = "0.0.0.0";
  public static final String ADDRESS_LOCAL = "localhost";

  private static final BiFunction<Object, HttpHandler, HttpHandler> COMPRESS =
      (__, n) -> new EncodingHandler.Builder().build(java.util.Map.of()).wrap(n);

  private Seq<Consumer<? super Undertow.Builder>> listeners = Seq();
  private
  Seq<BiFunction<? super Function<? super HttpServerExchange, ? extends C>, ? super HttpHandler, ? extends HttpHandler>>
      handlerChain = Seq(COMPRESS);

  @Nullable
  private Function<? super Function<? super HttpServerExchange, ? extends C>, ? extends HttpHandler> mainHandler = null;
  @Nullable
  private Function<? super HttpServerExchange, ? extends C> contextFactory;
  private Seq<Consumer<? super Undertow>> postConstruct = Seq();
  private Seq<Consumer<? super Undertow>> postStart = Seq();

  public static <C> EP<C> holder() {
    return holder(Undertow::builder);
  }

  public static <C> EP<C> holder(
      Supplier<? extends Undertow.Builder> undertowBuilderSupplier) {
    return new EP<>(new UndertowBlueprint<>(), undertowBuilderSupplier);
  }

  public UndertowBlueprint<C> httpAll(int port) {
    return http(ADDRESS_ALL, port);
  }

  public UndertowBlueprint<C> httpLocal(int port) {
    return http(ADDRESS_LOCAL, port);
  }

  public UndertowBlueprint<C> http(String address, int port) {
    listeners = listeners.append(b -> b.addHttpListener(port, address));
    return this;
  }

  public synchronized UndertowBlueprint<C> httpAllSecure(int port, SSLContext sslContext) {
    return httpSecure(ADDRESS_ALL, port, sslContext);
  }

  public synchronized UndertowBlueprint<C> httpLocalSecure(int port, SSLContext sslContext) {
    return httpSecure(ADDRESS_LOCAL, port, sslContext);
  }

  public synchronized UndertowBlueprint<C> httpSecure(String address, int port, SSLContext sslContext) {
    listeners = listeners.append(b -> b.addHttpsListener(port, address, sslContext));
    return this;
  }

  public UndertowBlueprint<C> handler(
      BiFunction<? super Function<? super HttpServerExchange, ? extends C>, ? super HttpHandler, ? extends HttpHandler>
          handler) {
    handlerChain = handlerChain.append(handler);
    return this;
  }

  public UndertowBlueprint<C> handler(Function<? super HttpHandler, ? extends HttpHandler> handler) {
    handlerChain = handlerChain.append((__, n) -> handler.apply(n));
    return this;
  }

  public UndertowBlueprint<C> mainHandler(
      Function<? super Function<? super HttpServerExchange, ? extends C>, ? extends HttpHandler> mainHandler) {
    this.mainHandler = mainHandler;
    return this;
  }

  public UndertowBlueprint<C> mainHandler(RoutingDefinition<? super C> routing) {
    return mainHandler(ctx -> RoutingDefinitions.buildHandlerTree(routing, ctx));
  }

  public UndertowBlueprint<C> disableCompression() {
    handlerChain = handlerChain.remove(COMPRESS);
    return this;
  }

  public UndertowBlueprint<C> requestContextFactory(
      Function<? super HttpServerExchange, ? extends C> factory) {
    this.contextFactory = factory;
    return this;
  }

  public UndertowBlueprint<C> postConstruct(Consumer<? super Undertow> consumer) {
    postConstruct = postConstruct.append(consumer);
    return this;
  }

  public UndertowBlueprint<C> postStart(Consumer<? super Undertow> consumer) {
    postStart = postStart.append(consumer);
    return this;
  }

  public static class EP<C> extends AbstractExtensionPoint<UndertowBlueprint<C>, Undertow> {
    private final Supplier<? extends Undertow.Builder> undertowBuilderSupplier;

    protected EP(UndertowBlueprint<C> blueprint, Supplier<? extends Undertow.Builder> undertowBuilderSupplier) {
      super(blueprint);
      this.undertowBuilderSupplier = undertowBuilderSupplier;
    }

    public EP withRequestContextFactory(Function<? super HttpServerExchange, ? extends C> factory) {
      acceptor().requestContextFactory(factory);
      return this;
    }

    @Override
    public Undertow apply() {
      return apply(publishedAcceptor());
    }

    @Override
    protected Undertow apply(UndertowBlueprint<C> acceptor) {
      return autoStart(acceptor, build(acceptor, prepareBuilder(acceptor)));
    }

    protected Undertow.Builder prepareBuilder(UndertowBlueprint<C> acceptor) {
      if (acceptor.contextFactory == null) {
        throw new IllegalStateException("No request context factory");
      }
      var contextStore = new RequestContextStore<C>(acceptor.contextFactory);
      if (acceptor.listeners.isEmpty()) {
        throw new IllegalStateException("No listeners");
      }
      if (acceptor.mainHandler == null) {
        throw new IllegalStateException("No main handler");
      }
      var builder = undertowBuilderSupplier.get();
      acceptor.listeners.forEach(l -> l.accept(builder));
      builder.setHandler(acceptor.handlerChain.foldRight((HttpHandler)acceptor.mainHandler.apply(contextStore),
          (self, next) -> self.apply(contextStore, next)));
      return builder;
    }

    protected Undertow build(UndertowBlueprint<C> acceptor, Undertow.Builder builder) {
      var undertow = builder.build();
      acceptor.postConstruct.forEach(h -> h.accept(undertow));
      return undertow;
    }

    protected Undertow start(UndertowBlueprint<C> acceptor, Undertow undertow) {
      undertow.start();
      publishedAcceptor().postStart.forEach(h -> h.accept(undertow));
      return undertow;
    }

    protected Undertow autoStart(UndertowBlueprint<C> acceptor, Undertow undertow) {
      return start(acceptor, undertow);
    }
  }
}
