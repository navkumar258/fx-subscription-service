package com.example.fx.subscription.service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenApiConfigTest {

  private OpenApiConfig openApiConfig;
  private static final String MOCK_VERSION = "1.2.3-TEST";

  @Mock
  private BuildProperties buildProperties;

  @BeforeEach
  void setUp() {
    when(buildProperties.getVersion()).thenReturn(MOCK_VERSION);
    openApiConfig = new OpenApiConfig(buildProperties);
  }

  @Test
  void testOpenApiBeanCreation() {
    // When
    OpenAPI openAPI = openApiConfig.openAPI();

    // Then
    assertNotNull(openAPI);
  }

  @Test
  void testOpenApiInfoDetails() {
    // When
    OpenAPI openAPI = openApiConfig.openAPI();
    Info info = openAPI.getInfo();

    // Then
    assertNotNull(info);
    assertEquals("FX Subscription Service", info.getTitle());
    assertEquals(MOCK_VERSION, info.getVersion());
    assertEquals("This API manages foreign exchange (FX) rate subscriptions and users with real-time notifications, MCP (Model Context Protocol) server capabilities.", info.getDescription());
    assertEquals("API Support", info.getContact().getName());
    assertEquals("support@navkumar258.com", info.getContact().getEmail());
    assertEquals("https://navkumar258.github.io", info.getContact().getUrl());
    assertEquals("MIT License", info.getLicense().getName());
    assertEquals("https://mit-license.org/", info.getLicense().getUrl());
  }

  @Test
  void testOpenApiSecurityConfiguration() {
    // When
    OpenAPI openAPI = openApiConfig.openAPI();
    Components components = openAPI.getComponents();

    // Then
    assertNotNull(components);
    SecurityScheme securityScheme = components.getSecuritySchemes().get("bearerAuth");
    assertNotNull(securityScheme);
    assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
    assertEquals("bearer", securityScheme.getScheme());
    assertEquals("JWT", securityScheme.getBearerFormat());
  }

  @Test
  void testOpenApiGlobalSecurityRequirement() {
    // When
    OpenAPI openAPI = openApiConfig.openAPI();

    // Then
    assertNotNull(openAPI.getSecurity());
    assertEquals(1, openAPI.getSecurity().size());

    SecurityRequirement securityRequirement = openAPI.getSecurity().getFirst();

    assertTrue(securityRequirement.containsKey("bearerAuth"));
  }
}
