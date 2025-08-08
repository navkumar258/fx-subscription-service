package com.example.fx.subscription.service.helper;

import com.example.fx.subscription.service.model.UserRole;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockFxUserSecurityContextFactory.class)
public @interface WithMockFxUser {
  UUID userId = UUID.fromString("7ca3517a-1930-4e18-916e-cae40f5dcfbe");

  UserRole role() default UserRole.USER;

  String email() default "testuser@example.com";
}
