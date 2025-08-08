package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.model.*;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionChangeSchedulerTest {

  @Mock
  private EventsOutboxRepository eventsOutboxRepository;

  @Mock
  private SubscriptionChangePublisher subscriptionChangePublisher;

  @InjectMocks
  private SubscriptionChangeScheduler subscriptionChangeScheduler;

  private EventsOutbox testEventsOutbox;
  private UUID testEventId;

  @BeforeEach
  void setUp() {
    testEventId = UUID.randomUUID();

    Subscription testSubscription = new Subscription();
    testSubscription.setId(UUID.randomUUID());
    testSubscription.setCurrencyPair("GBP/USD");
    testSubscription.setThreshold(BigDecimal.valueOf(1.25));
    testSubscription.setDirection(ThresholdDirection.ABOVE);
    testSubscription.setNotificationsChannels(List.of("email", "sms"));
    testSubscription.setStatus(SubscriptionStatus.ACTIVE);

    testEventsOutbox = new EventsOutbox();
    testEventsOutbox.setId(testEventId);
    testEventsOutbox.setAggregateType("Subscription");
    testEventsOutbox.setAggregateId(testSubscription.getId());
    testEventsOutbox.setEventType("SubscriptionCreated");
    testEventsOutbox.setPayload(SubscriptionResponse.fromSubscription(testSubscription));
    testEventsOutbox.setStatus("PENDING");
    testEventsOutbox.setTimestamp(System.currentTimeMillis());
  }

  @Test
  void checkForOutboxSubscriptions_WhenPendingEventsExist_ShouldPublishAllEvents() {
    // Given
    List<EventsOutbox> pendingEvents = List.of(testEventsOutbox);
    when(eventsOutboxRepository.findByStatus("PENDING"))
            .thenReturn(pendingEvents);

    // When
    subscriptionChangeScheduler.checkForOutboxSubscriptions();

    // Then
    verify(eventsOutboxRepository).findByStatus("PENDING");
    verify(subscriptionChangePublisher).sendMessage(any(SubscriptionChangeEvent.class));
  }

  @Test
  void checkForOutboxSubscriptions_WhenNoPendingEvents_ShouldNotPublishAnyEvents() {
    // Given
    when(eventsOutboxRepository.findByStatus("PENDING"))
            .thenReturn(List.of());

    // When
    subscriptionChangeScheduler.checkForOutboxSubscriptions();

    // Then
    verify(eventsOutboxRepository).findByStatus("PENDING");
    verify(subscriptionChangePublisher, never()).sendMessage(any(SubscriptionChangeEvent.class));
  }

  @Test
  void checkForOutboxSubscriptions_WhenMultiplePendingEvents_ShouldPublishAllEvents() {
    // Given
    EventsOutbox secondEvent = new EventsOutbox();
    secondEvent.setId(UUID.randomUUID());
    secondEvent.setAggregateType("Subscription");
    secondEvent.setAggregateId(UUID.randomUUID());
    secondEvent.setEventType("SubscriptionUpdated");
    secondEvent.setStatus("PENDING");
    secondEvent.setTimestamp(System.currentTimeMillis());

    List<EventsOutbox> pendingEvents = List.of(testEventsOutbox, secondEvent);
    when(eventsOutboxRepository.findByStatus("PENDING"))
            .thenReturn(pendingEvents);

    // When
    subscriptionChangeScheduler.checkForOutboxSubscriptions();

    // Then
    verify(eventsOutboxRepository).findByStatus("PENDING");
    verify(subscriptionChangePublisher, times(2)).sendMessage(any(SubscriptionChangeEvent.class));
  }

  @Test
  void createSubscriptionChangeEvent_ShouldCreateCorrectEvent() {
    // Given
    // Use reflection to access private method for testing
    try {
      java.lang.reflect.Method method = SubscriptionChangeScheduler.class
              .getDeclaredMethod("createSubscriptionChangeEvent", EventsOutbox.class);
      method.setAccessible(true);

      // When
      SubscriptionChangeEvent result = (SubscriptionChangeEvent) method.invoke(
              subscriptionChangeScheduler, testEventsOutbox);

      // Then
      assertNotNull(result);
      assertEquals(testEventId.toString(), result.eventId());
      assertEquals("SubscriptionCreated", result.eventType());
      assertEquals(testEventsOutbox.getTimestamp(), result.timestamp());
      assertEquals(testEventsOutbox.getPayload(), result.payload());
    } catch (Exception e) {
      fail("Failed to test private method: " + e.getMessage());
    }
  }
} 