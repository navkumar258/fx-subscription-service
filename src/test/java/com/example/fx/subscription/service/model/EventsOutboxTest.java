package com.example.fx.subscription.service.model;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EventsOutboxTest {

  private EventsOutbox eventsOutbox;
  private UUID testId;
  private String testStatus;

  @Mock
  private SubscriptionResponse mockPayload;

  @BeforeEach
  void setUp() {
    testId = UUID.randomUUID();
    testStatus = "PENDING";
    eventsOutbox = new EventsOutbox();
  }

  @Test
  void constructor_WithIdAndStatus_ShouldCreateEventsOutbox() {
    // When
    EventsOutbox constructed = new EventsOutbox(testId, testStatus);

    // Then
    assertEquals(testId, constructed.getId());
    assertEquals(testStatus, constructed.getStatus());
  }

  @Test
  void constructor_Default_ShouldCreateEmptyEventsOutbox() {
    // When
    EventsOutbox constructed = new EventsOutbox();

    // Then
    assertNull(constructed.getId());
    assertNull(constructed.getAggregateType());
    assertNull(constructed.getAggregateId());
    assertNull(constructed.getEventType());
    assertNull(constructed.getPayload());
    assertNull(constructed.getStatus());
    assertEquals(0L, constructed.getTimestamp());
  }

  @Test
  void equals_WhenSameValues_ShouldReturnTrue() {
    // Given
    EventsOutbox other = new EventsOutbox();
    eventsOutbox.setId(testId);
    eventsOutbox.setAggregateType("Subscription");
    eventsOutbox.setAggregateId(UUID.randomUUID());
    eventsOutbox.setEventType("SUBSCRIPTION_CREATED");
    eventsOutbox.setPayload(mockPayload);
    eventsOutbox.setStatus(testStatus);
    eventsOutbox.setTimestamp(123456789L);

    other.setId(testId);
    other.setAggregateType("Subscription");
    other.setAggregateId(eventsOutbox.getAggregateId());
    other.setEventType("SUBSCRIPTION_CREATED");
    other.setPayload(mockPayload);
    other.setStatus(testStatus);
    other.setTimestamp(123456789L);

    // Then
    assertEquals(eventsOutbox, other);
  }

  @Test
  void equals_WhenDifferentValues_ShouldReturnFalse() {
    // Given
    EventsOutbox other = new EventsOutbox();
    eventsOutbox.setId(testId);
    other.setId(UUID.randomUUID());

    // Then
    assertNotEquals(eventsOutbox, other);
  }

  @Test
  void toString_ShouldContainAllFields() {
    // Given
    eventsOutbox.setId(testId);
    eventsOutbox.setAggregateType("Subscription");
    eventsOutbox.setAggregateId(UUID.randomUUID());
    eventsOutbox.setEventType("SUBSCRIPTION_CREATED");
    eventsOutbox.setStatus(testStatus);
    eventsOutbox.setTimestamp(123456789L);

    // When
    String result = eventsOutbox.toString();

    // Then
    assertTrue(result.contains(testId.toString()));
    assertTrue(result.contains("Subscription"));
    assertTrue(result.contains("SUBSCRIPTION_CREATED"));
    assertTrue(result.contains(testStatus));
    assertTrue(result.contains("123456789"));
  }

  @Test
  void hashCode_WhenSameValues_ShouldReturnSameHashCode() {
    // Given
    EventsOutbox other = new EventsOutbox();
    eventsOutbox.setId(testId);
    eventsOutbox.setAggregateType("Subscription");
    eventsOutbox.setAggregateId(UUID.randomUUID());
    eventsOutbox.setEventType("SUBSCRIPTION_CREATED");
    eventsOutbox.setPayload(mockPayload);
    eventsOutbox.setStatus(testStatus);
    eventsOutbox.setTimestamp(123456789L);

    other.setId(testId);
    other.setAggregateType("Subscription");
    other.setAggregateId(eventsOutbox.getAggregateId());
    other.setEventType("SUBSCRIPTION_CREATED");
    other.setPayload(mockPayload);
    other.setStatus(testStatus);
    other.setTimestamp(123456789L);

    // When & Then
    assertEquals(eventsOutbox.hashCode(), other.hashCode());
  }

  @Test
  void hashCode_WhenDifferentValues_ShouldReturnDifferentHashCode() {
    // Given
    EventsOutbox other = new EventsOutbox();
    eventsOutbox.setId(testId);
    other.setId(UUID.randomUUID());

    // When & Then
    assertNotEquals(eventsOutbox.hashCode(), other.hashCode());
  }

  @Test
  void toString_WithNullValues_ShouldHandleNullsGracefully() {
    // Given
    EventsOutbox outboxWithNulls = new EventsOutbox();
    outboxWithNulls.setId(testId);
    // Leave other fields as null

    // When
    String result = outboxWithNulls.toString();

    // Then
    assertTrue(result.contains("id=" + testId));
    assertTrue(result.contains("aggregateType='null'"));
    assertTrue(result.contains("aggregateId=null"));
    assertTrue(result.contains("eventType='null'"));
    assertTrue(result.contains("payload=null"));
    assertTrue(result.contains("status=null"));
    assertTrue(result.contains("timestamp=0"));
  }
}
