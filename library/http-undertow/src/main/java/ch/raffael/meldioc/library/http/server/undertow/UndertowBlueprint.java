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

package ch.raffael.meldioc.library.http.server.undertow;

import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.library.http.server.undertow.handler.AdvisedDispatchHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.DispatchToWorkerHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.ErrorMessageHandler;
import ch.raffael.meldioc.library.http.server.undertow.routing.RoutingDefinition;
import ch.raffael.meldioc.library.http.server.undertow.routing.RoutingDefinitions;
import ch.raffael.meldioc.library.http.server.undertow.util.RequestContextStore;
import ch.raffael.meldioc.util.advice.AroundAdvice;
import io.undertow.Undertow;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Acceptor for the base Undertow configuration.
 */
@ExtensionPoint
public final class UndertowBlueprint<C> {

  public static final String ADDRESS_ALL = "0.0.0.0";
  public static final String ADDRESS_LOCAL = "localhost";

  private static final BiFunction<Object, HttpHandler, HttpHandler> DISPATCH =
      (__, n) -> new DispatchToWorkerHandler(n);
  private static final BiFunction<Object, HttpHandler, HttpHandler> COMPRESS =
      (__, n) -> new EncodingHandler.Builder().build(java.util.Map.of()).wrap(n);
  private static final BiFunction<Object, HttpHandler, HttpHandler> ERROR =
      (__, n) -> new ErrorMessageHandler(n);

  private Seq<Consumer<? super Undertow.Builder>> listeners = List.empty();
  private
  Seq<BiFunction<? super Function<? super HttpServerExchange, ? extends C>, ? super HttpHandler, ? extends HttpHandler>>
      handlerChain = List.of(DISPATCH, COMPRESS, ERROR);

  @Nullable
  private Function<? super Function<? super HttpServerExchange, ? extends C>, ? extends HttpHandler> mainHandler = null;
  @Nullable
  private Function<? super HttpServerExchange, ? extends C> contextFactory;
  private Seq<Consumer<? super Undertow>> postConstruct = List.empty();
  private Seq<Consumer<? super Undertow>> postStart = List.empty();
  private Option<? extends Supplier<? extends AroundAdvice>> dispatchAdvice = none();

  @Deprecated(forRemoval = true)
  public static <C> EP<C> holder() {
    return holder(Undertow::builder);
  }

