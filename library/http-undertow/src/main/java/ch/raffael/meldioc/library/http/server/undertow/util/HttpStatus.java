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

package ch.raffael.meldioc.library.http.server.undertow.util;

import io.undertow.server.HttpServerExchange;
import io.vavr.collection.Array;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import java.util.Objects;

import static io.vavr.control.Option.none;

/**
 * TODO JavaDoc
 */
public final class HttpStatus {

  private static final Map<Integer, String> DEFAULT_REASONS;

  public static final HttpStatus CONTINUE;
  public static final HttpStatus SWITCHING_PROTOCOLS;
  public static final HttpStatus PROCESSING;
  public static final HttpStatus OK;
  public static final HttpStatus CREATED;
  public static final HttpStatus ACCEPTED;
  public static final HttpStatus NON_AUTHORITATIVE_INFORMATION;
  public static final HttpStatus NO_CONTENT;
  public static final HttpStatus RESET_CONTENT;
  public static final HttpStatus PARTIAL_CONTENT;
  public static final HttpStatus MULTI_STATUS;
  public static final HttpStatus ALREADY_REPORTED;
  public static final HttpStatus IM_USED;
  public static final HttpStatus MULTIPLE_CHOICES;
  public static final HttpStatus MOVED_PERMANENTLY;
  public static final HttpStatus FOUND;
  public static final HttpStatus SEE_OTHER;
  public static final HttpStatus NOT_MODIFIED;
  public static final HttpStatus USE_PROXY;
  public static final HttpStatus TEMPORARY_REDIRECT;
  public static final HttpStatus PERMANENT_REDIRECT;
  public static final HttpStatus BAD_REQUEST;
  public static final HttpStatus UNAUTHORIZED;
  public static final HttpStatus PAYMENT_REQUIRED;
  public static final HttpStatus FORBIDDEN;
  public static final HttpStatus NOT_FOUND;
  public static final HttpStatus METHOD_NOT_ALLOWED;
  public static final HttpStatus NOT_ACCEPTABLE;
  public static final HttpStatus PROXY_AUTHENTICATION_REQUIRED;
  public static final HttpStatus REQUEST_TIME_OUT;
  public static final HttpStatus CONFLICT;
  public static final HttpStatus GONE;
  public static final HttpStatus LENGTH_REQUIRED;
  public static final HttpStatus PRECONDITION_FAILED;
  public static final HttpStatus PAYLOAD_TOO_LARGE;
  public static final HttpStatus URI_TOO_LONG;
  public static final HttpStatus UNSUPPORTED_MEDIA_TYPE;
  public static final HttpStatus REQUEST_RANGE_NOT_SATISFIABLE;
  public static final HttpStatus EXPECTATION_FAILED;
  public static final HttpStatus IM_A_TEAPOT;
  public static final HttpStatus MISDIRECTED_REQUEST;
  public static final HttpStatus UNPROCESSABLE_ENTITY;
  public static final HttpStatus LOCKED;
  public static final HttpStatus FAILED_DEPENDENCY;
  public static final HttpStatus TOO_EARLY;
  public static final HttpStatus UPGRADE_REQUIRED;
  public static final HttpStatus PRECONDITION_REQUIRED;
  public static final HttpStatus TOO_MANY_REQUESTS;
  public static final HttpStatus REQUEST_HEADER_FIELDS_TOO_LARGE;
  public static final HttpStatus UNAVAILABLE_FOR_LEGAL_REASONS;
  public static final HttpStatus INTERNAL_SERVER_ERROR;
  public static final HttpStatus NOT_IMPLEMENTED;
  public static final HttpStatus BAD_GATEWAY;
  public static final HttpStatus SERVICE_UNAVAILABLE;
  public static final HttpStatus GATEWAY_TIME_OUT;
  public static final HttpStatus HTTP_VERSION_NOT_SUPPORTED;
  public static final HttpStatus INSUFFICIENT_STORAGE;
  public static final HttpStatus LOOP_DETECTED;
  public static final HttpStatus NOT_EXTENDED;
  public static final HttpStatus NETWORK_AUTHENTICATION_REQUIRED;

  private static final String FALLBACK_REASON = "Unknown";

  private final int code;
  private final Option<String> reason;

