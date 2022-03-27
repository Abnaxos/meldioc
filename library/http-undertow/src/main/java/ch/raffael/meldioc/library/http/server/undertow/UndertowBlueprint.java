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

package ch.raffael.meldioc.library.http.server.undertow;

import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.library.http.server.undertow.handler.AdvisedDispatchHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.DispatchToWorkerHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.ErrorMessageHandler;
import ch.raffael.meldioc.library.http.server.undertow.routing.RoutingDefinition;
import ch.raffael.meldioc.library.http.server.undertow.routing.RoutingDefinitions;
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
public final class UndertowBlueprint {

  public static final String ADDRESS_ALL = "0.0.0.0";
  public static final String ADDRESS_LOCAL = "localhost";

  private static final Function<HttpHandler, HttpHandler> DISPATCH = DispatchToWorkerHandler::new;
  private static final Function<HttpHandler, HttpHandler> COMPRESS = n ->
      new EncodingHandler.Builder().build(java.util.Map.of()).wrap(n);
  private static final Function<HttpHandler, HttpHandler> ERROR = ErrorMessageHandler::new;

  private Seq<Consumer<? super Undertow.Builder>> listeners = List.empty();
  private
  Seq<Function<? super HttpHandler, ? extends HttpHandler>> handlerChain = List.of(DISPATCH, COMPRESS, ERROR);

  @Nullable
  private Supplier<? extends HttpHandler> mainHandler = null;
  private Seq<Consumer<? super Undertow>> postConstruct = List.empty();
  private Seq<Consumer<? super Undertow>> postStart = List.empty();
  private Option<? extends Supplier<? extends AroundAdvice>> dispatchAdvice = none();

  @Deprecated(forRemoval = true)
  public static  EP holder() {
    return holder(Undertow::builder);
  }

  public static  EP holder(Supplier<? extends Undertow.Builder> undertowBuilderSupplier) {
    return new EP(new UndertowBlueprint(), undertowBuilderSupplier);
  }

  public UndertowBlueprint httpAll(int port) {
    return http(ADDRESS_ALL, port);
  }

  public UndertowBlueprint httpLocal(int port) {
    return http(ADDRESS_LOCAL, port);
  }

  public UndertowBlueprint http(String address, int port) {
    listeners = listeners.append(b -> b.addHttpListener(port, address));
    return this;
  }

  public synchronized UndertowBlueprint httpAllSecure(int port, SSLContext sslContext) {
    return httpSecure(ADDRESS_ALL, port, sslContext);
  }

  public synchronized UndertowBlueprint httpLocalSecure(int port, SSLContext sslContext) {
    return httpSecure(ADDRESS_LOCAL, port, sslContext);
  }

  public synchronized UndertowBlueprint httpSecure(String address, int port, SSLContext sslContext) {
    listeners = listeners.append(b -> b.addHttpsListener(port, address, sslContext));
    return this;
  }

  public UndertowBlueprint handler(Function<? super HttpHandler, ? extends HttpHandler> handler) {
    handlerChain = handlerChain.append(handler);
    return this;
  }

  public UndertowBlueprint prependHandler(Function<? super HttpHandler, ? extends HttpHandler> handler) {
    handlerChain = handlerChain.prepend(handler);
    return this;
  }

  public UndertowBlueprint dispatchAdvice(Supplier<? extends AroundAdvice> dispatchAdvice) {
    return dispatchAdvice(some(dispatchAdvice));
  }

  public UndertowBlueprint dispatchAdvice(Option<? extends Supplier<? extends AroundAdvice>> dispatchAdvice) {
    this.dispatchAdvice = dispatchAdvice;
    return this;
  }

  public SecurityBuilder security(IdentityManager identityManager) {
    return new SecurityBuilder(this, identityManager);
  }

  public UndertowBlueprint security(IdentityManager identityManager, AuthenticationMechanism... mechanisms) {
    return new SecurityBuilder(this, identityManager).mechanism(mechanisms).end();
  }

  public UndertowBlueprint security(IdentityManager identityManager,
                                       Iterable<? extends AuthenticationMechanism> mechanisms) {
    return new SecurityBuilder(this, identityManager).mechanism(mechanisms).end();
  }

  public UndertowBlueprint basicSecurity(IdentityManager identityManager) {
    return new SecurityBuilder(this, identityManager).basicAuth().end();
  }

  public UndertowBlueprint basicSecurity(IdentityManager identityManager, String realm) {
    return new SecurityBuilder(this, identityManager).basicAuth(realm).end();
  }

  public UndertowBlueprint customMainHandler(Supplier<? extends HttpHandler> mainHandler) {
    this.mainHandler = mainHandler;
    return this;
  }

  public UndertowBlueprint routing(Supplier<? extends RoutingDefinition> routing) {
    return customMainHandler(() -> RoutingDefinitions.materialize(routing.get()));
  }

  public UndertowBlueprint disableCompression() {
    handlerChain = handlerChain.remove(COMPRESS);
    return this;
  }

  public UndertowBlueprint disableStandardErrorHandler() {
    handlerChain = handlerChain.remove(ERROR);
    return this;
  }

  public UndertowBlueprint disableEarlyDispatch() {
    handlerChain = handlerChain.remove(DISPATCH);
    return this;
  }

  public UndertowBlueprint enableStackTraces() {
    return enableStackTraces(true);
  }

  public UndertowBlueprint enableStackTraces(boolean enable) {
    if (enable) {
      handlerChain = handlerChain.prepend(ErrorMessageHandler.ExceptionRenderer::enableStackTracesHandler);
    }
    return this;
  }

  public UndertowBlueprint enableStackTraces(Supplier<Boolean> enable) {
    handlerChain = handlerChain.prepend(
        n -> ErrorMessageHandler.ExceptionRenderer.enableStackTracesHandler(n, enable));
    return this;
  }

