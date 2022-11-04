module ch.raffael.meldioc.library.codec.gson {
  exports ch.raffael.meldioc.library.codec.gson;

  requires transitive com.google.gson;
  requires transitive io.vavr;
  requires transitive ch.raffael.meldioc.library.codec;
  requires ch.raffael.meldioc.util;
  // TODO #auto com.fatboyindustrial.gson-javatime-serialisers:gson-javatime-serialisers
  requires auto.gson.javatime.serialisers;

  requires static org.jetbrains.annotations;
}
