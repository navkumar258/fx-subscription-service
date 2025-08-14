package com.example.fx.subscription.service.model;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionChangeEventTest {

  @Test
  void equals_WithSameValues_ShouldReturnTrue() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    SubscriptionChangeEvent event1 = createTestEvent(subscriptionId, "SubscriptionCreated");
    SubscriptionChangeEvent event2 = createTestEvent(subscriptionId, "SubscriptionCreated");

    // When & Then
    assertEquals(event1, event2);
    assertEquals(event1.hashCode(), event2.hashCode());
  }

  @Test
  void equals_WithDifferentValues_ShouldReturnFalse() {
    // Given
    SubscriptionChangeEvent event1 = createTestEvent(null, "SubscriptionCreated");
    SubscriptionChangeEvent event2 = createTestEvent(null, "Different-Type");

    // When & Then
    assertNotEquals(event1, event2);
  }

  @Test
  void toString_ShouldReturnFormattedString() {
    // Given
    SubscriptionChangeEvent event = createTestEvent(null, "SubscriptionCreated");

    // When
    String result = event.toString();

    // Then
    assertTrue(result.contains("SubscriptionChangeEvent"));
    assertTrue(result.contains("eventId="));
    assertTrue(result.contains("timestamp="));
    assertTrue(result.contains("eventType="));
    assertTrue(result.contains("payload="));
  }

  @Test
  void constructor_ShouldSetAllFieldsCorrectly() {
    // Given
    String eventId = "test-event-123";
    long timestamp = 1234567890L;
    String eventType = "SUBSCRIPTION_UPDATED";
    SubscriptionResponse payload = createTestEvent(null, eventType).payload();

    // When
    SubscriptionChangeEvent event = new SubscriptionChangeEvent(eventId, timestamp, eventType, payload);

    // Then
    assertEquals(eventId, event.eventId());
    assertEquals(timestamp, event.timestamp());
    assertEquals(eventType, event.eventType());
    assertEquals(payload, event.payload());
  }

  @Test
  void constructor_ShouldHandleNullValues() {
    // When
    SubscriptionChangeEvent event = new SubscriptionChangeEvent(null, 0L, null, null);

    // Then
    assertNull(event.eventId());
    assertEquals(0L, event.timestamp());
    assertNull(event.eventType());
    assertNull(event.payload());
  }

  @Test
  void equals_WithNullEventId_ShouldReturnFalse() {
    // Given
    SubscriptionChangeEvent event1 = createTestEvent(null, "SubscriptionCreated");
    SubscriptionChangeEvent event2 = new SubscriptionChangeEvent(
            null,
            event1.timestamp(),
            event1.eventType(),
            event1.payload()
    );

    // When & Then
    assertNotEquals(event1, event2);
  }

  private SubscriptionChangeEvent createTestEvent(UUID subscriptionId, String eventType) {
    return new SubscriptionChangeEvent(
            "test-event-id",
            System.currentTimeMillis(),
            eventType,
            new SubscriptionResponse(
                    subscriptionId == null ? UUID.randomUUID().toString() : subscriptionId.toString(),
                    null,
                    "GBP/USD",
                    BigDecimal.valueOf(1.25),
                    ThresholdDirection.ABOVE,
                    List.of("email"),
                    SubscriptionStatus.ACTIVE,
                    null,
                    null
            )
    );
  }
} 