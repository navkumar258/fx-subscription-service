package com.example.fx.subscription.service.model;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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

    private SubscriptionChangeEvent createTestEvent(UUID subscriptionId, String eventType) {
        return new SubscriptionChangeEvent(
                "test-event-id",
                Instant.parse("2025-01-01T10:15:30.00Z").toEpochMilli(),
                eventType,
                new SubscriptionResponse(
                        subscriptionId == null ? UUID.randomUUID() : subscriptionId,
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