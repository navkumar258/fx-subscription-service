package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionChangePublisherTest {
  @Mock
  private KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

  @Mock
  private EventsOutboxService eventsOutboxService;

  private SubscriptionChangePublisher subscriptionChangePublisher;
  private final String topic = "test-topic";

  @BeforeEach
  void setUp() {
    subscriptionChangePublisher = new SubscriptionChangePublisher(kafkaTemplate, eventsOutboxService, topic);
  }

  @Test
  void sendMessage_Success_ShouldUpdateStatusToSent() {
    // Given
    var event = createTestEvent();
    var metadata = new RecordMetadata(new org.apache.kafka.common.TopicPartition(topic, 0), 0L, 0, 0L, 0, 0);
    var sendResult = mock(SendResult.class);

    when(sendResult.getRecordMetadata()).thenReturn(metadata);
    when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(sendResult));

    // When
    subscriptionChangePublisher.sendMessage(event);

    // Then
    verify(eventsOutboxService).updateOutboxStatus(event.eventId(), "SENT");
    verify(kafkaTemplate).send(topic, event.payload().id(), event);
  }

  @Test
  void sendMessage_KafkaFailure_ShouldUpdateStatusToFailed() {
    // Given
    var event = createTestEvent();
    var future = new CompletableFuture<SendResult<String, SubscriptionChangeEvent>>();
    future.completeExceptionally(new RuntimeException("Kafka Down"));

    when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

    // When
    subscriptionChangePublisher.sendMessage(event);

    // Then
    verify(eventsOutboxService).updateOutboxStatus(event.eventId(), "FAILED");
  }

  @Test
  void sendMessage_WhenInterrupted_ShouldRestoreStatusAndFail() {
    // Given
    var event = createTestEvent();
    var future = new CompletableFuture<SendResult<String, SubscriptionChangeEvent>>();
    // .join() wraps checked exceptions in CompletionException
    future.completeExceptionally(new CompletionException(new InterruptedException("Interrupted!")));

    when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

    // When
    subscriptionChangePublisher.sendMessage(event);

    // Then
    verify(eventsOutboxService).updateOutboxStatus(event.eventId(), "FAILED");
    // Verify thread interrupt flag was restored
    assert Thread.interrupted();
  }

  private SubscriptionChangeEvent createTestEvent() {
    UUID testEventId = UUID.randomUUID();
    return new SubscriptionChangeEvent(
            testEventId.toString(),
            System.currentTimeMillis(),
            "SubscriptionCreated",
            SubscriptionResponse.fromSubscription(createTestSubscription())
    );
  }

  private Subscription createTestSubscription() {
    Subscription testSubscription = new Subscription();
    testSubscription.setId(UUID.randomUUID());
    testSubscription.setCurrencyPair("GBP/USD");
    testSubscription.setThreshold(BigDecimal.valueOf(1.25));
    testSubscription.setDirection(ThresholdDirection.ABOVE);
    testSubscription.setNotificationsChannels(List.of("email", "sms"));
    testSubscription.setStatus(SubscriptionStatus.ACTIVE);
    testSubscription.setCreatedAt(Instant.now());

    return testSubscription;
  }
} 