/*
 *  Copyright (c) 2020 Raffael Herzog
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

package ch.raffael.meldioc.util;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.function.BiFunction;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Utilties for dealing with I/O streams.
 */
public class IOStreams {

  public static final int DEFAULT_BUFFER_SIZE = 8192;

  private IOStreams() {
  }

  public static ByteArrayOutputStream readFully(InputStream stream, ByteArrayOutputStream target, byte[] readBuffer) throws IOException {
    int c;
    while ((c = stream.read(readBuffer)) >= 0) {
      target.write(readBuffer, 0, c);
    }
    return target;
  }

  public static byte[] readFully(InputStream stream, int bufferSize) throws IOException {
    return readFully(stream, new ByteArrayOutputStream(), new byte[bufferSize]).toByteArray();
  }

  public static byte[] readFully(InputStream stream) throws IOException {
    return readFully(stream, DEFAULT_BUFFER_SIZE);
  }

  public static <R> Tuple2<Option<R>, InputStream> probe(InputStream stream, byte[] buffer, int probeSize,
                                                         BiFunction<? super byte[], Integer, ? extends R> probeFun)
      throws IOException {
    if (probeSize < buffer.length) {
      throw new IllegalArgumentException("Probe size " + probeSize + " is less than buffer size" + buffer.length);
    }
    int read = 0;
    int c;
    Option<R> probeResult = none();
    while ((c = stream.read(buffer, read, buffer.length - read)) >= 0) {
      read += c;
      if (read >= probeSize) {
        probeResult = some(probeFun.apply(buffer, read));
      }
    }
    byte[] head = Arrays.copyOf(buffer, read);
    return Tuple.of(probeResult, new SequenceInputStream(new ByteArrayInputStream(head), stream));
  }

  public static <R> Tuple2<Option<R>, InputStream> probe(InputStream stream, int probeSize,
                                                         BiFunction<? super byte[], Integer, ? extends R> probeFun)
      throws IOException {
    return probe(stream, new byte[DEFAULT_BUFFER_SIZE], probeSize, probeFun);
  }

  public static <R> Tuple2<Option<R>, InputStream> probe(InputStream stream, int bufferSize, int probeSize,
                                                         BiFunction<? super byte[], Integer, ? extends R> probeFun)
      throws IOException {
    return probe(stream, new byte[bufferSize], probeSize, probeFun);
  }

  public static <R> Tuple2<Option<R>, InputStream> probe(InputStream stream, Option<R> declared, int probeSize,
                                                         BiFunction<? super byte[], Integer, ? extends R> probeFun)
      throws IOException {
    return probe(stream, declared, DEFAULT_BUFFER_SIZE, probeSize, probeFun);
  }

  public static <R> Tuple2<Option<R>, InputStream> probe(InputStream stream, Option<R> declared,
                                                         int bufferSize, int probeSize,
                                                         BiFunction<? super byte[], Integer, ? extends R> probeFun)
      throws IOException {
    return probe(stream, declared, new byte[bufferSize], probeSize, probeFun);
  }

  public static <R> Tuple2<Option<R>, InputStream> probe(InputStream stream, Option<R> declared,
                                                         byte[] buffer, int probeSize,
                                                         BiFunction<? super byte[], Integer, ? extends R> probeFun)
      throws IOException {
    if (declared.isDefined()) {
      return Tuple.of(declared, stream);
    } else {
      return probe(stream, buffer, probeSize, probeFun);
    }
  }
}
