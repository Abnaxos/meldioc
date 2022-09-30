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

package ch.raffael.meldioc.model;

import ch.raffael.meldioc.model.messages.Message;
import ch.raffael.meldioc.model.messages.MessageSink;
import ch.raffael.meldioc.util.immutables.Immutable;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.immutables.value.Value;

import static io.vavr.control.Option.some;

/**
 * Rich representation of a method participating in the Meld model.
 */
@Immutable.Pure
@SuppressWarnings("varargs") // Bug in immutables or immutables-vavr: the builder methods are not annotated correctly
public abstract class ModelMethod<S, T> implements ModelMethod_With<S, T> {
  ModelMethod() {}
  public static <S, T> Builder<S, T> builder() {return new Builder<>();}
  public static <S, T> ModelMethod<S, T> of(SrcElement<S, T> element, ModelType<S, T> modelType) {
    return ModelMethod_Immutable.of(element, modelType);
  }

  @Value.Parameter
  public abstract SrcElement<S, T> element();

  @Value.Parameter
  public abstract ModelType<S, T> modelType();

  @Value.Lazy
  public T returnType() {
    return via().flatMap(v -> modelType().model().modelOf(v.element().type()).allMethods().toStream()
            .map(ModelMethod::element)
            .find(m -> m.methodSignature().equals(element().methodSignature()))
            .map(SrcElement::type))
        .getOrElse(element().type());
  }

  @Value.Lazy
  public Seq<T> exceptions() {
    return via().fold(
        () -> implyReason() == ImplyReason.HIERARCHY ? inheritedExceptions() : element().exceptions(),
        __ -> viaExceptions());
  }

  private Seq<T> inheritedExceptions() {
    var model = modelType().model();
    return element().exceptions()
        .filter(e ->
            model.adaptor().isSubtypeOf(e, model.runtimeExceptionType())
                || model.adaptor().isSubtypeOf(e, model.errorType())
                || overrides().forAll(o -> o.exceptions().exists(oe -> model.adaptor().isSubtypeOf(e, oe))));
  }

  private Seq<T> viaExceptions() {
    return via().flatMap(v -> modelType().model().modelOf(v.element().type()).allMethods().toStream()
            .filter(m -> m.element().methodSignature().equals(element().methodSignature()))
            .headOption())
        .map(ModelMethod::exceptions)
        .getOrElse(List.empty());
  }

  public abstract Seq<ModelMethod<S, T>> overrides();

  public abstract Option<ModelMethod<S, T>> via();
  public ModelMethod<S, T> withVia(ModelMethod<S, T> via) {
    return withVia(some(via));
  }

  public boolean implied() {
    return implyReason() != ImplyReason.NONE;
  }

  @Value.Default
  public ImplyReason implyReason() {
    return ImplyReason.NONE;
  }

  @Value.Auxiliary
  public abstract Seq<Either<ModelMethod<S, T>, BuiltinArgument>> arguments();

  @Value.Auxiliary
  public abstract Seq<Message<S, T>> messages();

  public abstract ModelMethod<S, T> withMessages(Seq<Message<S, T>> value);

  ModelMethod<S, T> addMessage(MessageSink<S, T> sink, Message<S, T> msg) {
    sink.message(msg);
    return this.withMessages(messages().append(msg));
  }

  ModelMethod<S, T> addMessages(MessageSink<S, T> sink, Iterable<? extends Message<S, T>> msg) {
    msg.forEach(sink::message);
    return this.withMessages(messages().appendAll(msg));
  }

  public enum ImplyReason {
    NONE, HIERARCHY, MOUNT, SYNTHESIZED
  }

  public static final class Builder<S, T> extends ModelMethod_Immutable.Builder<S, T> {
    Builder() {}
  }
}
