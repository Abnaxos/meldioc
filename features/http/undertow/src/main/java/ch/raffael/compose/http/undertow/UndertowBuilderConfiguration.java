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
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.vavr.collection.Seq;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.vavr.API.*;

/**
 * Acceptor for the base Undertow configuration.
 */
@ExtensionPoint.Acceptor
public class UndertowBuilderConfiguration<C> {

  public static final String ADDRESS_ALL = "0.0.0.0";
  public static final String ADDRESS_LOCAL = "localhost";

  private Seq<Consumer<? super Undertow.Builder>> listeners = Seq();
  private Seq<HandlerChainLink> handlerChain = Seq();
  @Nullable
  private Function<? super HttpServerExchange, ? extends C> contextFactory;
  private Seq<Consumer<? super Undertow>> postConstruct = Seq();
  private Seq<Consumer<? super Undertow>> postStart = Seq();

  public UndertowBuilderConfiguration<C> httpAll(int port) {
    return http(ADDRESS_ALL, port);
  }

  public UndertowBuilderConfiguration<C> httpLocal(int port) {
    return http(ADDRESS_LOCAL, port);
  }

  public UndertowBuilderConfiguration<C> http(String address, int port) {
    listeners = listeners.append(b -> b.addHttpListener(port, address));
    return this;
  }

  public UndertowBuilderConfiguration<C> httpAllSecure(int port, SSLContext sslContext) {
    return httpSecure(ADDRESS_ALL, port, sslContext);
  }

  public UndertowBuilderConfiguration<C> httpLocalSecure(int port, SSLContext sslContext) {
    return httpSecure(ADDRESS_LOCAL, port, sslContext);
  }

  public UndertowBuilderConfiguration<C> httpSecure(String address, int port, SSLContext sslContext) {
    listeners = listeners.append(b -> b.addHttpsListener(port, address, sslContext));
    return this;
  }

  public UndertowBuilderConfiguration<C> handler(HandlerChainLink handler) {
    handlerChain = handlerChain.append(handler);
    return this;
  }

  public UndertowBuilderConfiguration<C> compressHandler() {
    handler(n -> new EncodingHandler.Builder().build(java.util.Map.of()).wrap(n));
    return this;
  }

  public UndertowBuilderConfiguration<C> requestContextFactory(Function<? super HttpServerExchange, ? extends C> factory) {
    this.contextFactory = factory;
    return this;
  }

  public UndertowBuilderConfiguration<C> postConstruct(Consumer<? super Undertow> handler) {
    postConstruct = postConstruct.append(handler);
    return this;
  }

  public UndertowBuilderConfiguration<C> postStart(Consumer<? super Undertow> handler) {
    postStart = postStart.append(handler);
    return this;
  }

  @FunctionalInterface
  public interface HandlerChainLink {
    HttpHandler chain(HttpHandler next);
  }

  public static final class Holder<C> {
    private final UndertowBuilderConfiguration<C> acceptor = new UndertowBuilderConfiguration<>();

    private final Supplier<? extends Undertow.Builder> undertowBuilderSupplier;

    private Holder(Supplier<? extends Undertow.Builder> undertowBuilderSupplier) {
      this.undertowBuilderSupplier = undertowBuilderSupplier;
    }

    public static <C> Holder<C> create() {
      return new Holder<>(Undertow::builder);
    }

    public static <C> Holder<C> create(Supplier<? extends Undertow.Builder> undertowBuilderSupplier) {
      return new Holder<>(undertowBuilderSupplier);
    }

    public Holder<C> withRequestContextFactory(Function<? super HttpServerExchange, ? extends C> factory) {
      acceptor.requestContextFactory(factory);
      return this;
    }

    public UndertowBuilderConfiguration<C> acceptor() {
      return acceptor;
    }

    public Undertow.Builder configure(RoutingDefinition<? super C> routingDefinition) {
      return configure(cf -> new HttpStatusException.Handler(
          RoutingDefinition.createHandlerTree(routingDefinition, cf)));
    }

    public Undertow.Builder configure(Function<Function<? super HttpServerExchange, ? extends C>, ? extends HttpHandler> customHandler) {
      if (acceptor.contextFactory == null) {
        throw new IllegalStateException("No request context factory");
      }
      if (acceptor.listeners.isEmpty()) {
        throw new IllegalStateException("No listeners");
      }
      var builder = undertowBuilderSupplier.get();
      acceptor.listeners.forEach(l -> l.accept(builder));
      builder.setHandler(acceptor.handlerChain.foldRight(customHandler.apply(acceptor.contextFactory), HandlerChainLink::chain));
      return builder;
    }

    public Undertow configureAndStart(RoutingDefinition<? super C> routingDefinition) {
      return start(build((configure(routingDefinition))));
    }

    public Undertow configureAndStart(
        Function<Function<? super HttpServerExchange, ? extends C>, ? extends HttpHandler> customHandler) {
      return start(build(configure(customHandler)));
    }

    public Undertow build(Undertow.Builder builder) {
      var undertow = builder.build();
      acceptor.postConstruct.forEach(h -> h.accept(undertow));
      return undertow;
    }

    public Undertow start(Undertow undertow) {
      undertow.start();
      acceptor.postStart.forEach(h -> h.accept(undertow));
      return undertow;
    }
  }
}