  public static <C> EP<C> holder(Supplier<? extends Undertow.Builder> undertowBuilderSupplier) {
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

  public UndertowBlueprint<C> prependHandler(
      BiFunction<? super Function<? super HttpServerExchange, ? extends C>, ? super HttpHandler, ? extends HttpHandler>
          handler) {
    handlerChain = handlerChain.prepend(handler);
    return this;
  }

  public UndertowBlueprint<C> prependHandler(Function<? super HttpHandler, ? extends HttpHandler> handler) {
    handlerChain = handlerChain.prepend((__, n) -> handler.apply(n));
    return this;
  }

  public UndertowBlueprint<C> dispatchAdvice(Supplier<? extends AroundAdvice> dispatchAdvice) {
    return dispatchAdvice(some(dispatchAdvice));
  }

  public UndertowBlueprint<C> dispatchAdvice(Option<? extends Supplier<? extends AroundAdvice>> dispatchAdvice) {
    this.dispatchAdvice = dispatchAdvice;
    return this;
  }

  public SecurityBuilder<C> security(IdentityManager identityManager) {
    return new SecurityBuilder<>(this, identityManager);
  }

  public UndertowBlueprint<C> security(IdentityManager identityManager, AuthenticationMechanism... mechanisms) {
    return new SecurityBuilder<>(this, identityManager).mechanism(mechanisms).end();
  }

  public UndertowBlueprint<C> security(IdentityManager identityManager,
                                       Iterable<? extends AuthenticationMechanism> mechanisms) {
    return new SecurityBuilder<>(this, identityManager).mechanism(mechanisms).end();
  }

  public UndertowBlueprint<C> basicSecurity(IdentityManager identityManager) {
    return new SecurityBuilder<>(this, identityManager).basicAuth().end();
  }

  public UndertowBlueprint<C> basicSecurity(IdentityManager identityManager, String realm) {
    return new SecurityBuilder<>(this, identityManager).basicAuth(realm).end();
  }

  public UndertowBlueprint<C> customMainHandler(
      Function<? super Function<? super HttpServerExchange, ? extends C>, ? extends HttpHandler> mainHandler) {
    this.mainHandler = mainHandler;
    return this;
  }

  public UndertowBlueprint<C> routing(Supplier<? extends RoutingDefinition<? super C>> routing) {
    return customMainHandler(ctx -> RoutingDefinitions.materialize(routing.get(), ctx));
  }

  /**
   * @deprecated Use {@link #customMainHandler(Function)} instead.
   */
  @Deprecated(forRemoval = true)
  public UndertowBlueprint<C> mainHandler(
      Function<? super Function<? super HttpServerExchange, ? extends C>, ? extends HttpHandler> mainHandler) {
    customMainHandler(mainHandler);
    return this;
  }

  /**
   * @deprecated Use {@link #routing(Supplier)} instead.
   */
  @Deprecated(forRemoval = true)
  public UndertowBlueprint<C> mainHandler(RoutingDefinition<? super C> routing) {
    return routing(() -> routing);
  }

  public UndertowBlueprint<C> disableCompression() {
    handlerChain = handlerChain.remove(COMPRESS);
    return this;
  }

  public UndertowBlueprint<C> disableStandardErrorHandler() {
    handlerChain = handlerChain.remove(ERROR);
    return this;
  }

  public UndertowBlueprint<C> disableEarlyDispatch() {
    handlerChain = handlerChain.remove(DISPATCH);
    return this;
  }

  public UndertowBlueprint<C> enableStackTraces() {
    return enableStackTraces(true);
  }

  public UndertowBlueprint<C> enableStackTraces(boolean enable) {
    if (enable) {
      handlerChain = handlerChain.prepend(
          (__, n) -> ErrorMessageHandler.ExceptionRenderer.enableStackTracesHandler(n));
    }
    return this;
  }

  public UndertowBlueprint<C> enableStackTraces(Supplier<Boolean> enable) {
    handlerChain = handlerChain.prepend(
        (__, n) -> ErrorMessageHandler.ExceptionRenderer.enableStackTracesHandler(n, enable));
    return this;
  }

  public UndertowBlueprint<C> enableStackTraces(Predicate<? super HttpServerExchange> enable) {
    handlerChain = handlerChain.prepend(
        (__, n) -> ErrorMessageHandler.ExceptionRenderer.enableStackTracesHandler(n, enable));
    return this;
  }

  @Deprecated(forRemoval = true)
  public UndertowBlueprint<C> suppressStackTraces() {
    return enableStackTraces(false);
  }

  @Deprecated(forRemoval = true)
  public UndertowBlueprint<C> suppressStackTraces(boolean suppress) {
    return enableStackTraces(!suppress);
  }

  @Deprecated(forRemoval = true)
  public UndertowBlueprint<C> suppressStackTraces(Supplier<Boolean> suppress) {
    return enableStackTraces(() -> {
      var result = suppress.get();
      return result == null ? null : !result;
    });
  }

  @Deprecated(forRemoval = true)
  public UndertowBlueprint<C> suppressStackTraces(Predicate<? super HttpServerExchange> suppress) {
    return enableStackTraces(e -> !suppress.test(e));
  }

  public UndertowBlueprint<C> clearHandlerChain() {
    handlerChain = List.empty();
    return this;
  }

  public UndertowBlueprint<C> addCompressionHandler() {
    return addStandardHandler(COMPRESS);
  }

  public UndertowBlueprint<C> addStandardErrorHandler() {
    return addStandardHandler(ERROR);
  }

  public UndertowBlueprint<C> addDispatchHandler() {
    return addStandardHandler(DISPATCH);
  }

  private UndertowBlueprint<C> addStandardHandler(BiFunction<Object, HttpHandler, HttpHandler> handler) {
    if (!handlerChain.contains(handler)) {
      handlerChain = handlerChain.append(handler);
    }
    return this;
  }

  public UndertowBlueprint<C> requestContextFactory(Function<? super HttpServerExchange, ? extends C> factory) {
    this.contextFactory = factory;
    return this;
  }

  public UndertowBlueprint<C> requestContextFactory(Supplier<? extends C> factory) {
    return requestContextFactory(__ -> factory.get());
  }

  public UndertowBlueprint<C> postConstruct(Consumer<? super Undertow> consumer) {
    postConstruct = postConstruct.append(consumer);
    return this;
  }

  public UndertowBlueprint<C> postStart(Consumer<? super Undertow> consumer) {
    postStart = postStart.append(consumer);
    return this;
  }

  public static final class SecurityBuilder<C> {
    private final UndertowBlueprint<C> parent;
    private final IdentityManager identityManager;
    private AuthenticationMode authenticationMode = AuthenticationMode.CONSTRAINT_DRIVEN;

    private Seq<AuthenticationMechanism> mechanisms = List.empty();

    private SecurityBuilder(UndertowBlueprint<C> parent, IdentityManager identityManager) {
      this.parent = parent;
      this.identityManager = identityManager;
    }

    public SecurityBuilder<C> constraintDriven() {
      return authenticationMode(AuthenticationMode.CONSTRAINT_DRIVEN);
    }

    public SecurityBuilder<C> proActive() {
      return authenticationMode(AuthenticationMode.PRO_ACTIVE);
    }

    public SecurityBuilder<C> authenticationMode(AuthenticationMode mode) {
      this.authenticationMode = mode;
      return this;
    }

    public SecurityBuilder<C> mechanism(AuthenticationMechanism... mechanisms) {
      return mechanism(Arrays.asList(mechanisms));
    }

    public SecurityBuilder<C> mechanism(Iterable<? extends AuthenticationMechanism> mechanisms) {
      this.mechanisms = this.mechanisms.appendAll(mechanisms);
      return this;
    }

    public SecurityBuilder<C> basicAuth() {
      return mechanism(new BasicAuthenticationMechanism("undertow"));
    }

    public SecurityBuilder<C> basicAuth(String realm) {
      return mechanism(new BasicAuthenticationMechanism(realm));
    }

    public UndertowBlueprint<C> end() {
      var mechanisms = Option.of(this.mechanisms)
          .filter(l -> !l.isEmpty())
          .map(Seq::asJava)
          .getOrElseThrow(() -> new IllegalStateException("No security mechanisms"));
      return parent.handler(n -> new SecurityInitialHandler(authenticationMode, identityManager,
          new AuthenticationMechanismsHandler(n, mechanisms)));
    }
  }

  public static class EP<C>  {
    private final UndertowBlueprint<C> blueprint;
    private final Supplier<? extends Undertow.Builder> undertowBuilderSupplier;

    protected EP(UndertowBlueprint<C> blueprint, Supplier<? extends Undertow.Builder> undertowBuilderSupplier) {
      this.blueprint = blueprint;
      this.undertowBuilderSupplier = undertowBuilderSupplier;
    }

    public UndertowBlueprint<C> acceptor() {
      return blueprint;
    }

    public EP<C> withRequestContextFactory(Function<? super HttpServerExchange, ? extends C> factory) {
      blueprint.requestContextFactory(factory);
      return this;
    }

    protected Undertow apply() {
      return autoStart(blueprint, build(blueprint, prepareBuilder(blueprint)));
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
      HttpHandler handler = acceptor.handlerChain.foldRight((HttpHandler) acceptor.mainHandler.apply(contextStore),
          (self, next) -> self.apply(contextStore, next));
      builder.setHandler(AdvisedDispatchHandler.prepend(handler, acceptor.dispatchAdvice));
      return builder;
    }

    protected Undertow build(UndertowBlueprint<C> acceptor, Undertow.Builder builder) {
      var undertow = builder.build();
      acceptor.postConstruct.forEach(h -> h.accept(undertow));
      return undertow;
    }

    protected Undertow start(UndertowBlueprint<C> acceptor, Undertow undertow) {
      undertow.start();
      blueprint.postStart.forEach(h -> h.accept(undertow));
      return undertow;
    }

    protected Undertow autoStart(UndertowBlueprint<C> acceptor, Undertow undertow) {
      return start(acceptor, undertow);
    }
  }
}
