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

package ch.raffael.meldioc.library.http.server.undertow.testlib;

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.Setup;
import ch.raffael.meldioc.library.base.lifecycle.Lifecycle;
import ch.raffael.meldioc.library.base.lifecycle.ShutdownFeature;
import ch.raffael.meldioc.library.base.lifecycle.StartupActions;
import ch.raffael.meldioc.library.base.threading.ThreadingFeature;
import ch.raffael.meldioc.library.http.server.undertow.UndertowBlueprint;
import ch.raffael.meldioc.library.http.server.undertow.UndertowServerFeature;
import ch.raffael.meldioc.library.http.server.undertow.util.RequestContexts;
import io.undertow.Undertow;

@Configuration
abstract class UndertowTestServerContext extends UndertowServerFeature.WithSharedWorkersAndShutdown<RequestContexts.Empty>
    implements ShutdownFeature, ThreadingFeature {

  @Feature.Mount
  abstract Lifecycle.Feature lifecylce();

  @Feature.Mount(injected = true)
  abstract RoutingFeature routingFeature();

  @Provision(singleton = true)
  @Override
  protected Undertow undertowServer() {
    return super.undertowServer();
  }

  @Setup
  void setup(StartupActions startup, UndertowBlueprint<RequestContexts.Empty> undertow) {
    undertow
        .http("localhost", 0)
        .requestContextFactory(RequestContexts::empty)
        .routing(() ->  routingFeature().routing());
    startup.add(this::start);
  }
}
