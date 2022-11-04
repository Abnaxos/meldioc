module ch.raffael.meldioc.tools.model {
  exports ch.raffael.meldioc.model;
  exports ch.raffael.meldioc.model.config;
  exports ch.raffael.meldioc.model.messages;

  requires transitive ch.raffael.meldioc.annotations;
  requires transitive ch.raffael.meldioc.util;
  requires static org.immutables.value.annotations;
}
