package com.example.fx.subscription.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
public class FxSubscriptionServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(FxSubscriptionServiceApplication.class, args);
  }
}
