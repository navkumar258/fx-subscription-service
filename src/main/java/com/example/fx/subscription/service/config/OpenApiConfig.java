package com.example.fx.subscription.service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  private static final String BEARER_AUTH = "bearerAuth";

  private final BuildProperties buildProperties;

  public OpenApiConfig(BuildProperties buildProperties) {
    this.buildProperties = buildProperties;
  }

  @Bean
  public OpenAPI openAPI() {
    SecurityScheme securityScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT");

    SecurityRequirement securityRequirement = new SecurityRequirement().addList(BEARER_AUTH);

    return new OpenAPI()
            .info(new Info()
                    .title("FX Subscription Service")
                    .version(buildProperties.getVersion())
                    .description("This API manages foreign exchange (FX) rate subscriptions and users with real-time notifications, MCP (Model Context Protocol) server capabilities.")
                    .contact(new Contact().name("API Support").email("support@navkumar258.com").url("https://navkumar258.github.io"))
                    .license(new License().name("MIT License").url("https://mit-license.org/")))
            .components(new Components().addSecuritySchemes(BEARER_AUTH, securityScheme))
            .addSecurityItem(securityRequirement);
  }
}

