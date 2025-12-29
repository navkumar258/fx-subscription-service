package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.helper.PostgresTestContainerConfig;
import com.example.fx.subscription.service.model.EventsOutbox;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.service.EventsOutboxService;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("kafka-test")
@Testcontainers
@Import(PostgresTestContainerConfig.class)
class SubscriptionChangeSchedulerIT {

  @Container
  static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.1.1");

  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
  }

  @TestConfiguration
  static class TestKafkaProducerConfig {
    @Bean
    @Primary
    public ProducerFactory<String, SubscriptionChangeEvent> testProducerFactory(KafkaProperties properties) {
      Map<String, Object> configProps = new HashMap<>(properties.buildProducerProperties());
      configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
      configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 1006);
      configProps.put(ProducerConfig.METADATA_MAX_AGE_CONFIG, 1000);
      configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);
      configProps.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 1000);

      configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
      configProps.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);

      return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    @Primary
    public KafkaTemplate<String, SubscriptionChangeEvent> testKafkaTemplate(
            ProducerFactory<String, SubscriptionChangeEvent> pf) {
      return new KafkaTemplate<>(pf);
    }

    @Bean
    public NewTopic subscriptionChangeEvents() {
      return new NewTopic("subscription-change-events", 1, (short) 1);
    }
  }

  @Autowired
  private EventsOutboxRepository eventsOutboxRepository;

  @Autowired
  private EventsOutboxService eventsOutboxService;

  @Autowired
  private KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

  @AfterEach
  void clear() {
    eventsOutboxRepository.deleteAll();
  }

  @Test
  void givenPendingJob_whenKafkaSuccess_thenStatusIsSent() {
    EventsOutbox eventsOutbox = new EventsOutbox();
    eventsOutbox.setTimestamp(System.currentTimeMillis());
    eventsOutbox.setEventType("SubscriptionCreated");
    eventsOutbox.setStatus("PENDING");
    eventsOutbox.setPayload(new SubscriptionResponse(
            UUID.randomUUID().toString(),
            null,
            "GBP/USD",
            BigDecimal.ONE,
            ThresholdDirection.ABOVE,
            null,
            SubscriptionStatus.ACTIVE,
            Instant.now(),
            null
    ));
    eventsOutboxRepository.save(eventsOutbox);

    await()
            .atMost(Duration.ofSeconds(1))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> {
              EventsOutbox eventsOutbox1 = eventsOutboxRepository.findById(eventsOutbox.getId()).orElseThrow();
              assertEquals("SENT", eventsOutbox1.getStatus());
            });
  }

  @Test
  void givenPendingJob_whenKafkaFails_thenStatusIsFailed() {
    EventsOutbox eventsOutbox = new EventsOutbox();
    eventsOutbox.setTimestamp(System.currentTimeMillis());
    eventsOutbox.setEventType("SubscriptionCreated");
    eventsOutbox.setStatus("PENDING");
    eventsOutbox.setPayload(new SubscriptionResponse(
            UUID.randomUUID().toString(),
            null,
            "A".repeat(1_100_000),
            null,
            null,
            null,
            null,
            null,
            null
    ));
    eventsOutboxRepository.save(eventsOutbox);

    await()
            .atMost(Duration.ofSeconds(2))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> {
              EventsOutbox eventsOutbox1 = eventsOutboxRepository.findById(eventsOutbox.getId()).orElseThrow();
              assertEquals("FAILED", eventsOutbox1.getStatus());
            });
  }
} 