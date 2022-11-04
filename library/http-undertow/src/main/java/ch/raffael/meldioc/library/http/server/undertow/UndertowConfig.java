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
import org.jetbrains.annotations.Nullable;

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
public final class UndertowConfig {

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

  public static Handle create(Supplier<? extends Undertow.Builder> undertowBuilderSupplier) {
    return new Handle(new UndertowConfig(), undertowBuilderSupplier);
  }

  public UndertowConfig httpAll(int port) {
    return http(ADDRESS_ALL, port);
  }

  public UndertowConfig httpLocal(int port) {
    return http(ADDRESS_LOCAL, port);
  }

  public UndertowConfig http(String address, int port) {
    listeners = listeners.append(b -> b.addHttpListener(port, address));
    return this;
  }

  public synchronized UndertowConfig httpAllSecure(int port, SSLContext sslContext) {
    return httpSecure(ADDRESS_ALL, port, sslContext);
  }

  public synchronized UndertowConfig httpLocalSecure(int port, SSLContext sslContext) {
    return httpSecure(ADDRESS_LOCAL, port, sslContext);
  }

  public synchronized UndertowConfig httpSecure(String address, int port, SSLContext sslContext) {
    listeners = listeners.append(b -> b.addHttpsListener(port, address, sslContext));
    return this;
  }

  public UndertowConfig handler(Function<? super HttpHandler, ? extends HttpHandler> handler) {
    handlerChain = handlerChain.append(handler);
    return this;
  }

  public UndertowConfig prependHandler(Function<? super HttpHandler, ? extends HttpHandler> handler) {
    handlerChain = handlerChain.prepend(handler);
    return this;
  }

  public UndertowConfig dispatchAdvice(Supplier<? extends AroundAdvice> dispatchAdvice) {
    return dispatchAdvice(some(dispatchAdvice));
  }

  public UndertowConfig dispatchAdvice(Option<? extends Supplier<? extends AroundAdvice>> dispatchAdvice) {
    this.dispatchAdvice = dispatchAdvice;
    return this;
  }

  public SecurityBuilder security(IdentityManager identityManager) {
    return new SecurityBuilder(this, identityManager);
  }

  public UndertowConfig security(IdentityManager identityManager, AuthenticationMechanism... mechanisms) {
    return new SecurityBuilder(this, identityManager).mechanism(mechanisms).end();
  }

  public UndertowConfig security(IdentityManager identityManager,
                                 Iterable<? extends AuthenticationMechanism> mechanisms) {
    return new SecurityBuilder(this, identityManager).mechanism(mechanisms).end();
  }

  public UndertowConfig basicSecurity(IdentityManager identityManager) {
    return new SecurityBuilder(this, identityManager).basicAuth().end();
  }

  public UndertowConfig basicSecurity(IdentityManager identityManager, String realm) {
    return new SecurityBuilder(this, identityManager).basicAuth(realm).end();
  }

  public UndertowConfig customMainHandler(Supplier<? extends HttpHandler> mainHandler) {
    this.mainHandler = mainHandler;
    return this;
  }

  public UndertowConfig routing(Supplier<? extends RoutingDefinition> routing) {
    return customMainHandler(() -> RoutingDefinitions.materialize(routing.get()));
  }

  public UndertowConfig disableCompression() {
    handlerChain = handlerChain.remove(COMPRESS);
    return this;
  }

  public UndertowConfig disableStandardErrorHandler() {
    handlerChain = handlerChain.remove(ERROR);
    return this;
  }

  public UndertowConfig disableEarlyDispatch() {
    handlerChain = handlerChain.remove(DISPATCH);
    return this;
  }

  public UndertowConfig enableStackTraces() {
    return enableStackTraces(true);
  }

  public UndertowConfig enableStackTraces(boolean enable) {
    if (enable) {
      handlerChain = handlerChain.prepend(ErrorMessageHandler.ExceptionRenderer::enableStackTracesHandler);
    }
    return this;
  }

  public UndertowConfig enableStackTraces(Supplier<Boolean> enable) {
    handlerChain = handlerChain.prepend(
        n -> ErrorMessageHandler.ExceptionRenderer.enableStackTracesHandler(n, enable));
    return this;
  }

  public UndertowConfig enableStackTraces(Predicate<? super HttpServerExchange> enable) {
    handlerChain = handlerChain.prepend(
        n -> ErrorMessageHandler.ExceptionRenderer.enableStackTracesHandler(n, enable));
    return this;
  }

  public UndertowConfig clearHandlerChain() {
    handlerChain = List.empty();
    return this;
  }

  public UndertowConfig addCompressionHandler() {
    return addStandardHandler(COMPRESS);
  }

  public UndertowConfig addStandardErrorHandler() {
    return addStandardHandler(ERROR);
  }

  public UndertowConfig addDispatchHandler() {
    return addStandardHandler(DISPATCH);
  }

  private UndertowConfig addStandardHandler(Function<HttpHandler, HttpHandler> handler) {
    if (!handlerChain.contains(handler)) {
      handlerChain = handlerChain.append(handler);
    }
    return this;
  }

  public UndertowConfig postConstruct(Consumer<? super Undertow> consumer) {
    postConstruct = postConstruct.append(consumer);
    return this;
  }

  public UndertowConfig postStart(Consumer<? super Undertow> consumer) {
    postStart = postStart.append(consumer);
    return this;
  }

  public static final class SecurityBuilder {
    private final UndertowConfig parent;
    private final IdentityManager identityManager;
    private AuthenticationMode authenticationMode = AuthenticationMode.CONSTRAINT_DRIVEN;

    private Seq<AuthenticationMechanism> mechanisms = List.empty();

    private SecurityBuilder(UndertowConfig parent, IdentityManager identityManager) {
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

    public UndertowConfig end() {
      var mechanisms = Option.of(this.mechanisms)
          .filter(l -> !l.isEmpty())
          .map(Seq::asJava)
          .getOrElseThrow(() -> new IllegalStateException("No security mechanisms"));
      return parent.handler(n -> new SecurityInitialHandler(authenticationMode, identityManager,
          new AuthenticationMechanismsHandler(n, mechanisms)));
    }
  }

  public static class Handle {
    private final UndertowConfig config;
    private final Supplier<? extends Undertow.Builder> undertowBuilderSupplier;

    protected Handle(UndertowConfig config, Supplier<? extends Undertow.Builder> undertowBuilderSupplier) {
      this.config = config;
      this.undertowBuilderSupplier = undertowBuilderSupplier;
    }

    public UndertowConfig config() {
      return config;
    }

    public Undertow apply() {
      return start(build(prepareBuilder()));
    }

    protected Undertow.Builder prepareBuilder() {
      if (config.listeners.isEmpty()) {
        throw new IllegalStateException("No listeners");
      }
      if (config.mainHandler == null) {
        throw new IllegalStateException("No main handler");
      }
      var builder = undertowBuilderSupplier.get();
      config.listeners.forEach(l -> l.accept(builder));
      HttpHandler handler = config.handlerChain.foldRight((HttpHandler) config.mainHandler.get(),
          Function::apply);
      builder.setHandler(AdvisedDispatchHandler.prepend(handler, config.dispatchAdvice));
      return builder;
    }

    public Undertow build(Undertow.Builder builder) {
      var undertow = builder.build();
      config.postConstruct.forEach(h -> h.accept(undertow));
      return undertow;
    }

    public Undertow start(Undertow undertow) {
      undertow.start();
      config.postStart.forEach(h -> h.accept(undertow));
      return undertow;
    }
  }
}
