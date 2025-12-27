package com.example.fx.subscription.service;

import com.example.fx.subscription.service.ai.tool.FxSubscriptionTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
public class FxSubscriptionServiceApplication {

  static void main(String[] args) {
    SpringApplication.run(FxSubscriptionServiceApplication.class, args);
  }

  @Bean
  ToolCallbackProvider methodToolCallbackProvider(FxSubscriptionTool fxSubscriptionTool) {
    return MethodToolCallbackProvider
            .builder()
            .toolObjects(fxSubscriptionTool)
            .build();
  }
}
