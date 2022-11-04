module ch.raffael.meldioc.util {
  exports ch.raffael.meldioc.util;
  exports ch.raffael.meldioc.util.advice;
  exports ch.raffael.meldioc.util.concurrent;
  exports ch.raffael.meldioc.util.immutables;

  requires static transitive io.vavr;
  requires static transitive org.jetbrains.annotations;
  requires static org.immutables.value.annotations;
  requires static org.immutables.vavr.encodings;
}
