package com.example.fx.subscription.service.helper;

import com.example.fx.subscription.service.model.FxUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import static com.example.fx.subscription.service.helper.WithMockFxUser.userId;

public class WithMockFxUserSecurityContextFactory implements WithSecurityContextFactory<WithMockFxUser> {
  @Override
  public SecurityContext createSecurityContext(WithMockFxUser annotation) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    FxUser user = new FxUser(userId, annotation.email(), "password", annotation.role());
    Authentication auth = new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities());
    context.setAuthentication(auth);
    return context;
  }
}
