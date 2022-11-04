module ch.raffael.meldioc.library.base {
  exports ch.raffael.meldioc.library.base;
  exports ch.raffael.meldioc.library.base.lifecycle;
  exports ch.raffael.meldioc.library.base.threading;
  exports ch.raffael.meldioc.library.base.jmx.registry;
  exports ch.raffael.meldioc.library.base.jmx.registry.std;
  exports ch.raffael.meldioc.library.base.jmx.registry.util;
  exports ch.raffael.meldioc.library.base.security.ssl;

  requires ch.raffael.meldioc.logging;
  requires transitive ch.raffael.meldioc.util;
  requires transitive org.slf4j;
  requires transitive io.vavr;
  requires transitive typesafe.config;

  requires static transitive java.management;
  requires static transitive ch.raffael.meldioc.annotations;
  requires static org.jetbrains.annotations;
  requires static org.immutables.value.annotations;
}
