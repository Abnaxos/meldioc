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

package ch.raffael.meldioc.processor;

import ch.raffael.meldioc.model.messages.Message;
import io.vavr.Tuple2;
import io.vavr.control.Option;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vavr.API.*;

public final class Diagnostics {

  private Diagnostics() {
  }

  public static final String MESSAGE_ID_PREFIX = "#MID:";

  private static final Pattern MSG_ID_RE =
      Pattern.compile("(" + Pattern.quote(MESSAGE_ID_PREFIX) + "(\\p{Alnum}+)\\s+)(.*)",
          Pattern.MULTILINE + Pattern.DOTALL);

  public static Tuple2<Option<Message.Id>, String> extractMessageId(String message) {
    Matcher m = MSG_ID_RE.matcher(message);
    if (m.matches()) {
      Message.Id id;
      try {
        id = Message.Id.valueOf(m.group(2));
      } catch (IllegalArgumentException e) {
        return Tuple(None(), message);
      }
      return Tuple(Some(id), m.group(3));
    } else {
      return Tuple(None(), message);
    }
  }

  public static StringBuilder appendMessageId(StringBuilder target, Message<?, ?> msg) {
    msg.id().forEach(id -> target.append(Diagnostics.MESSAGE_ID_PREFIX).append(id).append(' '));
    return target;
  }

}
