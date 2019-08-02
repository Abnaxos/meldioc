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

package ch.raffael.compose.usecases.undertow.hello.security;

import ch.raffael.compose.http.undertow.Role;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Set;

import javax.annotation.Nullable;
import java.security.Principal;

import static io.vavr.API.*;

public class HelloIdentityManager implements IdentityManager {

  private final Map<String, Tuple2<String, Set<HelloRole>>> users = HashMap
      .<String, Tuple2<String, Set<HelloRole>>>empty()
      .put("user", Tuple("geheim", Set(HelloRole.USER)))
      .put("admin", Tuple("sehrGeheim", Set(HelloRole.ADMIN)));

  @Override
  @Nullable
  public Account verify(Account account) {
    return account;
  }

  @Override
  @Nullable
  public Account verify(String id, Credential credential) {
    if (!(credential instanceof PasswordCredential)) {
      return null;
    }
    var password = (PasswordCredential) credential;
    return users.get(id)
        .filter(u -> new String(password.getPassword()).equals(u._1))
        .map(u -> new Account() {
          private final Principal principal = () -> id;
          private final java.util.Set<String> roles = u._2.map(Role::name).toJavaSet();
          @Override
          public Principal getPrincipal() {
            return principal;
          }
          @Override
          public java.util.Set<String> getRoles() {
            return roles;
          }
        })
        .getOrNull();
  }

  @Override
  @Nullable
  public Account verify(Credential credential) {
    return null;
  }
}
