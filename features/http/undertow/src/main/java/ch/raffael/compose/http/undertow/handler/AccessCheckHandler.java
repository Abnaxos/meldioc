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

import ch.raffael.compose.http.undertow.Role;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import io.vavr.collection.Set;
import io.vavr.control.Option;

import java.util.Objects;
import java.util.function.Function;

import static io.vavr.API.*;
import static java.util.function.Function.identity;

/**
 * A handler that checks if the current user has access to the following
 * handlers. If the user does not have access, 403 Forbidden is returned.
 */
public class AccessCheckHandler implements HttpHandler {

  private final AccessRestriction restriction;
  private final HttpHandler next;

  public AccessCheckHandler(AccessRestriction restriction, HttpHandler next) {
    this.restriction = restriction;
    this.next = next;
  }

  public static <R extends Role> AccessByRole<R> accessByRole(
      Function<? super String, ? extends Option<? extends R>> mapper,
      Set<? extends R> roles) {
    return new AccessByRole<>(mapper, roles);
  }

  public static <R extends Enum & Role> AccessByRoleEnum<R> accessByRole(Class<R> enumType, Set<? extends R> roles) {
    return new AccessByRoleEnum<>(enumType, roles);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    var securityContext = Objects.requireNonNull(exchange.getSecurityContext(), "exchange.getSecurityContext()");
    var forbidden = true;
    var account = securityContext.getAuthenticatedAccount();
    if (account != null) {
      forbidden = !restriction.accessPermitted(account);
    }
    if (forbidden) {
      exchange.setStatusCode(StatusCodes.FORBIDDEN);
      exchange.endExchange();
    } else {
      next.handleRequest(exchange);
    }
  }

  private static <T extends Enum & Role> Function<String, Option<T>> enumMapper(Class<T> roleEnum) {
    return Array(roleEnum.getEnumConstants()).toMap(Role::name, identity())::get;
  }

  public interface AccessRestriction {
    boolean accessPermitted(Account account);
  }

  public static class AccessByRole<R extends Role> implements AccessRestriction {
    private final Function<? super String, ? extends Option<? extends R>> mapper;
    private final Set<? extends R> roles;

    public AccessByRole(Function<? super String, ? extends Option<? extends R>> mapper, Set<? extends R> roles) {
      this.mapper = mapper;
      this.roles = roles;
    }

    @Override
    public boolean accessPermitted(Account account) {
      return account.getRoles().stream()
          .map(mapper)
          .flatMap(Option::toJavaStream)
          .anyMatch(r -> roles.exists(r::implies));
    }

    @Override
    public String toString() {
      return getClass().getName() + "[" + roles + "]";
    }
  }

  static class AccessByRoleEnum<R extends Enum & Role> extends AccessByRole<R> {
    public AccessByRoleEnum(Class<R> enumType, Set<? extends R> roles) {
      super(enumMapper(enumType), roles);
    }
  }
}
