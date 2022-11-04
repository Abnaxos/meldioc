module ch.raffael.meldioc.library.codec.jackson {
  exports ch.raffael.meldioc.library.codec.jackson;

  requires ch.raffael.meldioc.library.codec;
  requires io.vavr;
  requires io.vavr.jackson;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires com.fasterxml.jackson.datatype.jsr310;

  requires static org.jetbrains.annotations;
}
