module ch.raffael.meldioc.library.http.server.undertow {
  exports ch.raffael.meldioc.library.http.server.undertow;
  exports ch.raffael.meldioc.library.http.server.undertow.codec;
  exports ch.raffael.meldioc.library.http.server.undertow.handler;
  exports ch.raffael.meldioc.library.http.server.undertow.routing;
  exports ch.raffael.meldioc.library.http.server.undertow.security;
  exports ch.raffael.meldioc.library.http.server.undertow.util;

  requires transitive ch.raffael.meldioc.library.base;
  requires transitive ch.raffael.meldioc.library.codec;
  // TODO #auto io.undertow:undertow-core
  requires transitive auto.undertow.core;
  // TODO #auto org.jboss.threads:jboss-threads
  requires transitive auto.jboss.threads;
  // TODO #auto org.jboss.xnio:xnio-api
  requires transitive auto.xnio.api;

  requires ch.raffael.meldioc.logging;

  // TODO #auto org.jboss.xnio:xnio-nio
  // TODO #auto org.wildfly.common:wildfly-common
  // TODO #auto org.wildfly.client:wildfly-client-config
  // TODO #auto org.jboss.logging:jboss-logging
}
