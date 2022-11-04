module ch.raffael.meldioc.library.codec {
  exports ch.raffael.meldioc.library.codec;

  requires transitive io.vavr;
  requires ch.raffael.meldioc.util;

  requires static transitive ch.raffael.meldioc.annotations;
  requires org.immutables.value.annotations;
}
