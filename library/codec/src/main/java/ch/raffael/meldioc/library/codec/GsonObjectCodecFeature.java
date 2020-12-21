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

package ch.raffael.meldioc.library.codec;

import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Provision;
import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.vavr.collection.List;
import io.vavr.gson.VavrGson;

import java.util.function.Consumer;


@Feature
public interface GsonObjectCodecFeature extends ObjectCodecFeature {

  @Deprecated(forRemoval = true)
  default Gson defaultGson() {
    return gson();
  }

  @Provision
  Gson gson();

  @Provision
  GsonObjectCodec.Factory gsonObjectCodecFactory();

  @Provision
  @Override
  default ObjectCodecFactory objectCodecFactory() {
    return gsonObjectCodecFactory();
  }

  @Feature
  abstract class Default implements GsonObjectCodecFeature {

    private final Configuration configuration = new Configuration();

    @Provision(singleton = true)
    public Gson gson() {
      var builder = new GsonBuilder();
      configuration.configurators().forEach(c -> c.accept(builder));
      return builder.create();
    }

    @Provision(singleton = true)
    @Override
    public GsonObjectCodec.Factory gsonObjectCodecFactory() {
      return new GsonObjectCodec.Factory(
          gson(),
          configuration.bufferSize().getOrElse(AbstractCharDataObjectCodec.DEFAULT_BUFFER_SIZE),
          configuration.defaultCharset());
    }

    @ExtensionPoint
    protected Configuration gsonObjectCodecFeatureConfiguration() {
      return configuration;
    }
  }

  @ExtensionPoint.Acceptor
  final class Configuration extends AbstractCharDataObjectCodec.Configuration<Configuration, GsonBuilder, Configuration.Standard> {
    public enum Standard implements Consumer<GsonBuilder> {
      SERVICE_LOADER {
        @Override
        public void accept(GsonBuilder b) {
          GsonObjectCodec.loadServiceLoaderTypeAdapters(b);
        }
      },
      JAVA_TIME {
        @Override
        public void accept(GsonBuilder b) {
          Converters.registerAll(b);
        }
      },
      VAVR {
        @Override
        public void accept(GsonBuilder b) {
          VavrGson.registerAll(b);
        }
      },
    }

    public Configuration() {
      super(List.of(Standard.values()));
    }

    @Override
    protected Standard[] allStandardConfigurators() {
      return Standard.values();
    }
  }
}