  private HttpStatus(int code) {
    this(code, none());
  }

  private HttpStatus(int code, Option<String> reason) {
    this.code = code;
    this.reason = reason.filter(Objects::nonNull);
  }

  public static HttpStatus of(int code) {
    return new HttpStatus(code, none());
  }

  public static HttpStatus of(int code, String reason) {
    return of(code, Option.of(reason));
  }

  public static HttpStatus of(int code, Option<String> reason) {
    return new HttpStatus(code, reason.filter(Objects::nonNull));
  }

  public int code() {
    return code;
  }

  public String reason() {
    return reason.orElse(DEFAULT_REASONS.get(code())).getOrElse(FALLBACK_REASON);
  }

  public HttpStatus reason(String reason) {
    return of(code, Option.of(reason));
  }

  public HttpStatus reason(Option<String> reason) {
    return of(code, reason);
  }

  public Option<String> reasonOption() {
    return reason;
  }

  public String defaultReason() {
    return defaultReasonOption().getOrElse(FALLBACK_REASON);
  }

  public Option<String> defaultReasonOption() {
    return DEFAULT_REASONS.get(code());
  }

  public Category category() {
    return Category.of(code()).getOrElse(Category.SERVER_ERROR);
  }

  public boolean isInformational() {
    return Category.INFORMATIONAL.contains(code);
  }

  public boolean isSuccess() {
    return Category.SUCCESS.contains(code);
  }

  public boolean isOk() {
    // OK, Created, Accepted, Non-Authoritative Information, No Content
    return code >= OK.code() && code <= NO_CONTENT.code();
  }

  public boolean isRedirect() {
    return Category.REDIRECT.contains(code());
  }

  public boolean isClientError() {
    return Category.CLIENT_ERROR.contains(code());
  }

  public boolean isServerError() {
    return Category.SERVER_ERROR.contains(code()) || !isValid();
  }

  public boolean isDefined() {
    return DEFAULT_REASONS.containsKey(code());
  }

  public boolean isValid() {
    return code >= 100 && code <= 599;
  }

  public void apply(HttpServerExchange exchange) {
    exchange.setStatusCode(code());
    exchange.setReasonPhrase(reason());
  }

