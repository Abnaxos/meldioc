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

import ch.raffael.meldioc.library.http.server.undertow.codec.EmptyBody;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpDecoder;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpEncoder;
import ch.raffael.meldioc.library.http.server.undertow.handler.EndpointHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.EndpointHandler.State;
import ch.raffael.meldioc.library.http.server.undertow.util.HttpMethod;
import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatus;
import io.vavr.collection.Set;
import io.vavr.control.Option;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action0;
import static ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action0Void;
import static ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action1;
import static ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action1Void;
import static ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action2;
import static ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action2Void;
import static ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action3;
import static ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action3Void;
import static io.vavr.control.Option.some;
///<<< n: 4..count
///> "import static ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action$n;"
///> "import static ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action${n}Void;"
///>>>

///> "// vagen ${new Date()}"
/**
 * TODO JavaDoc
 */
/// filename EndpointBuilder.java
/// = `$EndpointBuilder
///   --> `EndpointBuilder
/// = `@$.Public
///   --> `public
/// = `$.$this()
///   --> `this
@$.Public class $EndpointBuilder<B, T> {
  ///<<< false
  /// -- avoid import removal by optimise imports:
  static {$.x(Action0.class, Action0Void.class);}
  ///>>>
  final DslTrace trace;
  final BiConsumer<$EndpointBuilder<?, ?>, $EndpointBuilder<?, ?>> updateCallback;
  final Set<HttpMethod> methods;
  final BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<B, T>> init;

  $EndpointBuilder(DslTrace trace,
                   BiConsumer<$EndpointBuilder<?, ?>, $EndpointBuilder<?, ?>> updateCallback,
                   Set<HttpMethod> methods,
                   BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<B, T>> init) {
    this.trace = trace;
    this.updateCallback = updateCallback;
    this.methods = methods;
    this.init = init;
  }

  $EndpointBuilder($EndpointBuilder<?, ?> prev,
                   BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<B, T>> init) {
    this(prev, prev.trace, prev.methods, init);
  }

  $EndpointBuilder($EndpointBuilder<?, ?> prev, Set<HttpMethod> methods,
                   BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<B, T>> init) {
    this(prev, prev.trace, methods, init);
  }

  $EndpointBuilder($EndpointBuilder<?, ?> prev, DslTrace trace, Set<HttpMethod> methods,
                   BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<B, T>> init) {
    this.updateCallback = prev.updateCallback;
    this.trace = trace;
    this.methods = methods;
    this.init = init;
    updateCallback.accept(prev, this);
  }

  EndpointHandler<B, T> handler(Frame frame) {
    return init.apply(frame, EndpointHandler.initial());
  }

  <BB, RR> BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<BB, RR>> addInit(
      BiFunction<Frame, EndpointHandler<B, T>, EndpointHandler<BB, RR>> init) {
    return (f, h) -> init.apply(f, this.init.apply(f, h));
  }

  <BB, RR> BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<BB, RR>> addInit(
      Function<EndpointHandler<B, T>, EndpointHandler<BB, RR>> init) {
    return (f, h) -> init.apply(this.init.apply(f, h));
  }

  $EndpointBuilder<B, T> fork(
      DslTrace trace,
      BiConsumer<$EndpointBuilder<?, ?>, $EndpointBuilder<?, ?>> updateCallback) {
    return new $EndpointBuilder<>(trace, updateCallback, methods, this.init);
  }

  public static class Method extends Decoding {
    Method(DslTrace trace, BiConsumer<$EndpointBuilder<?, ?>, $EndpointBuilder<?, ?>> updateCallback,
           Set<HttpMethod> methods) {
      super(trace, updateCallback, methods);
    }

    Method($EndpointBuilder<?, ?> prev,
           BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<EmptyBody, EmptyBody>> init) {
      super(prev, init);
    }

    Method($EndpointBuilder<?, ?> prev, Set<HttpMethod> methods,
           BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<EmptyBody, EmptyBody>> init) {
      super(prev, methods, init);
    }

    public Method method(HttpMethod... methods) {
      return new Method(this, this.methods.addAll(Arrays.asList(methods)), init);
    }

    public Method get() {
      return method(HttpMethod.GET);
    }

    public Method head() {
      return method(HttpMethod.HEAD);
    }

    public Method post() {
      return method(HttpMethod.POST);
    }

    public Method put() {
      return method(HttpMethod.PUT);
    }

    public Method patch() {
      return method(HttpMethod.PATCH);
    }

    public Method delete() {
      return method(HttpMethod.DELETE);
    }
  }

  public static class Decoding extends Processing<EmptyBody, EmptyBody> {

    Decoding(DslTrace trace, BiConsumer<$EndpointBuilder<?, ?>, $EndpointBuilder<?, ?>> updateCallback,
             Set<HttpMethod> methods) {
      super(trace, updateCallback, methods, (f, p) -> p);
    }

    Decoding($EndpointBuilder<?, ?> prev,
             BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<EmptyBody, EmptyBody>> init) {
      super(prev, init);
    }

    Decoding($EndpointBuilder<?, ?> prev, Set<HttpMethod> methods,
             BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<EmptyBody, EmptyBody>> init) {
      super(prev, methods, init);
    }

    public <T> Processing<T, T> accept(Codecs.DecoderSupplier<? extends T> decoder) {
      return new Processing<>(this, addInit((f, h) -> h.decoder(decoder.decoder(f))));
    }

    public <T> Processing<T, T> accept(HttpDecoder<? extends T> decoder) {
      return new Processing<>(this, addInit(h -> h.decoder(decoder)));
    }

    public <T> Processing<T, T> accept(Class<T> type) {
      return new Processing<>(this, addInit((f, h) -> h.decoder(f.dec.object(type))));
    }
  }

  public static class Processing<B, T> extends Response<B, T> {

    Processing(DslTrace trace, BiConsumer<$EndpointBuilder<?, ?>, $EndpointBuilder<?, ?>> updateCallback,
               Set<HttpMethod> methods, BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>,
        EndpointHandler<B, T>> init) {
      super(trace, updateCallback, methods, init);
    }

    Processing($EndpointBuilder<?, ?> prev,
               BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<B, T>> init) {
      super(prev, init);
    }

    Processing($EndpointBuilder<?, ?> prev, Set<HttpMethod> methods,
               BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<B, T>> init) {
      super(prev, methods, init);
    }

    private <U> Processing<B, U> map0(Action1<? super State<? extends T>, ? extends U> invoke) {
      return new Processing<>($.$this(), addInit(h -> h.processor(s -> {
        if (s.isException()) return s.promoteException();
        return s.value(invoke.perform(s));
      })));
    }

    private Processing<B, T> status0(Action1<? super State<? extends T>, ? extends Option<? extends HttpStatus>> invoke) {
      return new Processing<>($.$this(), addInit(h -> h.processor(s -> {
        if (s.isException()) return s.promoteException();
        return s.httpStatus(Option.narrow(invoke.perform(s)));
      })));
    }

    private <X extends Throwable> Processing<B, T> recover0(Class<X> excType, Action2<? super X, ? super State<? extends T>, ? extends T> invoke) {
      return new Processing<>($.$this(), addInit(h -> h.processor(s -> {
        if (!(s.isException() && excType.isInstance(s.exception()))) return s;
        return s.recover(invoke.perform(excType.cast(s.exception()), s));
      })));
    }

    private <X extends Throwable> Processing<B, T> exceptionStatus0(Class<X> excType, Action2<? super X, ? super State<? extends T>, ? extends Option<? extends HttpStatus>> invoke) {
      return new Processing<>($.$this(), addInit(h -> h.processor(s -> {
        if (!(s.isException() && excType.isInstance(s.exception()))) return s;
        return s.exceptionHttpStatus(Option.narrow(invoke.perform(excType.cast(s.exception()), s)));
      })));
    }

    ///-- cleanup artifacts that happen during processing (mostly due to zero repeats):
    /// ~ `,\s*,
    ///   --> `,
    /// ~ `, (\? (super |extends ))?\s*>
    ///   --> `>
    /// ~ `<(\? (super |extends ))?\s*,\s*
    ///   --> `<
    /// ~ `\(\s*,\s*
    ///   --> `(
    /// ~ `\s*,\s*\)
    ///   --> `)
    /// ~ `public\s*<>\s*
    ///   --> `public `
    ///
    ///<<</ n: 0..count
    /// <<< def & use processing methods
    ///   ~ `\$n(\W|$)
    ///     --> "$n$_1"
    ///   ~ `Capture<([^>]*)Pall> pall
    ///     --> fwd 1..n collect {"Capture<${_1}P$it> p$it"} join ', '
    ///   ~ `Action1(Void)?<([^>,]*)Pall(, [^>,]*)?>
    ///     --> "Action$n$_1<${fwd 1..n collect {"${_2}P$it"} join ', '}$_3>".rm '<>'
    ///   ~ `Action2(Void)?<([^>,]*), ([^>]*)Pall(, [^>,]*)?>
    ///     --> "Action${n+1}$_1<${_2}, ${fwd 1..n collect {"${_3}P$it"} join ', '}$_4>".rm '<>'
    ///   ~ `Action3(Void)?<([^>,]*,[^>,]*), ([^>]*)Pall(, [^>,]*)?>
    ///     --> "Action${n+2}$_1<${_2}, ${fwd 1..n collect {"${_3}P$it"} join ', '}$_4>".rm '<>'
    ///   ~ `Pall
    ///     --> fwd 1..n collect {"P$it"} join ', '
    ///   = `pall.get(s.exchange())
    ///     --> fwd 1..n collect {"p${it}.get(s.exchange())"} join ', '
    /// >>>
    ///<<< n < count
    // $n map body
    public <U, Pall> Processing<B, U> map(Capture<? extends Pall> pall, Action2<? super T, ? super Pall, ? extends U> action) {
      return map0(s ->
          action.perform(s.value(), pall.get(s.exchange())));
    }

    ///>>>
    // $n map
    public <U, Pall> Processing<B, U> map(Capture<? extends Pall> pall, Action1<? super Pall, ? extends U> action) {
      return map0(s ->
          action.perform(pall.get(s.exchange())));
    }
    ///<<< n < count

    // $n tap body
    public <Pall> Processing<B, T> tap(Capture<? extends Pall> pall, Action2Void<? super T, ? super Pall> action) {
      return map0(s -> {
        action.perform(s.value(), pall.get(s.exchange()));
        return s.value();
      });
    }
    ///>>>

    // $n tap
    public <Pall> Processing<B, T> tap(Capture<? extends Pall> pall, Action1Void<? super Pall> action) {
      return map0(s -> {
        action.perform(pall.get(s.exchange()));
        return s.value();
      });
    }
    ///<<< n < count

    // $n consume body
    public <Pall> Processing<B, EmptyBody> consume(Capture<? extends Pall> pall, Action2Void<? super T, ? super Pall> action) {
      return map0(s -> {
        action.perform(s.value(), pall.get(s.exchange()));
        return EmptyBody.instance();
      });
    }
    ///>>>

    // $n consume
    public <Pall> Processing<B, EmptyBody> consume(Capture<? extends Pall> pall, Action1Void<? super Pall> action) {
      return map0(s -> {
        action.perform(pall.get(s.exchange()));
        return EmptyBody.instance();
      });
    }
    ///<<< n == 0

    public Processing<B, T> status(HttpStatus status) {
      return status0(__ -> some(status));
    }
    ///>>>
    ///<<< n < count

    // $n status body
    public <Pall> Processing<B, T> status(Capture<? extends Pall> pall, Action2<? super T, ? super Pall, ? extends Option<? extends HttpStatus>> action) {
      return status0(s ->
            action.perform(s.value(), pall.get(s.exchange())));
    }
    ///>>>

    // $n status
    public <Pall> Processing<B, T> status(Capture<? extends Pall> pall, Action1<? super Pall, ? extends Option<? extends HttpStatus>> action) {
      return status0(s ->
            action.perform(pall.get(s.exchange())));
    }
    ///<<< n < count

    // $n recover exception
    public <X extends Throwable, Pall> Processing<B, T> recover(Class<X> excType, Capture<? extends Pall> pall, Action2<? super X, ? super Pall, ? extends T> action) {
      return recover0(excType,
          (e, s) -> action.perform(e, pall.get(s.exchange())));
    }
    ///>>>

    // $n recover
    public <X extends Throwable, Pall> Processing<B, T> recover(Class<X> excType, Capture<? extends Pall> pall, Action1<? super Pall, ? extends T> action) {
      return recover0(excType,
          (e, s) -> action.perform(pall.get(s.exchange())));
    }
    ///<<< n == 0

    public <X extends Throwable> Processing<B, T> exceptionStatus(Class<X> excType, HttpStatus status) {
      return exceptionStatus0(excType, (e, s) -> some(status));
    }
    ///>>>
    ///<<< n < count

    // $n exceptionStatus exception
    public <X extends Throwable, Pall> Processing<B, T> exceptionStatus(Class<X> excType, Capture<? extends Pall> pall, Action2<? super X, ? super Pall, ? extends Option<? extends HttpStatus>> action) {
      return exceptionStatus0(excType, (e, s) ->
            action.perform(e, pall.get(s.exchange())));
    }
    ///>>>

    // $n exceptionStatus
    public <X extends Throwable, Pall> Processing<B, T> exceptionStatus(Class<X> excType, Capture<? extends Pall> pall, Action1<? super Pall, ? extends Option<? extends HttpStatus>> action) {
      return exceptionStatus0(excType, (e, s) ->
            action.perform(pall.get(s.exchange())));
    }
    ///<<< n < count

    // $n pipe body
    public <V, Pall> Pipe<V> pipe(Capture<? extends Pall> pall, Action2<? super T, ? super Pall, ? extends V> action) {
      return new Pipe<>(s ->
          action.perform(s.value(), pall.get(s.exchange())));
    }
    ///>>>

    // $n pipe
    public <V, Pall> Pipe<V> pipe(Capture<? extends Pall> pall, Action1<? super Pall, ? extends V> action) {
      return new Pipe<>(s ->
          action.perform(pall.get(s.exchange())));
    }
    ///>>>

    public final class Pipe<V> {
      private final Action1<State<? extends T>, ? extends V> pipeFun;

      Pipe(Action1<State<? extends T>, ? extends V> pipeFun) {
        this.pipeFun = pipeFun;
      }

      ///<<</ n: 0..(count-1)
      /// use processing methods
      ///
      ///<<< n < count-1
      // $n pipe map body
      public <U, Pall> Processing<B, U> map(Capture<? extends Pall> pall, Action3<? super V, ? super T, ? super Pall, ? extends U> action) {
        return map0(s ->
            action.perform(pipeFun.perform(s), s.value(), pall.get(s.exchange())));
      }
      ///>>>

      // $n map
      public <U, Pall> Processing<B, U> map(Capture<? extends Pall> pall, Action2<? super V, ? super Pall, ? extends U> action) {
        return map0(s ->
            action.perform(pipeFun.perform(s), pall.get(s.exchange())));
      }
      ///<<< n < count-1

      // $n pipe tap body
      public <Pall> Processing<B, T> tap(Capture<? extends Pall> pall, Action3Void<? super V, ? super T, ? super Pall> action) {
        return map0(s -> {
          action.perform(pipeFun.perform(s), s.value(), pall.get(s.exchange()));
          return s.value();
        });
      }
      ///>>>

      // $n pipe tap
      public <Pall> Processing<B, T> tap(Capture<? extends Pall> pall, Action2Void<? super V, ? super Pall> action) {
        return map0(s -> {
          action.perform(pipeFun.perform(s), pall.get(s.exchange()));
          return s.value();
        });
      }
      ///<<< n < count-1

      // $n pipe consume body
      public <Pall> Processing<B, EmptyBody> consume(Capture<? extends Pall> pall, Action3Void<? super V, ? super T, ? super Pall> action) {
        return map0(s -> {
          action.perform(pipeFun.perform(s), s.value(), pall.get(s.exchange()));
          return EmptyBody.instance();
        });
      }
      ///>>>

      // $n pipe consume
      public <Pall> Processing<B, EmptyBody> consume(Capture<? extends Pall> pall, Action2Void<? super V, ? super Pall> action) {
        return map0(s -> {
          action.perform(pipeFun.perform(s), pall.get(s.exchange()));
          return EmptyBody.instance();
        });
      }
      ///<<< n < count-1

      // $n pipe pipe body
      public <U, Pall> Pipe<U> pipe(Capture<? extends Pall> pall, Action3<? super V, ? super T, ? super Pall, ? extends U> action) {
        return new Pipe<>(s ->
            action.perform(pipeFun.perform(s), s.value(), pall.get(s.exchange())));
      }
      ///>>>

      // $n pipe pipe
      public <U, Pall> Pipe<U> pipe(Capture<? extends Pall> pall, Action2<? super V, ? super Pall, ? extends U> action) {
        return new Pipe<>(s ->
            action.perform(pipeFun.perform(s), pall.get(s.exchange())));
      }
      ///>>>
    }
  }

  public static class Response<B, T> extends $EndpointBuilder<B, T> {

    Response(DslTrace trace, BiConsumer<$EndpointBuilder<?, ?>, $EndpointBuilder<?, ?>> updateCallback,
             Set<HttpMethod> methods, BiFunction<Frame,
        EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<B, T>> init) {
      super(trace, updateCallback, methods, init);
    }

    Response($EndpointBuilder<?, ?> prev,
             BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<B, T>> init) {
      super(prev, init);
    }

    Response($EndpointBuilder<?, ?> prev, Set<HttpMethod> methods,
             BiFunction<Frame, EndpointHandler<EmptyBody, EmptyBody>, EndpointHandler<B, T>> init) {
      super(prev, methods, init);
    }

    public $EndpointBuilder<B, T> respond() {
      return respond(HttpStatus.OK);
    }

    public $EndpointBuilder<B, T> respond(HttpStatus status) {
      return respond(status, Object.class);
    }

    public $EndpointBuilder<B, T> respond(Class<? super T> type) {
      return respond(HttpStatus.OK, type);
    }

    public $EndpointBuilder<B, T> respond(HttpStatus status, Class<? super T> type) {
      return new $EndpointBuilder<>(this, addInit((f, h) -> h.defaultStatus(status).encoder(f.enc.object(type))));
    }

    public $EndpointBuilder<B, T> respond(Codecs.EncoderSupplier<? super T> encoder) {
      return respond(HttpStatus.OK, encoder);
    }

    public $EndpointBuilder<B, T> respond(HttpStatus status, Codecs.EncoderSupplier<? super T> encoder) {
      return new $EndpointBuilder<>(this, addInit((f, h) -> h.defaultStatus(status).encoder(encoder.encoder(f))));
    }

    public $EndpointBuilder<B, T> respond(HttpEncoder<? super T> encoder) {
      return respond(HttpStatus.OK, encoder);
    }

    public $EndpointBuilder<B, T> respond(HttpStatus status, HttpEncoder<? super T> encoder) {
      return new $EndpointBuilder<>(this, addInit(h -> h.defaultStatus(status).encoder(encoder)));
    }

    public $EndpointBuilder<B, EmptyBody> respondEmpty() {
      return respondEmpty(HttpStatus.OK);
    }

    public $EndpointBuilder<B, EmptyBody> respondEmpty(HttpStatus status) {
      return new $EndpointBuilder<>(this,
          this.addInit(h ->
              h.processor(s -> s.map(__ -> EmptyBody.instance()))
                  .defaultStatus(status)
                  .encoder(EmptyBody.encoder())));
    }
  }
}
