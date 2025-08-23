package com.example.fx.subscription.service.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty("server.http.port")
@Configuration
public class TomcatHttpConnector {

  @Bean
  public ServletWebServerFactory servletContainer(
          @Value("${server.http.port}") int httpPort) {
    TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
    tomcat.addAdditionalTomcatConnectors(createStandardConnector(httpPort));
    return tomcat;
  }

  Connector createStandardConnector(int port) {
    Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
    connector.setPort(port);
    return connector;
  }
}
