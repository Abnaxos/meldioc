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

package ch.raffael.compose.library.http.server.undertow.handler;

import ch.raffael.compose.library.codec.ContentType;
import ch.raffael.compose.library.codec.ContentTypes;
import ch.raffael.compose.util.Exceptions;
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
import io.vavr.collection.Stream;
import io.vavr.control.Option;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.vavr.API.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ErrorMessageHandler implements HttpHandler {

  private static final AttachmentKey<AttachmentList<Object>> ERROR_MESSAGES_KEY =
      AttachmentKey.createList(Object.class);
  private static final AttachmentKey<AttachmentList<MessageRenderer>> MESSAGE_RENDERER_KEY =
      AttachmentKey.createList(MessageRenderer.class);
  private static final Charset CHARSET = UTF_8;
  private static final String NON_ESCAPED_CONTROLS = "\n\r \t";

  private final HttpHandler next;

  private final Tuple2<ContentType, Function3<Integer, String, Seq<String>, String>> standardRenderer =
      Tuple(ContentTypes.PLAIN_TEXT.withDefaultCharset(CHARSET), this::renderText);
  private final Map<ContentType, Function3<Integer, String, Seq<String>, String>> renderers = Map(
      ContentTypes.JSON, this::renderJson,
      ContentTypes.XML, this::renderXml,
      ContentTypes.PLAIN_TEXT, this::renderText);

  public ErrorMessageHandler(HttpHandler next) {
    this.next = next;
  }

  public static void addMessage(HttpServerExchange exchange, Object message) {
    exchange.addToAttachmentList(ERROR_MESSAGES_KEY, message);
  }

  public static void addMessageRenderer(HttpServerExchange exchange, MessageRenderer renderer) {
    exchange.addToAttachmentList(MESSAGE_RENDERER_KEY, renderer);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    exchange.addToAttachmentList(MESSAGE_RENDERER_KEY, exceptionRenderer());
    exchange.addDefaultResponseListener(this::handleDefaultResponse);
    next.handleRequest(exchange);
  }

  protected MessageRenderer exceptionRenderer() {
    return ExceptionRenderer.DEFAULT_INSTANCE;
  }

  protected boolean handleDefaultResponse(HttpServerExchange exchange) {
    if (!exchange.isResponseChannelAvailable()) {
      return false;
    }
    if (isError(exchange.getStatusCode())) {
      String reason = StatusCodes.getReason(exchange.getStatusCode());
      var messages = Stream.ofAll(exchange.getAttachmentList(ERROR_MESSAGES_KEY))
          .map(m -> renderMessage(exchange, m));
      Option<Seq<ContentType>> step = Option(exchange.getRequestHeaders().getFirst(Headers.ACCEPT))
          .filter(s -> !s.isBlank())
          .map(ContentTypes::parseContentTypeListQ);
      var errorPage = step
          .<Tuple2<ContentType, Function3<Integer, String, Seq<String>, String>>>flatMap(
              cts -> cts.<Option<Tuple2<ContentType, Function3<Integer, String, Seq<String>, String>>>>foldLeft(None(),
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

  protected String renderMessage(HttpServerExchange exchange, Object message) {
    return exchange.getAttachmentList(MESSAGE_RENDERER_KEY).stream()
        .map(r -> r.render(exchange, message))
        .flatMap(Option::toJavaStream)
        .findFirst()
        .orElseGet(() -> String.valueOf(message));
  }

  protected String renderJson(int code, String reason, Seq<String> messages) {
    StringBuilder buf = new StringBuilder();
    buf.append("{");
    appendJsonQuoted(buf, "statusCode").append(':').append(code).append(",\n");
    appendJsonQuoted(buf, "reason").append(':');
    appendJsonQuoted(buf, reason).append(",\n");
    appendJsonQuoted(buf, "messages").append(": [");
    boolean first = true;
    for (var m : messages) {
      buf.append("\n ");
      appendJsonQuoted(buf, m);
      if (first) {
        first = false;
      } else {
        buf.append(',');
      }
    }
    buf.append("]}");
    return buf.toString();
  }

  protected String renderXml(int code, String reason, Seq<String> messages) {
    StringBuilder buf = new StringBuilder();
    buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    buf.append("<error>\n");
    buf.append(" <statusCode>").append(code).append("</statusCode>\n");
    appendXmlEscaped(buf.append(" <reason>"), reason).append("</reason>\n");
    if (messages.isEmpty()) {
      buf.append(" <messages/>\n");
    } else {
      buf.append(" <messages>\n");
      for (var m : messages) {
        appendXmlEscaped(buf.append("  <message>"), m).append("</message>\n");
      }
      buf.append(" </messages>");
    }
    buf.append("\n</error>\n");
    return buf.toString();
  }

  protected String renderText(int code, String reason, Seq<String> messages) {
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
          if (Character.isISOControl(c) && NON_ESCAPED_CONTROLS.indexOf(c) < 0) {
            unicodeHex4(buf.append("&#x"), c).append(';');
          } else {
            buf.append(c);
          }
      }
    }
    return buf;
  }

  protected static StringBuilder unicodeHex4(StringBuilder buf, char c) {
    String s = Integer.toString(c, 16);
    buf.append("0".repeat(Math.max(0, 4 - s.length())));
    return buf.append(s);
  }

  @FunctionalInterface
  public interface MessageRenderer {
    Option<String> render(HttpServerExchange exchange, Object message);

    static <T> MessageRenderer forType(Class<T> type, Function<? super T, String> fun) {
      return forType(type, (__, m) -> fun.apply(m));
    }

    static <T> MessageRenderer forType(Class<T> type, BiFunction<? super HttpServerExchange, ? super T, String> fun) {
      return (e, m) -> type.isInstance(m) ? None() : Some(fun.apply(e, type.cast(m)));
    }

    static MessageRenderer stringValue() {
      return (__, m) -> Some(String.valueOf(m));
    }

    static MessageRenderer stringValue(Class<?> type) {
      return forType(type, String::valueOf);
    }
  }

  public static class ExceptionRenderer implements MessageRenderer {

    private static final ExceptionRenderer DEFAULT_INSTANCE = new ExceptionRenderer();

    private static final AttachmentKey<Boolean> SUPPRESS_STACK_TRACES = AttachmentKey.create(Boolean.class);

    public static ExceptionRenderer defaultInstance() {
      return DEFAULT_INSTANCE;
    }

    public static HttpServerExchange setSuppressStackTraces(HttpServerExchange exchange, boolean suppress) {
      exchange.putAttachment(SUPPRESS_STACK_TRACES, suppress);
      return exchange;
    }

    public static HttpServerExchange setSuppressStackTraces(HttpServerExchange exchange,
                                                            Predicate<? super HttpServerExchange> suppress) {
      exchange.putAttachment(SUPPRESS_STACK_TRACES, suppress.test(exchange));
      return exchange;
    }

    public static HttpHandler suppressStackTracesHandler(HttpHandler next) {
      return e -> next.handleRequest(setSuppressStackTraces(e, true));
    }

    public static HttpHandler suppressStackTracesHandler(HttpHandler next,
                                                         Predicate<? super HttpServerExchange> suppress) {
      return e -> next.handleRequest(setSuppressStackTraces(e, suppress.test(e)));
    }

    public static HttpHandler suppressStackTracesHandler(HttpHandler next,
                                                         Supplier<Boolean> suppress) {
      return e -> next.handleRequest(setSuppressStackTraces(e, Objects.requireNonNullElse(suppress.get(), false)));
    }

    @Override
    public Option<String> render(HttpServerExchange exchange, Object message) {
      return !(message instanceof Throwable)
             ? None()
             : Some(Exceptions.toString((Throwable) message,
                 Option(exchange.getAttachment(SUPPRESS_STACK_TRACES)).map(s -> !s).getOrElse(true)));
    }
  }
}
