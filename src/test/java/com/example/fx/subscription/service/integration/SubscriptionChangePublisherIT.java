package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.helper.PostgresTestContainerConfig;
import com.example.fx.subscription.service.model.EventsOutbox;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.service.EventsOutboxService;
import com.example.fx.subscription.service.service.SubscriptionChangePublisher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("kafka-test")
@Testcontainers
@Import(PostgresTestContainerConfig.class)
class SubscriptionChangePublisherIT {

  private static final String SUBSCRIPTION_CHANGE_EVENT_TOPIC = "subscription-change-events";

  @Container
  static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.2.0");

  @DynamicPropertySource
  static void kafkaProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    registry.add("spring.kafka.topic.subscription-changes", () -> SUBSCRIPTION_CHANGE_EVENT_TOPIC);
  }

  @Autowired
  EventsOutboxRepository eventsOutboxRepository;

  @Autowired
  EventsOutboxService eventsOutboxService;

  @Autowired
  SubscriptionChangePublisher subscriptionChangePublisher;

  private static KafkaConsumer<String, SubscriptionChangeEvent> consumer;

  @BeforeAll
  static void setUpConsumer() {
    Properties props = new Properties();
    props.put("bootstrap.servers", kafka.getBootstrapServers());
    props.put("group.id", "subscription-change-publisher-it");
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
    if (consumer != null) {
      consumer.close();
    }
  }

  @Test
  void sendMessage_publishesToKafka_andMarksOutboxSent() {
    // given
    EventsOutbox outbox = createAndSaveOutbox();

    // when
    subscriptionChangePublisher.sendMessage(toEvent(outbox));

    // then
    ConsumerRecords<String, SubscriptionChangeEvent> records =
            consumer.poll(Duration.ofSeconds(2));

    List<ConsumerRecord<String, SubscriptionChangeEvent>> list = new ArrayList<>();
    records.iterator().forEachRemaining(list::add);

    ConsumerRecord<String, SubscriptionChangeEvent> found =
            list.stream()
                    .filter(r -> SUBSCRIPTION_CHANGE_EVENT_TOPIC.equals(r.topic()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No record received from Kafka"));

    assertThat(found.key()).isEqualTo(outbox.getPayload().id());
    assertThat(found.value().eventId()).isEqualTo(toEvent(outbox).eventId());

    String status = eventsOutboxService.findOutboxById(outbox.getId().toString()).getStatus();
    assertThat(status).isEqualTo("SENT");
  }

  @Test
  void sendMessage_failsWhenPayloadTooLarge_andMarksOutboxFailed() {
    // given: A payload much larger than the default 1MB limit
    String bloatedString = "A".repeat(2 * 1024 * 1024);

    EventsOutbox outbox = createAndSaveOutbox();
    outbox.setPayload(new SubscriptionResponse(
            outbox.getAggregateId().toString(),
            null,
            bloatedString,
            BigDecimal.valueOf(1.25),
            ThresholdDirection.ABOVE,
            List.of("EMAIL"),
            SubscriptionStatus.ACTIVE,
            Instant.now().toString(),
            Instant.now().toString()
    ));
    eventsOutboxRepository.saveAndFlush(outbox);

    SubscriptionChangeEvent event = toEvent(outbox);

    // when
    subscriptionChangePublisher.sendMessage(event);

    // then
    String status = eventsOutboxService.findOutboxById(outbox.getId().toString()).getStatus();
    assertThat(status).isEqualTo("FAILED");

    ConsumerRecords<String, SubscriptionChangeEvent> records = consumer.poll(Duration.ofMillis(500));
    assertThat(records).isEmpty();
  }

  private EventsOutbox createAndSaveOutbox() {
    EventsOutbox outbox = new EventsOutbox();
    outbox.setAggregateId(UUID.randomUUID());
    outbox.setEventType("SubscriptionCreated");
    outbox.setStatus("PENDING");
    outbox.setPayload(new SubscriptionResponse(
            outbox.getAggregateId().toString(),
            null,
            "GBP/USD",
            BigDecimal.valueOf(1.25),
            ThresholdDirection.ABOVE,
            List.of("EMAIL"),
            SubscriptionStatus.ACTIVE,
            Instant.now().toString(),
            Instant.now().toString()
    ));
    outbox.setTimestamp(System.currentTimeMillis());
    return eventsOutboxRepository.saveAndFlush(outbox);
  }

  private SubscriptionChangeEvent toEvent(EventsOutbox outbox) {
    return new SubscriptionChangeEvent(
            outbox.getId().toString(),
            outbox.getTimestamp(),
            outbox.getEventType(),
            outbox.getPayload()
    );
  }
}
