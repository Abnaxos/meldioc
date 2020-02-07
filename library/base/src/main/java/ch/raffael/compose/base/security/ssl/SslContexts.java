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

package ch.raffael.compose.base.security.ssl;

import io.vavr.Lazy;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class SslContexts {

  private static final Lazy<SSLContext> trustAllSslContext = Lazy.of(SslContexts::createTrustAll);

  private SslContexts() {
  }

  public static SSLContext trustAll() {
    return trustAllSslContext.get();
  }

  public static SSLContext system() {
    try {
      return SSLContext.getDefault();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Error initializing SSL: " + e, e);
    }
  }

  private static SSLContext createTrustAll() {
    SSLContext ctx;
    try {
      ctx = SSLContext.getInstance("SSL");
      var trustAll = new X509TrustManager() {
        @SuppressWarnings("NullabilityAnnotations")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
          // never throw
        }

        @SuppressWarnings("NullabilityAnnotations")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
          // never throw
        }

        @SuppressWarnings({"ZeroLengthArrayAllocation"})
        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }
      };
      ctx.init(null, new TrustManager[]{trustAll}, new SecureRandom());
      return ctx;
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to create trust-all SSL context: " + e.toString(), e);
    }
  }

}
