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

package ch.raffael.compose.util;

import io.vavr.collection.Seq;

import java.util.function.UnaryOperator;

import static io.vavr.API.*;

/**
 * Some string utilities for messages.
 */
public class Strings {

  private Strings() {
  }

  public static String capitalize(CharSequence input) {
    return changeFirstLetter(input, Character::toUpperCase);
  }

  public static String uncapitalize(CharSequence input) {
    return changeFirstLetter(input, Character::toLowerCase);
  }

  private static String changeFirstLetter(CharSequence input, UnaryOperator<Character> operator) {
    if (input.length() <= 0) {
      return input.toString();
    } else {
      //noinspection StringBufferReplaceableByString
      return new StringBuilder(input.length())
          .append(operator.apply(input.charAt(0)))
          .append(input.subSequence(1, input.length()))
          .toString();
    }
  }

  public static Seq<String> camelCaseWords(String string) {
    return CamelCaseWords.split(string);
  }

  private static final class CamelCaseWords {
    private CamelCaseWords() {
    }

    private static Seq<String> split(String str) {
      Seq<String> words = Seq();
      var start = skipIgnorable(0, str);
      if (pastEnd(start, str)) {
        return words;
      }
      var pos = start;
      var digitMode = false;
      var upperMode = upper(pos + 1, str);
      while (!pastEnd(pos, str)) {
        if (ignorable(pos, str)) {
          words = append(words, start, pos, str);
          pos = start = skipIgnorable(pos + 1, str);
        } else if (digit(pos, str)) {
          upperMode = false;
          digitMode = true;
          pos++;
        } else if (digitMode) {
          if (!digit(pos, str)) {
            words = append(words, start, pos, str);
            digitMode = false;
            start = pos;
          } else {
            pos++;
          }
        } else if (!upperMode && upper(pos, str)) {
          words = append(words, start, pos, str);
          start = pos++;
          upperMode = upper(pos, str);
        } else if (upperMode && lower(pos + 1, str)) {
          words = append(words, start, pos, str);
          start = pos++;
          upperMode = false;
        } else {
          pos++;
        }
      }
      return append(words, start, str.length(), str);
    }

    private static boolean upper(int pos, String str) {
      return !pastEnd(pos, str) && (Character.isUpperCase(str.charAt(pos)) || Character.isDigit(str.charAt(pos)));
    }

    private static boolean lower(int pos, String str) {
      return !pastEnd(pos, str) && (Character.isLowerCase(str.charAt(pos)) || Character.isDigit(str.charAt(pos)));
    }

    private static boolean digit(int pos, String str) {
      return !pastEnd(pos, str) && Character.isDigit(str.charAt(pos));
    }

    private static boolean ignorable(int pos, String str) {
      return !pastEnd(pos, str) && !Character.isLetterOrDigit(str.charAt(pos));
    }

    private static int skipIgnorable(int pos, String str) {
      //noinspection StatementWithEmptyBody
      for (; ignorable(pos, str); pos++);
      return pos;
    }

    private static boolean pastEnd(int pos, String str) {
      return pos >= str.length();
    }

    private static Seq<String> append(Seq<String> words, int start, int end, String str) {
      String word = str.substring(start, end);
      return word.isEmpty() ? words : words.append(word);
    }

  }

}
