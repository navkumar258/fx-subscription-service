package com.example.fx.subscription.service.config;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

@Component
public class ToolProviderLogger {
  private final ToolCallbackProvider provider;

  public ToolProviderLogger(ToolCallbackProvider provider) {
    this.provider = provider;
  }

  @PostConstruct
  public void logRegisteredTools() {
    // The actual method to list tools is not public, so we log the class as a proxy
    System.out.println("✅ ToolCallbackProvider is initialized: " + provider.getClass());

    // If it's a MethodToolCallbackProvider, we can cast and access internal tools reflectively
    if (provider instanceof MethodToolCallbackProvider methodProvider) {
      System.out.println("🧩 MethodToolCallbackProvider registered: " + methodProvider);
    }
  }

}
