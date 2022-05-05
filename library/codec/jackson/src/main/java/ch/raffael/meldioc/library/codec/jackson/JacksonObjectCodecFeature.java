/*
 *  Copyright (c) 2022 Raffael Herzog
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

package ch.raffael.meldioc.library.codec.jackson;

import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.library.codec.AbstractCharDataObjectCodec;
import ch.raffael.meldioc.library.codec.ObjectCodecFactory;
import ch.raffael.meldioc.library.codec.ObjectCodecFeature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.jackson.datatype.VavrModule;

import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * TODO JavaDoc
 */
@Feature
public interface JacksonObjectCodecFeature extends ObjectCodecFeature {

  @Provision
  ObjectMapper jacksonObjectMapper();

  @Provision
  JacksonObjectCodec.Factory jacksonObjectCodecFactory();

  @Feature
  abstract class Default implements JacksonObjectCodecFeature {
    private final Configuration configuration = new Configuration();

    @Provision(singleton = true)
    @Override
    public ObjectMapper jacksonObjectMapper() {
      var mapper = new ObjectMapper();
      mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
      mapper.configure(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM, false);
      mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
      configuration.configurators().forEach(c -> c.accept(mapper));
      return mapper;
    }

    @Provision(singleton = true)
    @Override
    public JacksonObjectCodec.Factory jacksonObjectCodecFactory() {
      return new JacksonObjectCodec.Factory(
          jacksonObjectMapper(),
          configuration.bufferSize().getOrElse(AbstractCharDataObjectCodec.DEFAULT_BUFFER_SIZE),
          configuration.defaultCharset());
    }

    @Provision
    @Override
    public ObjectCodecFactory objectCodecFactory() {
      return jacksonObjectCodecFactory();
    }

    @ExtensionPoint
    protected Configuration jacksonObjectCodecConfiguration() {
      return configuration;
    }
  }

  @ExtensionPoint
  final class Configuration extends AbstractCharDataObjectCodec.Configuration<Configuration, ObjectMapper, Configuration.Standard> {
    enum Standard implements Consumer<ObjectMapper> {
      JAVA8 {
        @Override
        public void accept(ObjectMapper objectMapper) {
          objectMapper.registerModule(new Jdk8Module());
        }
      },
      JAVA_TIME {
        @Override
        public void accept(ObjectMapper objectMapper) {
          objectMapper.registerModule(new JavaTimeModule());
        }
      },
      VAVR {
        @Override
        public void accept(ObjectMapper objectMapper) {
          objectMapper.registerModule(new VavrModule());
        }
      }
    }

    public Configuration() {
      super(List.of(Standard.values()));
    }

    @Override
    protected Standard[] allStandardConfigurators() {
      return Standard.values();
    }

    @Override
    protected Seq<Consumer<? super ObjectMapper>> configurators() {
      return super.configurators();
    }

    @Override
    protected Option<Integer> bufferSize() {
      return super.bufferSize();
    }

    @Override
    protected Option<Charset> defaultCharset() {
      return super.defaultCharset();
    }
  }
}
