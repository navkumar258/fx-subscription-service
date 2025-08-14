package com.example.fx.subscription.service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class OpenApiConfigTest {

  @Test
  void openApiConfig_ShouldHaveOpenAPIDefinitionAnnotation() {
    // When
    OpenAPIDefinition annotation = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);

    // Then
    assertNotNull(annotation, "OpenApiConfig should have @OpenAPIDefinition annotation");
  }

  @Test
  void openApiConfig_ShouldHaveCorrectSecuritySchemeConfiguration() {
    // When
    SecurityScheme annotation = OpenApiConfig.class.getAnnotation(SecurityScheme.class);

    // Then
    assertNotNull(annotation, "OpenApiConfig should have @SecurityScheme annotation");
    assertEquals("bearerAuth", annotation.name());
    assertEquals(SecuritySchemeType.HTTP, annotation.type());
    assertEquals("bearer", annotation.scheme());
    assertEquals("JWT", annotation.bearerFormat());
  }

  @Test
  void openApiConfig_ShouldHaveBasicApiInformation() {
    // When
    OpenAPIDefinition openApiDef = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);

    // Then
    assertNotNull(openApiDef);
    assertEquals("FX Subscription Service", openApiDef.info().title());
    assertEquals("1.0.0", openApiDef.info().version());
    assertNotNull(openApiDef.info().description());
    assertNotNull(openApiDef.info().license());
    assertNotNull(openApiDef.info().contact());
  }
}
