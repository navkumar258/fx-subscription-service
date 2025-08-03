package com.example.fx.subscription.service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
        info = @Info(
                title = "FX Subscription Service",
                version = "1.0.0",
                description = "This api manages foreign exchange (FX) rate subscriptions and users with real-time notifications, MCP (Model Context Protocol) server capabilities",
                contact = @Contact(name = "API Support", email = "support@navkumar258.com", url = "https://navkumar258.github.io"),
                license = @License(name = "MIT License", url = "https://mit-license.org/")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {}

