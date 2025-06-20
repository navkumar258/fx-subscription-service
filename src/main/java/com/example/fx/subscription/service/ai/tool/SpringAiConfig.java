package com.example.fx.subscription.service.ai.tool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {
  private final OllamaChatModel ollamaChatModel;
  private final FxSubscriptionTool fxSubscriptionTool;

  public SpringAiConfig(OllamaChatModel ollamaChatModel, FxSubscriptionTool fxSubscriptionTool) {
    this.ollamaChatModel = ollamaChatModel;
    this.fxSubscriptionTool = fxSubscriptionTool;
  }

  @Bean
  public ChatClient chatClient() {
    return ChatClient
            .builder(ollamaChatModel)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .defaultTools(fxSubscriptionTool)
            .build();
  }
}
