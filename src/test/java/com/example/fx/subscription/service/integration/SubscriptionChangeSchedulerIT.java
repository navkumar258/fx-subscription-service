package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.helper.PostgresTestContainerConfig;
import com.example.fx.subscription.service.model.EventsOutbox;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.service.SubscriptionChangeScheduler;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("kafka-test")
@Testcontainers
@Import(PostgresTestContainerConfig.class)
class SubscriptionChangeSchedulerIT {

  private static final String SUBSCRIPTION_CHANGE_EVENT_TOPIC = "subscription-change-events";

  @Container
  static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.2.0");

  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    registry.add("spring.kafka.topic.subscription-changes", () -> SUBSCRIPTION_CHANGE_EVENT_TOPIC);
  }

  @Autowired
  private EventsOutboxRepository eventsOutboxRepository;

  @Autowired
  private SubscriptionChangeScheduler subscriptionChangeScheduler;

  private static KafkaConsumer<String, SubscriptionChangeEvent> consumer;

  @BeforeAll
  static void setUpConsumer() {
    Properties props = new Properties();
    props.put("bootstrap.servers", kafka.getBootstrapServers());
    props.put("group.id", "scheduler-it-group");
    props.put("auto.offset.reset", "earliest");
    props.put("key.deserializer", StringDeserializer.class.getName());
    props.put("value.deserializer", JacksonJsonDeserializer.class.getName());
    props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, SubscriptionChangeEvent.class);
    props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.example.fx.subscription.service.model");

    consumer = new KafkaConsumer<>(props);
    consumer.subscribe(Collections.singletonList(SUBSCRIPTION_CHANGE_EVENT_TOPIC));
  }

  @BeforeEach
  void cleanDb() {
    eventsOutboxRepository.deleteAll();
  }

  @AfterAll
  static void tearDown() {
    if (consumer != null) consumer.close();
  }

  @Test
  void scheduler_shouldProcessPendingRecords_andUpdateToSent() {
    // 1. GIVEN: Save a PENDING record in Postgres
    EventsOutbox outbox = createOutboxRecord("PENDING", "GBP/USD");
    eventsOutboxRepository.saveAndFlush(outbox);

    // 2. WHEN: Manually trigger the scheduler
    subscriptionChangeScheduler.checkForOutboxSubscriptions();

    // 3. THEN: Verify Postgres status updated to SENT
    EventsOutbox updated = eventsOutboxRepository.findById(outbox.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo("SENT");

    // 4. THEN: Verify Kafka received the message
    var records = consumer.poll(Duration.ofSeconds(5));
    assertThat(records.count()).isGreaterThanOrEqualTo(1);
  }

  @Test
  void scheduler_shouldHandleFailures_andMarkAsFailed() {
    // 1. GIVEN: A record that will trigger the "Too Large" failure in the Publisher
    String hugeData = "X".repeat(2 * 1024 * 1024); // 2MB payload
    EventsOutbox outbox = createOutboxRecord("PENDING", hugeData);
    eventsOutboxRepository.saveAndFlush(outbox);

    // 2. WHEN: Trigger scheduler
    subscriptionChangeScheduler.checkForOutboxSubscriptions();

    // 3. THEN: Verify Postgres status updated to FAILED because of the Publisher's try-catch
    EventsOutbox updated = eventsOutboxRepository.findById(outbox.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo("FAILED");
  }

  @Test
  void scheduler_shouldIgnoreAlreadySentRecords() {
    // 1. GIVEN: A record already marked as SENT
    EventsOutbox outbox = createOutboxRecord("SENT", "GBP/USD");
    eventsOutboxRepository.saveAndFlush(outbox);

    // 2. WHEN: Trigger scheduler
    subscriptionChangeScheduler.checkForOutboxSubscriptions();

    // 3. THEN: Verify Kafka consumer is empty
    var records = consumer.poll(Duration.ofMillis(500));
    assertThat(records.isEmpty()).isTrue();
  }

  private EventsOutbox createOutboxRecord(String status, String currencyPair) {
    EventsOutbox outbox = new EventsOutbox();
    outbox.setAggregateId(UUID.randomUUID());
    outbox.setEventType("SubscriptionCreated");
    outbox.setStatus(status);
    outbox.setTimestamp(System.currentTimeMillis());
    outbox.setPayload(new SubscriptionResponse(
            outbox.getAggregateId().toString(),
            null,
            currencyPair,
            BigDecimal.valueOf(1.10),
            ThresholdDirection.BELOW,
            List.of("SMS"),
            SubscriptionStatus.ACTIVE,
            Instant.now().toString(),
            Instant.now().toString()
    ));
    return outbox;
  }
} 