  public UndertowBlueprint enableStackTraces(Predicate<? super HttpServerExchange> enable) {
    handlerChain = handlerChain.prepend(
        n -> ErrorMessageHandler.ExceptionRenderer.enableStackTracesHandler(n, enable));
    return this;
  }

  @Deprecated(forRemoval = true)
  public UndertowBlueprint suppressStackTraces() {
    return enableStackTraces(false);
  }

  @Deprecated(forRemoval = true)
  public UndertowBlueprint suppressStackTraces(boolean suppress) {
    return enableStackTraces(!suppress);
  }

  @Deprecated(forRemoval = true)
  public UndertowBlueprint suppressStackTraces(Supplier<Boolean> suppress) {
    return enableStackTraces(() -> {
      var result = suppress.get();
      return result == null ? null : !result;
    });
  }

  @Deprecated(forRemoval = true)
  public UndertowBlueprint suppressStackTraces(Predicate<? super HttpServerExchange> suppress) {
    return enableStackTraces(e -> !suppress.test(e));
  }

  public UndertowBlueprint clearHandlerChain() {
    handlerChain = List.empty();
    return this;
  }

  public UndertowBlueprint addCompressionHandler() {
    return addStandardHandler(COMPRESS);
  }

  public UndertowBlueprint addStandardErrorHandler() {
    return addStandardHandler(ERROR);
  }

  public UndertowBlueprint addDispatchHandler() {
    return addStandardHandler(DISPATCH);
  }

  private UndertowBlueprint addStandardHandler(Function<HttpHandler, HttpHandler> handler) {
    if (!handlerChain.contains(handler)) {
      handlerChain = handlerChain.append(handler);
    }
    return this;
  }

  public UndertowBlueprint postConstruct(Consumer<? super Undertow> consumer) {
    postConstruct = postConstruct.append(consumer);
    return this;
  }

  public UndertowBlueprint postStart(Consumer<? super Undertow> consumer) {
    postStart = postStart.append(consumer);
    return this;
  }

  public static final class SecurityBuilder {
    private final UndertowBlueprint parent;
    private final IdentityManager identityManager;
    private AuthenticationMode authenticationMode = AuthenticationMode.CONSTRAINT_DRIVEN;

    private Seq<AuthenticationMechanism> mechanisms = List.empty();

    private SecurityBuilder(UndertowBlueprint parent, IdentityManager identityManager) {
      this.parent = parent;
      this.identityManager = identityManager;
    }

    public SecurityBuilder constraintDriven() {
      return authenticationMode(AuthenticationMode.CONSTRAINT_DRIVEN);
    }

    public SecurityBuilder proActive() {
      return authenticationMode(AuthenticationMode.PRO_ACTIVE);
    }

    public SecurityBuilder authenticationMode(AuthenticationMode mode) {
      this.authenticationMode = mode;
      return this;
    }

    public SecurityBuilder mechanism(AuthenticationMechanism... mechanisms) {
      return mechanism(Arrays.asList(mechanisms));
    }

    public SecurityBuilder mechanism(Iterable<? extends AuthenticationMechanism> mechanisms) {
      this.mechanisms = this.mechanisms.appendAll(mechanisms);
      return this;
    }

    public SecurityBuilder basicAuth() {
      return mechanism(new BasicAuthenticationMechanism("undertow"));
    }

    public SecurityBuilder basicAuth(String realm) {
      return mechanism(new BasicAuthenticationMechanism(realm));
    }

    public UndertowBlueprint end() {
      var mechanisms = Option.of(this.mechanisms)
          .filter(l -> !l.isEmpty())
          .map(Seq::asJava)
          .getOrElseThrow(() -> new IllegalStateException("No security mechanisms"));
      return parent.handler(n -> new SecurityInitialHandler(authenticationMode, identityManager,
          new AuthenticationMechanismsHandler(n, mechanisms)));
    }
  }

  public static class EP  {
    private final UndertowBlueprint blueprint;
    private final Supplier<? extends Undertow.Builder> undertowBuilderSupplier;

    protected EP(UndertowBlueprint blueprint, Supplier<? extends Undertow.Builder> undertowBuilderSupplier) {
      this.blueprint = blueprint;
      this.undertowBuilderSupplier = undertowBuilderSupplier;
    }

    public UndertowBlueprint acceptor() {
      return blueprint;
    }

    protected Undertow apply() {
      return autoStart(blueprint, build(blueprint, prepareBuilder(blueprint)));
    }

    protected Undertow.Builder prepareBuilder(UndertowBlueprint acceptor) {
      if (acceptor.listeners.isEmpty()) {
        throw new IllegalStateException("No listeners");
      }
      if (acceptor.mainHandler == null) {
        throw new IllegalStateException("No main handler");
      }
      var builder = undertowBuilderSupplier.get();
      acceptor.listeners.forEach(l -> l.accept(builder));
      HttpHandler handler = acceptor.handlerChain.foldRight((HttpHandler) acceptor.mainHandler.get(),
          Function::apply);
      builder.setHandler(AdvisedDispatchHandler.prepend(handler, acceptor.dispatchAdvice));
      return builder;
    }

    protected Undertow build(UndertowBlueprint acceptor, Undertow.Builder builder) {
      var undertow = builder.build();
      acceptor.postConstruct.forEach(h -> h.accept(undertow));
      return undertow;
    }

    protected Undertow start(UndertowBlueprint acceptor, Undertow undertow) {
      undertow.start();
      blueprint.postStart.forEach(h -> h.accept(undertow));
      return undertow;
    }

    protected Undertow autoStart(UndertowBlueprint acceptor, Undertow undertow) {
      return start(acceptor, undertow);
    }
  }
}
