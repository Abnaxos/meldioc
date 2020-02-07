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

package ch.raffael.compose.base.threading;

import io.vavr.Function1;
import io.vavr.control.Option;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static io.vavr.API.*;

public class CountingThreadFactory implements ThreadFactory {

  private final Option<ThreadGroup> group;
  private final Function1<Integer, String> nameBuilder;
  private final AtomicInteger counter = new AtomicInteger();

  public CountingThreadFactory(Function1<Integer, String> nameBuilder) {
    this(None(), nameBuilder);
  }

  public CountingThreadFactory(ThreadGroup group, Function1<Integer, String> nameBuilder) {
    this(Some(group), nameBuilder);
  }

  public CountingThreadFactory(String nameFormat) {
    this(None(), formatNameBuilder(nameFormat));
  }

  public CountingThreadFactory(ThreadGroup group, String nameFormat) {
    this(Some(group), formatNameBuilder(nameFormat));
  }

  public CountingThreadFactory(Option<ThreadGroup> group, Function1<Integer, String> nameBuilder) {
    this.group = group;
    this.nameBuilder = nameBuilder;
  }

  @Override
  public Thread newThread(Runnable runnable) {
    return new Thread(group.getOrNull(), runnable, nameBuilder.apply(counter.getAndIncrement()));
  }

  public static Function1<Integer, String> formatNameBuilder(String format) {
    return (i) -> String.format(format, i);
  }

}
