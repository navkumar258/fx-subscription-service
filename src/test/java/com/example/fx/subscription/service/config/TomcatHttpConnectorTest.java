package com.example.fx.subscription.service.config;

import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class TomcatHttpConnectorTest {

  @Test
  void servletContainer_shouldAddAdditionalConnectorWithCorrectPort() {
    int httpPort = 8081;
    TomcatHttpConnector config = new TomcatHttpConnector();

    ServletWebServerFactory factory = config.servletContainer(httpPort);

    assertThat(factory).isInstanceOf(TomcatServletWebServerFactory.class);

    Connector connector = config.createStandardConnector(httpPort);
    assertThat(connector.getPort()).isEqualTo(httpPort);
  }

}
