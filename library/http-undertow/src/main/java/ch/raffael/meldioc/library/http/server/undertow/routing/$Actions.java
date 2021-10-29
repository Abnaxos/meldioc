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

package ch.raffael.meldioc.library.http.server.undertow.routing;

///> ! "// dslgen ${new Date()}"
/// filename Actions.java
// normalize spaces
///
/// = $Actions
///   --> Actions
/// ===
/// ~ <,\s+
///   --> <
/**
 * TODO JavaDoc
 */
///> @SuppressWarnings("RedundantSuppression")
public final class $Actions {

  private $Actions() {
  }

  ///<<</ n: 0..count
  ///
  /// ~ (Pn )?argN
  ///   --> ! fwd 1..n collect {"${_1.emptyOr "P$it "}arg$it"} join ', '
  /// ~ (\? super )?Pn
  ///   --> ! fwd 1..n collect {"${_1}P$it"} join ', '
  /// ===
  /// = ActionN
  ///   --> ! "Action$n"
  /// ===
  /// = <>
  ///   -->
  /// ===
  /// = (Action0Void)
  ///   -->
  ///
  ///> ! "// Action$n\n"
  @SuppressWarnings({"unchecked", "overloads"})
  public static <Pn, R> ActionN<Pn, R> of(ActionN<? super Pn, ? extends R> action) {
    return (ActionN<Pn, R>) action;
  }

  @SuppressWarnings({"unchecked", "overloads"})
  public static <Pn> ActionNVoid<Pn> of(ActionNVoid<? super Pn> action) {
    return (ActionNVoid<Pn>) action;
  }

  public static <Pn, R> ActionN<Pn, R> of(R returnValue, ActionNVoid<? super Pn> action) {
    return (Pn argN) -> {
      action.perform(argN);
      return returnValue;
    };
  }

  @SuppressWarnings({"overloads"})
  public static <Pn, R> ActionN<Pn, R> action(ActionN<? super Pn, ? extends R> action) {
    return of(action);
  }

  @SuppressWarnings({"overloads"})
  public static <Pn> ActionNVoid<Pn> action(ActionNVoid<? super Pn> action) {
    return of(action);
  }

  public static <Pn, R> ActionN<Pn, R> action(R returnValue, ActionNVoid<? super Pn> action) {
    return of(returnValue, action);
  }
  ///>>>

  // Action interfaces

  ///<<</ n: 0..count
  ///
  /// = ActionN
  ///   --> ! "Action$n"
  /// ===
  /// = Pn argN
  ///   --> ! fwd 1..n collect {"P$it arg$it"} join ', '
  /// = Pn
  ///   --> ! fwd 1..n collect {"P$it"} join ', '
  /// ===
  /// = <>
  ///   -->
  @FunctionalInterface
  public interface ActionN<Pn, R> {
    R perform(Pn argN) throws Exception;
  }

  @FunctionalInterface
  public interface ActionNVoid<Pn> {
    void perform(Pn argN) throws Exception;
  }
  ///>>>
}
