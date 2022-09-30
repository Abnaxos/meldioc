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

package ch.raffael.meldioc.library.base.jmx.registry.util;

import ch.raffael.meldioc.util.immutables.Immutable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import org.immutables.value.Value;

import java.util.Comparator;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;


/**
 * Configurable mapper to determine domain names from classes. Usually, the
 * class of the managed object should be used.
 */
@Immutable.Pure
public abstract class DomainMappings implements DomainMappings_With {

  DomainMappings() {}
  public static Builder builder() {return new Builder();}
  public static DomainMappings of(String defaultDomain) {
    return DomainMappings_Immutable.of(defaultDomain);
  }

  @Value.Parameter
  public abstract String defaultDomain();

  public abstract Map<String, String> mappings();

  public DomainMappings addMapping(String baseName, String domain) {
    return withMappings(mappings().put(baseName, domain));
  }

  public String domainFor(String baseName) {
    return mappings()
        .flatMap(m -> baseName.equals(m._1) || baseName.startsWith(m._1 + ".")
                       ? some(Tuple.of(m._2, m._1.length()))
                       : none())
        .maxBy(Comparator.comparingInt(Tuple2::_2))
        .map(Tuple2::_1)
        .getOrElse(defaultDomain());
  }

  public String domainFor(Class<?> type) {
    return domainFor(type.getName());
  }

  public static final class Builder extends DomainMappings_Immutable.Builder {
    Builder() {}
    Builder mapping(String base, String domain) {
      return putMappings(base, domain);
    }
  }
}
