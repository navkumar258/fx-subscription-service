package com.example.fx.subscription.service;

import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class FxSubscriptionServiceApplicationTest {

  @Autowired
  private ApplicationContext applicationContext;

  // Mock external dependencies to speed up tests
  @MockitoBean
  KafkaAdmin kafkaAdmin;

  @MockitoBean
  KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

  @Test
  void contextLoads() {
    // Then
    assertNotNull(applicationContext, "Application context should be loaded");
  }
}
