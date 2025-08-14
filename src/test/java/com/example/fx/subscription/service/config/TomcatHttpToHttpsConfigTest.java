package com.example.fx.subscription.service.config;

import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TomcatHttpToHttpsConfigTest {

  @Mock
  private TomcatServletWebServerFactory factory;

  @Captor
  private ArgumentCaptor<Connector> connectorCaptor;

  private TomcatHttpToHttpsConfig tomcatHttpToHttpsConfig;

  @BeforeEach
  void setUp() {
    tomcatHttpToHttpsConfig = new TomcatHttpToHttpsConfig();
  }

  @Test
  void customize_ShouldAddHttpConnector() {
    // When
    tomcatHttpToHttpsConfig.customize(factory);

    // Then
    verify(factory).addAdditionalTomcatConnectors(any(Connector.class));
  }

  @Test
  void createHttpConnector_ShouldCreateConnectorWithCorrectConfiguration() {
    // When
    tomcatHttpToHttpsConfig.customize(factory);

    // Then
    verify(factory).addAdditionalTomcatConnectors(connectorCaptor.capture());

    Connector connector = connectorCaptor.getValue();
    assertEquals("http", connector.getScheme());
    assertEquals(8080, connector.getPort());
    assertFalse(connector.getSecure());
    assertEquals(8443, connector.getRedirectPort());
  }

  @Test
  void createHttpConnector_ShouldUseHttp11NioProtocol() {
    // When
    tomcatHttpToHttpsConfig.customize(factory);

    // Then
    verify(factory).addAdditionalTomcatConnectors(connectorCaptor.capture());

    Connector connector = connectorCaptor.getValue();
    assertEquals("org.apache.coyote.http11.Http11NioProtocol", connector.getProtocol());
  }

  @Test
  void customize_ShouldBeCalledMultipleTimes() {
    // Given
    TomcatServletWebServerFactory factory2 = mock(TomcatServletWebServerFactory.class);

    // When
    tomcatHttpToHttpsConfig.customize(factory);
    tomcatHttpToHttpsConfig.customize(factory2);

    // Then
    verify(factory).addAdditionalTomcatConnectors(any(Connector.class));
    verify(factory2).addAdditionalTomcatConnectors(any(Connector.class));
  }

  @Test
  void customize_ShouldCreateNewConnectorEachTime() {
    // When
    tomcatHttpToHttpsConfig.customize(factory);

    // Then
    verify(factory).addAdditionalTomcatConnectors(connectorCaptor.capture());

    Connector firstConnector = connectorCaptor.getValue();
    assertNotNull(firstConnector);

    // Call customize again
    tomcatHttpToHttpsConfig.customize(factory);

    verify(factory, times(2)).addAdditionalTomcatConnectors(connectorCaptor.capture());

    Connector secondConnector = connectorCaptor.getValue();
    assertNotNull(secondConnector);

    // Each call should create a new connector instance
    assertNotSame(firstConnector, secondConnector);
  }

  @Test
  void tomcatHttpToHttpsConfig_ShouldImplementWebServerFactoryCustomizer() {
    // Then
    assertInstanceOf(WebServerFactoryCustomizer.class, tomcatHttpToHttpsConfig);
  }
}
