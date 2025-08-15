package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.helper.PostgresTestContainerConfig;
import com.example.fx.subscription.service.model.*;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.service.SubscriptionChangePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("kafka-test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(PostgresTestContainerConfig.class)
class SubscriptionChangePublisherIT {

  @Value(value = "${spring.kafka.topic.subscription-changes}")
  private String subscriptionChangesTopic;

  @Container
  static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.9.1");

  @DynamicPropertySource
  static void kafkaProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }

  @Autowired
  EventsOutboxRepository eventsOutboxRepository;

  @Autowired
  SubscriptionChangePublisher subscriptionChangePublisher;

  private final List<SubscriptionChangeEvent> receivedEvents = new ArrayList<>();

  @KafkaListener(
          topics = "${spring.kafka.topic.subscription-changes}",
          groupId = "test-group")
  public void listenForEvents(SubscriptionChangeEvent event) {
    receivedEvents.add(event);
  }

  @BeforeEach
  void setUp() {
    receivedEvents.clear();
  }

  @Test
  void sendMessage_WhenSuccessful_ShouldSendToKafka() {
    SubscriptionChangeEvent event = createTestEvent("SubscriptionCreated");

    subscriptionChangePublisher.sendMessage(event);

    // Then - Fixed await block
    await()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .until(() -> !receivedEvents.isEmpty());

    // Verify the event details
    assertThat(receivedEvents).hasSize(1);
    SubscriptionChangeEvent receivedEvent = receivedEvents.getFirst();
    assertThat(receivedEvent.eventType()).isEqualTo("SubscriptionCreated");
    assertThat(receivedEvent.payload().id()).isEqualTo(event.payload().id());
  }

  @Test
  void publishSubscriptionChangeEvent_WithNonExistentOutbox_ShouldNotThrowException() {
    // Given - Create event with non-existent outbox ID
    Subscription subscription = createTestSubscription();
    SubscriptionChangeEvent event = new SubscriptionChangeEvent(
            UUID.randomUUID().toString(), // Non-existent ID
            System.currentTimeMillis(),
            "SubscriptionCreated",
            SubscriptionResponse.fromSubscription(subscription)
    );

    // When & Then - Should not throw exception
    subscriptionChangePublisher.sendMessage(event);

    // Then - Fixed await block
    await()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .until(() -> !receivedEvents.isEmpty());

    // Verify the event details
    assertThat(receivedEvents).hasSize(1);

    // Verify no outbox was updated (since it doesn't exist)
    List<EventsOutbox> allOutboxes = eventsOutboxRepository.findAll();
    assertThat(allOutboxes).isEmpty();
  }

  @Test
  void sendMessage_WhenSubscriptionUpdated_ShouldSendUpdateEvent() {
    SubscriptionChangeEvent updateEvent = createTestEvent("SubscriptionUpdated");

    subscriptionChangePublisher.sendMessage(updateEvent);

    // Then - Fixed await block
    await()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .until(() -> !receivedEvents.isEmpty());

    // Verify the event details
    assertThat(receivedEvents).hasSize(1);
    SubscriptionChangeEvent receivedEvent = receivedEvents.getFirst();
    assertThat(receivedEvent.eventType()).isEqualTo("SubscriptionUpdated");
    assertThat(receivedEvent.payload().id()).isEqualTo(updateEvent.payload().id());
  }

  @Test
  void sendMessage_WhenSubscriptionDeleted_ShouldSendDeleteEvent() {
    SubscriptionChangeEvent deleteEvent = createTestEvent("SubscriptionDeleted");

    subscriptionChangePublisher.sendMessage(deleteEvent);

    // Then - Fixed await block
    await()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .until(() -> !receivedEvents.isEmpty());

    // Verify the event details
    assertThat(receivedEvents).hasSize(1);
    SubscriptionChangeEvent receivedEvent = receivedEvents.getFirst();
    assertThat(receivedEvent.eventType()).isEqualTo("SubscriptionDeleted");
    assertThat(receivedEvent.payload().id()).isEqualTo(deleteEvent.payload().id());
  }

  @Test
  void sendMultipleMessages_ShouldSendAllToKafka() {
    List<SubscriptionChangeEvent> events = List.of(
            createTestEvent("SubscriptionCreated"),
            createTestEvent("SubscriptionUpdated"),
            createTestEvent("SubscriptionDeleted")
    );

    events.forEach(subscriptionChangePublisher::sendMessage);

    // Then - Fixed await block
    await()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .until(() -> !receivedEvents.isEmpty());

    // Verify the event details
    assertThat(receivedEvents).hasSize(3);
  }

  private Subscription createTestSubscription() {
    Subscription subscription = new Subscription();
    subscription.setId(UUID.randomUUID());
    subscription.setCurrencyPair("GBP/USD");
    subscription.setThreshold(BigDecimal.valueOf(1.25));
    subscription.setDirection(ThresholdDirection.ABOVE);
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    return subscription;
  }

  private SubscriptionChangeEvent createTestEvent(String eventType) {
    return new SubscriptionChangeEvent(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            eventType,
            SubscriptionResponse.fromSubscription(createTestSubscription())
    );
  }
}
