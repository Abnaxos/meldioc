module ch.raffael.meldioc.util.logging {
  exports ch.raffael.meldioc.logging;
  exports ch.raffael.meldioc.logging.spi;

  requires transitive org.slf4j;
  requires static java.logging;
  requires static org.apache.logging.log4j;
  // TODO #auto org.slf4j:jul-to-slf4j
  requires static auto.jul.to.slf4j;
}
