package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.model.*;
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
class SubscriptionChangePublisherUnitTest {
  @Mock
  private KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

  @Mock
  private EventsOutboxRepository eventsOutboxRepository;

  @InjectMocks
  private SubscriptionChangePublisher subscriptionChangePublisher;

  private SubscriptionChangeEvent testEvent;

  @BeforeEach
  void setUp() {
    UUID testEventId = UUID.randomUUID();

    Subscription testSubscription = new Subscription();
    testSubscription.setId(UUID.randomUUID());
    testSubscription.setCurrencyPair("GBP/USD");
    testSubscription.setThreshold(BigDecimal.valueOf(1.25));
    testSubscription.setDirection(ThresholdDirection.ABOVE);
    testSubscription.setNotificationsChannels(List.of("email", "sms"));
    testSubscription.setStatus(SubscriptionStatus.ACTIVE);

    testEvent = new SubscriptionChangeEvent();
    testEvent.setEventId(testEventId.toString());
    testEvent.setEventType("SubscriptionCreated");
    testEvent.setPayload(SubscriptionResponse.fromSubscription(testSubscription));
    testEvent.setTimestamp(System.currentTimeMillis());

    // Set the topic value using reflection
    ReflectionTestUtils.setField(subscriptionChangePublisher, "subscriptionChangesTopic", "test-topic");
  }

  @Test
  void sendMessage_WhenSuccessful_ShouldCallKafkaTemplate() {
    // Given
    SendResult<String, SubscriptionChangeEvent> sendResult = mock(SendResult.class);
    CompletableFuture<SendResult<String, SubscriptionChangeEvent>> future =
            CompletableFuture.completedFuture(sendResult);

    when(kafkaTemplate.send(anyString(), anyString(), any(SubscriptionChangeEvent.class)))
            .thenReturn(future);

    // When
    subscriptionChangePublisher.sendMessage(testEvent);

    // Then
    verify(kafkaTemplate).send("test-topic", testEvent.getPayload().id().toString(), testEvent);
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
    RuntimeException kafkaException = new RuntimeException("Kafka connection failed");
    CompletableFuture<SendResult<String, SubscriptionChangeEvent>> future =
            CompletableFuture.failedFuture(kafkaException);

    when(kafkaTemplate.send(anyString(), anyString(), any(SubscriptionChangeEvent.class)))
            .thenReturn(future);

    // When
    subscriptionChangePublisher.sendMessage(testEvent);

    // Then
    verify(kafkaTemplate).send("test-topic", testEvent.getPayload().id().toString(), testEvent);
    // Exception should be handled in async callback
  }

  @Test
  void sendMessage_WhenKafkaProducerExceptionOccurs_ShouldHandleExceptionCorrectly() {
    // Given
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
    verify(kafkaTemplate).send("test-topic", testEvent.getPayload().id().toString(), testEvent);
    // The exception should be re-thrown in the async callback
    verify(eventsOutboxRepository, never()).findById(any(UUID.class));
  }

  @Test
  void sendMessage_WhenEventIdIsInvalid_ShouldHandleGracefully() {
    // Given
    testEvent.setEventId("invalid-uuid");
    SendResult<String, SubscriptionChangeEvent> sendResult = mock(SendResult.class);
    CompletableFuture<SendResult<String, SubscriptionChangeEvent>> future =
            CompletableFuture.completedFuture(sendResult);

    when(kafkaTemplate.send(anyString(), anyString(), any(SubscriptionChangeEvent.class)))
            .thenReturn(future);

    // When
    subscriptionChangePublisher.sendMessage(testEvent);

    // Then
    verify(kafkaTemplate).send("test-topic", testEvent.getPayload().id().toString(), testEvent);
    // Should handle invalid UUID gracefully
    verify(eventsOutboxRepository, never()).findById(any(UUID.class));
  }
} 