  @SuppressWarnings("ObjectEquality")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HttpStatus that = (HttpStatus) o;
    return code == that.code && reason.equals(that.reason);
  }

  @Override
  public int hashCode() {
    int result = code;
    result = 31 * result + reason.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return code() + " " + reason();
  }

  public enum Category {
    INFORMATIONAL(100), SUCCESS(200), REDIRECT(300), CLIENT_ERROR(400), SERVER_ERROR(500);

    private static final Seq<Category> VALUES = Array.of(values());

    private final int min;
    private final int max;

    Category(int min) {
      this(min, min + 99);
    }

    Category(int min, int max) {
      this.min = min;
      this.max = max;
    }

    public static Option<Category> of(int code) {
      return VALUES.filter(c -> c.contains(code)).headOption();
    }

    public boolean contains(int code) {
      return code >= min && code <= max;
    }
  }

  static {
    class Builder {
      Map<Integer, String> defaultReasons = LinkedHashMap.empty();

      HttpStatus add(int code, String defaultReason) {
        defaultReasons = defaultReasons.put(code, defaultReason);
        return new HttpStatus(code, none());
      }
    }
    var builder = new Builder();
    CONTINUE = builder.add(100, "Continue");
    SWITCHING_PROTOCOLS = builder.add(101, "Switching Protocols");
    PROCESSING = builder.add(102, "Processing");
    OK = builder.add(200, "OK");
    CREATED = builder.add(201, "Created");
    ACCEPTED = builder.add(202, "Accepted");
    NON_AUTHORITATIVE_INFORMATION = builder.add(203, "Non-Authoritative Information");
    NO_CONTENT = builder.add(204, "No Content");
    RESET_CONTENT = builder.add(205, "Reset Content");
    PARTIAL_CONTENT = builder.add(206, "Partial Content");
    MULTI_STATUS = builder.add(207, "Multi-Status");
    ALREADY_REPORTED = builder.add(208, "Already Reported");
    IM_USED = builder.add(226, "IM Used");
    MULTIPLE_CHOICES = builder.add(300, "Multiple Choices");
    MOVED_PERMANENTLY = builder.add(301, "Moved Permanently");
    FOUND = builder.add(302, "Found");
    SEE_OTHER = builder.add(303, "See Other");
    NOT_MODIFIED = builder.add(304, "Not Modified");
    USE_PROXY = builder.add(305, "Use Proxy");
    TEMPORARY_REDIRECT = builder.add(307, "Temporary Redirect");
    PERMANENT_REDIRECT = builder.add(308, "Permanent Redirect");
    BAD_REQUEST = builder.add(400, "Bad Request");
    UNAUTHORIZED = builder.add(401, "Unauthorized");
    PAYMENT_REQUIRED = builder.add(402, "Payment Required");
    FORBIDDEN = builder.add(403, "Forbidden");
    NOT_FOUND = builder.add(404, "Not Found");
    METHOD_NOT_ALLOWED = builder.add(405, "Method Not Allowed");
    NOT_ACCEPTABLE = builder.add(406, "Not Acceptable");
    PROXY_AUTHENTICATION_REQUIRED = builder.add(407, "Proxy Authentication Required");
    REQUEST_TIME_OUT = builder.add(408, "Request Timeout");
    CONFLICT = builder.add(409, "Conflict");
    GONE = builder.add(410, "Gone");
    LENGTH_REQUIRED = builder.add(411, "Length Required");
    PRECONDITION_FAILED = builder.add(412, "Precondition Failed");
    PAYLOAD_TOO_LARGE = builder.add(413, "Payload Too Large"); // TODO FIXME (2021-01-22) wikipedia check, contradicts undertow
    URI_TOO_LONG = builder.add(414, "URI Too Long"); // TODO FIXME (2021-01-22) wikipedia check, contradicts undertow
    UNSUPPORTED_MEDIA_TYPE = builder.add(415, "Unsupported Media Type");
    REQUEST_RANGE_NOT_SATISFIABLE = builder.add(416, "Range Not Satisfiable");
    EXPECTATION_FAILED = builder.add(417, "Expectation Failed");
    IM_A_TEAPOT = builder.add(418, "I'm a teapot"); // missing in undertow
    MISDIRECTED_REQUEST = builder.add(421, "Misdirected Request"); // TODO FIXME (2021-01-22) missing in undertow
    UNPROCESSABLE_ENTITY = builder.add(422, "Unprocessable Entity");
    LOCKED = builder.add(423, "Locked");
    FAILED_DEPENDENCY = builder.add(424, "Failed Dependency");
    TOO_EARLY = builder.add(425, "Too Early"); // TODO FIXME (2021-01-22) missing in undertow
    UPGRADE_REQUIRED = builder.add(426, "Upgrade Required");
    PRECONDITION_REQUIRED = builder.add(428, "Precondition Required");
    TOO_MANY_REQUESTS = builder.add(429, "Too Many Requests");
    REQUEST_HEADER_FIELDS_TOO_LARGE = builder.add(431, "Request Header Fields Too Large");
    UNAVAILABLE_FOR_LEGAL_REASONS = builder.add(451, "Unavailable For Legal Reasons"); // TODO FIXME (2021-01-22) missing in undertow
    INTERNAL_SERVER_ERROR = builder.add(500, "Internal Server Error");
    NOT_IMPLEMENTED = builder.add(501, "Not Implemented");
    BAD_GATEWAY = builder.add(502, "Bad Gateway");
    SERVICE_UNAVAILABLE = builder.add(503, "Service Unavailable");
    GATEWAY_TIME_OUT = builder.add(504, "Gateway Timeout");
    HTTP_VERSION_NOT_SUPPORTED = builder.add(505, "HTTP Version Not Supported");
    INSUFFICIENT_STORAGE = builder.add(507, "Insufficient Storage");
    LOOP_DETECTED = builder.add(508, "Loop Detected");
    NOT_EXTENDED = builder.add(510, "Not Extended");
    NETWORK_AUTHENTICATION_REQUIRED = builder.add(511, "Network Authentication Required");
    DEFAULT_REASONS = builder.defaultReasons;
    // TODO FIXME (2021-01-22) Wikipedia lists some unofficial codes; include them?
  }
}
