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

import ch.raffael.compose.core.Configs;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.EncodingHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ch.raffael.compose.util.Exceptions.autoClose;

/**
 * TODO JavaDoc
 */
public abstract class UndertowBuilder<C> {

  public static final String CONFIG_PORT = "port";
  public static final String CONFIG_HOST = "host";

  public static final Config DEFAULTS =
      ConfigFactory.parseURL(DefaultUndertowServerFeature.class.getResource("defaults.conf"));

  private final Config config;
  private final Supplier<? extends SSLContext> sslContext;

  private final Object initLock = new Object();
  @Nullable
  private volatile Undertow undertow = null;

  protected UndertowBuilder(Config config, Supplier<? extends SSLContext> sslContext) {
    this.config = config.withFallback(DEFAULTS);
    this.sslContext = sslContext;
  }

  protected UndertowBuilder(Config config) {
    this(config, ch.raffael.compose.core.security.ssl.SslContexts::system);
  }

  protected Undertow undertow() {
    var undertow = this.undertow;
    if (undertow == null) {
      synchronized (initLock) {
        if ((undertow = this.undertow) == null) {
          this.undertow = undertow = Objects.requireNonNull(buildUndertow(), "buildUndertow()");
        }
      }
    }
    return undertow;
  }

  @Nonnull
  private Undertow buildUndertow() {
    var builder = Undertow.builder();
    configureListeners(builder);
    var handlerChain = buildHandlerChain();
    builder.setHandler(exchange -> {
      var context = createRequestContext(exchange);
      try (var __ = autoClose(() -> destroyRequestContext(context))) {
        handlerChain.handleRequest(exchange);
      }
    });
    return builder.build();
  }

  protected abstract C createRequestContext(HttpServerExchange serverExchange);

  protected void destroyRequestContext(C context) {
  }

  protected abstract HttpHandler buildHandlerChain();

  protected void configureListeners(Builder builder) {
    configureListener("http", c ->
        builder.addHttpListener(c.getInt(CONFIG_PORT), c.getString(CONFIG_HOST)));
    configureListener("https", c ->
        builder.addHttpsListener(c.getInt(CONFIG_PORT), c.getString(CONFIG_HOST), sslContext.get()));
  }

  protected HttpHandler compressHandler(Builder builder, HttpHandler next) {
    return new EncodingHandler.Builder().build(Map.of()).wrap(next);
  }

  private void configureListener(String listenerPath, Consumer<? super Config> configurer) {
    var config = this.config;
    Configs.configure(config, Config::getConfig, listenerPath, c -> {
      if (Configs.option(c, Config::getBoolean, "enabled").getOrElse(false)) {
        configurer.accept(c.withFallback(DEFAULTS.getConfig(listenerPath)));
      }
    });
  }

  public static abstract class WithEmptyContext extends UndertowBuilder<RequestContext.Empty> {
    protected WithEmptyContext(Config config, Supplier<? extends SSLContext> sslContext) {
      super(config, sslContext);
    }

    protected WithEmptyContext(Config config) {
      super(config);
    }

    @Override
    protected RequestContext.Empty createRequestContext(HttpServerExchange serverExchange) {
      return RequestContext.empty();
    }
  }

  public static abstract class WithServerExchangeContext extends UndertowBuilder<RequestContext.ServerExchange> {
    protected WithServerExchangeContext(Config config, Supplier<? extends SSLContext> sslContext) {
      super(config, sslContext);
    }

    protected WithServerExchangeContext(Config config) {
      super(config);
    }

    @Override
    protected RequestContext.ServerExchange createRequestContext(HttpServerExchange serverExchange) {
      return RequestContext.withServerExchange(serverExchange);
    }
  }

}
