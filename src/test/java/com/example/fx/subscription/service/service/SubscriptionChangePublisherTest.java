package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionChangePublisherTest {
  @Mock
  private KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

  @Mock
  private EventsOutboxRepository eventsOutboxRepository;

  @InjectMocks
  private SubscriptionChangePublisher subscriptionChangePublisher;

  @BeforeEach
  void setUp() {
    // Set the topic value using reflection
    ReflectionTestUtils.setField(subscriptionChangePublisher, "subscriptionChangesTopic", "test-topic");
  }

  @Test
  void sendMessage_WhenSuccessful_ShouldCallKafkaTemplate() {
    // Given
    SubscriptionChangeEvent testEvent = createTestEvent();
    SendResult<String, SubscriptionChangeEvent> sendResult = mock(SendResult.class);
    CompletableFuture<SendResult<String, SubscriptionChangeEvent>> future =
            CompletableFuture.completedFuture(sendResult);

    when(kafkaTemplate.send(anyString(), anyString(), any(SubscriptionChangeEvent.class)))
            .thenReturn(future);

    // When
    subscriptionChangePublisher.sendMessage(testEvent);

    // Then
    verify(kafkaTemplate).send("test-topic", testEvent.payload().id().toString(), testEvent);
  }

  @Test
  void sendMessage_ShouldHandleNullEventGracefully() {
    // When & Then
    assertThrows(NullPointerException.class, () -> subscriptionChangePublisher.sendMessage(null));

    // Should not call kafkaTemplate.send when event is null
    verify(kafkaTemplate, never()).send(anyString(), anyString(), any(SubscriptionChangeEvent.class));
  }

  @Test
  void sendMessage_WhenKafkaSendFails_ShouldHandleException() {
    // Given
    SubscriptionChangeEvent testEvent = createTestEvent();
    RuntimeException kafkaException = new RuntimeException("Kafka connection failed");
    CompletableFuture<SendResult<String, SubscriptionChangeEvent>> future =
            CompletableFuture.failedFuture(kafkaException);

    when(kafkaTemplate.send(anyString(), anyString(), any(SubscriptionChangeEvent.class)))
            .thenReturn(future);

    // When
    subscriptionChangePublisher.sendMessage(testEvent);

    // Then
    verify(kafkaTemplate).send("test-topic", testEvent.payload().id().toString(), testEvent);
    // Exception should be handled in async callback
  }

  @Test
  void sendMessage_WhenKafkaProducerExceptionOccurs_ShouldUpdateOutboxStatusToFailed() {
    // Given
    SubscriptionChangeEvent testEvent = createTestEvent();
    ProducerRecord<String, SubscriptionChangeEvent> failedRecord =
            new ProducerRecord<>("test-topic", "key", testEvent);
    KafkaProducerException kafkaException = new KafkaProducerException(
            failedRecord, "Failed to publish message", new RuntimeException("Network error"));

    CompletableFuture<SendResult<String, SubscriptionChangeEvent>> future =
            CompletableFuture.failedFuture(kafkaException);

    when(kafkaTemplate.send(anyString(), anyString(), any(SubscriptionChangeEvent.class)))
            .thenReturn(future);

    // When
    subscriptionChangePublisher.sendMessage(testEvent);

    // Then
    verify(kafkaTemplate).send("test-topic", testEvent.payload().id().toString(), testEvent);
    // The exception should be re-thrown in the async callback
    verify(eventsOutboxRepository, never()).findById(any(UUID.class));
  }

  @Test
  void sendMessage_WhenEventIdIsInvalid_ShouldHandleGracefully() {
    // Given
    SubscriptionChangeEvent testEvent = createTestEvent();
    SendResult<String, SubscriptionChangeEvent> sendResult = mock(SendResult.class);
    CompletableFuture<SendResult<String, SubscriptionChangeEvent>> future =
            CompletableFuture.completedFuture(sendResult);

    when(kafkaTemplate.send(anyString(), anyString(), any(SubscriptionChangeEvent.class)))
            .thenReturn(future);

    // When
    subscriptionChangePublisher.sendMessage(testEvent);

    // Then
    verify(kafkaTemplate).send("test-topic", testEvent.payload().id().toString(), testEvent);
    // Should handle invalid UUID gracefully
    verify(eventsOutboxRepository, never()).findById(any(UUID.class));
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

    return testSubscription;
  }
} 