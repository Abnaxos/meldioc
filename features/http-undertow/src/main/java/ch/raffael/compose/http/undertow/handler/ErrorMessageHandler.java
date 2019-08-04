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

package ch.raffael.compose.http.undertow.handler;

import ch.raffael.compose.codec.ContentType;
import ch.raffael.compose.codec.ContentTypes;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.AttachmentList;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import io.vavr.Function3;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import java.nio.charset.Charset;
import java.util.List;

import static io.vavr.API.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ErrorMessageHandler implements HttpHandler {

  private static final AttachmentKey<AttachmentList<Object>> ERROR_MESSAGES_KEY = AttachmentKey.createList(Object.class);
  private static final Charset CHARSET = UTF_8;

  private final HttpHandler next;

  private final Tuple2<ContentType, Function3<Integer, String, List<Object>, String>> standardRenderer =
      Tuple(ContentTypes.PLAIN_TEXT.withDefaultCharset(CHARSET), this::renderText);
  private final Map<ContentType, Function3<Integer, String, List<Object>, String>> renderers = Map(
      ContentTypes.JSON, this::renderJson,
      ContentTypes.XML, this::renderXml,
      ContentTypes.PLAIN_TEXT, this::renderText);

  public ErrorMessageHandler(HttpHandler next) {
    this.next = next;
  }

  public static void addMessage(HttpServerExchange exchange, Object message) {
    exchange.addToAttachmentList(ERROR_MESSAGES_KEY, message);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    exchange.addDefaultResponseListener(this::handleDefaultResponse);
    next.handleRequest(exchange);
  }

  protected boolean handleDefaultResponse(HttpServerExchange exchange) {
    if (!exchange.isResponseChannelAvailable()) {
      return false;
    }
    if (isError(exchange.getStatusCode())) {
      String reason = StatusCodes.getReason(exchange.getStatusCode());
      var messages = exchange.getAttachmentList(ERROR_MESSAGES_KEY);
      Option<Seq<ContentType>> step = Option(exchange.getRequestHeaders().getFirst(Headers.ACCEPT))
          .filter(s -> !s.isBlank())
          .map(ContentTypes::parseContentTypeListQ);
      var errorPage = step
          .<Tuple2<ContentType, Function3<Integer, String, List<Object>, String>>>flatMap(
              cts -> cts.<Option<Tuple2<ContentType, Function3<Integer, String, List<Object>, String>>>>foldLeft(None(),
                  (cur, ct) -> cur.orElse(() -> renderers.get(ct.withoutAttributes()).map(r -> Tuple(ct, r)))))
          .map(t -> t.map1(ct -> ct.withDefaultCharset(CHARSET)))
          .getOrElse(standardRenderer)
          .map2(r -> r.apply(exchange.getStatusCode(), reason, messages))
          .map1(ct -> ContentTypes.isUnicodeType(ct) ? ct.withDefaultCharset(UTF_8) : ct);
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, errorPage._1.render());
      exchange.getResponseSender().send(errorPage._2);
      return true;
    } else {
      return false;
    }
  }

  protected boolean isError(int statusCode) {
    return statusCode >= 400;
  }

  protected String renderJson(int code, String reason, List<Object> messages) {
    StringBuilder buf = new StringBuilder();
    buf.append("{");
    appendJsonQuoted(buf, "statusCode").append(':').append(code).append(",\n");
    appendJsonQuoted(buf, "reason").append(':');
    appendJsonQuoted(buf, reason).append(",\n");
    appendJsonQuoted(buf, "messages").append(": [");
    boolean first = true;
    for (var m : messages) {
      buf.append("\n ");
      appendJsonQuoted(buf, m.toString());
      if (first) {
        first = false;
      } else {
        buf.append(',');
      }
    }
    buf.append("]}");
    return buf.toString();
  }

  protected String renderXml(int code, String reason, List<Object> messages) {
    StringBuilder buf = new StringBuilder();
    buf.append("<?xml version=\"1.0\" charset=\"UTF-8\">\n");
    buf.append("<error>\n");
    buf.append(" <statusCode>").append(code).append("</statusCode>\n");
    appendXmlEscaped(buf.append(" <reason>"), reason).append("</reason>\n");
    if (messages.isEmpty()) {
      buf.append(" <messages/>\n");
    } else {
      buf.append(" <messages>\n");
      for (var m : messages) {
        appendXmlEscaped(buf.append("  <message>"), m.toString()).append("</message>\n");
      }
      buf.append(" </messages>");
    }
    buf.append("</error>\n");
    return buf.toString();
  }

  protected String renderText(int code, String reason, List<Object> messages) {
    StringBuilder buf = new StringBuilder();
    buf.append(code).append(' ').append(reason).append('\n');
    messages.forEach(m -> buf.append("- ").append(m).append('\n'));
    return buf.toString();
  }

  protected static StringBuilder appendJsonQuoted(StringBuilder buf, String str) {
    buf.append('"');
    for (int i = 0; i < str.length(); i++) {
      final char c = str.charAt(i);
      switch (c) {
        case '\b':
          buf.append("\\b");
          break;
        case '\t':
          buf.append("\\t");
          break;
        case '\f':
          buf.append("\\f");
          break;
        case '\n':
          buf.append("\\n");
          break;
        case '\r':
          buf.append("\\r");
          break;
        case '"':
          buf.append("\\\"");
          break;
        case '\\':
          buf.append("\\\\");
          break;
        default:
          if (Character.isISOControl(c)) {
            unicodeHex4(buf.append("\\u"), c);
          } else {
            buf.append(c);
          }
      }
    }
    buf.append('"');
    return buf;
  }

  protected static StringBuilder appendXmlEscaped(StringBuilder buf, String str) {
    for (int i = 0; i < str.length(); i++) {
      final char c = str.charAt(i);
      switch (c) {
        case '<':
          buf.append("&lt;");
          break;
        case '>':
          buf.append("&gt;");
          break;
        case '"':
          buf.append("&quot;");
          break;
        default:
          if (Character.isISOControl(c)) {
            unicodeHex4(buf.append("&#"), c).append(';');
          } else {
            buf.append(c);
          }
      }
    }
    return buf;
  }

  protected static StringBuilder unicodeHex4(StringBuilder buf, char c) {
    String s = Integer.toString(c, 16);
    buf.append("0".repeat(Math.max(0, 4 - s.length() + 1)));
    return buf.append(s);
  }
}
