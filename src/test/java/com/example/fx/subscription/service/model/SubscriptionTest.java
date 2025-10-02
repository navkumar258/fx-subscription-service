package com.example.fx.subscription.service.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionTest {

  @Mock
  private FxUser mockUser;

  private Subscription subscription;
  private UUID testId;

  @BeforeEach
  void setUp() {
    testId = UUID.randomUUID();
    subscription = new Subscription();
    subscription.setId(testId);
    subscription.setUser(mockUser);
    subscription.setCurrencyPair("GBP/USD");
    subscription.setThreshold(BigDecimal.valueOf(1.25));
    subscription.setDirection(ThresholdDirection.ABOVE);
    subscription.setNotificationsChannels(List.of("email", "sms"));
    subscription.setStatus(SubscriptionStatus.ACTIVE);
  }

  @Test
  void constructor_ShouldCreateSubscriptionWithParameters() {
    // Given
    List<String> channels = List.of("email", "sms");

    // When
    Subscription newSubscription = new Subscription(
            testId,
            "EUR/USD",
            BigDecimal.valueOf(1.10),
            ThresholdDirection.BELOW,
            channels,
            SubscriptionStatus.INACTIVE
    );

    // Then
    assertEquals(testId, newSubscription.getId());
    assertEquals("EUR/USD", newSubscription.getCurrencyPair());
    assertEquals(BigDecimal.valueOf(1.10), newSubscription.getThreshold());
    assertEquals(ThresholdDirection.BELOW, newSubscription.getDirection());
    assertEquals(channels, newSubscription.getNotificationsChannels());
    assertEquals(SubscriptionStatus.INACTIVE, newSubscription.getStatus());
  }

  @Test
  void getNotificationsChannels_WhenChannelsExist_ShouldReturnChannels() {
    // Given
    List<String> channels = List.of("email", "sms", "push");
    subscription.setNotificationsChannels(channels);

    // When
    List<String> result = subscription.getNotificationsChannels();

    // Then
    assertEquals(channels, result);
  }

  @Test
  void getNotificationsChannels_WhenChannelsAreNull_ShouldReturnEmptyList() {
    // Given
    subscription.setNotificationsChannels(null);

    // When
    List<String> result = subscription.getNotificationsChannels();

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void setNotificationsChannels_WhenInputIsNotNull_ShouldCreateNewList() {
    // Given
    List<String> inputChannels = List.of("email", "sms");

    // When
    subscription.setNotificationsChannels(inputChannels);

    // Then
    List<String> result = subscription.getNotificationsChannels();
    assertEquals(inputChannels, result);
    assertNotSame(inputChannels, result); // Should be a new list instance
  }

  @Test
  void setNotificationsChannels_ShouldCreateNewListInstance() {
    // Given
    List<String> originalChannels = new ArrayList<>();
    originalChannels.add("email");

    // When
    subscription.setNotificationsChannels(originalChannels);
    originalChannels.add("sms"); // Modify the original list

    // Then
    List<String> result = subscription.getNotificationsChannels();
    assertEquals(1, result.size()); // Should not be affected by original list modification
    assertEquals("email", result.getFirst());
  }

  @Test
  void equals_WhenSameValues_ShouldReturnTrue() {
    // Given
    Subscription other = new Subscription();
    other.setId(testId);
    other.setCurrencyPair("GBP/USD");
    other.setThreshold(BigDecimal.valueOf(1.25));
    other.setDirection(ThresholdDirection.ABOVE);
    other.setNotificationsChannels(List.of("email", "sms"));
    other.setStatus(SubscriptionStatus.ACTIVE);

    // When & Then
    assertEquals(subscription, other);
  }

  @Test
  void equals_WhenDifferentValues_ShouldReturnFalse() {
    // Given
    Subscription other = new Subscription();
    other.setId(UUID.randomUUID()); // Different ID

    // When & Then
    assertNotEquals(subscription, other);
  }

  @Test
  void hashCode_WhenSameValues_ShouldReturnSameHashCode() {
    // Given
    Subscription other = new Subscription();
    other.setId(testId);
    other.setCurrencyPair("GBP/USD");
    other.setThreshold(BigDecimal.valueOf(1.25));
    other.setDirection(ThresholdDirection.ABOVE);
    other.setNotificationsChannels(List.of("email", "sms"));
    other.setStatus(SubscriptionStatus.ACTIVE);

    // When & Then
    assertEquals(subscription.hashCode(), other.hashCode());
  }

  @Test
  void hashCode_WhenDifferentValues_ShouldReturnDifferentHashCode() {
    // Given
    Subscription other = new Subscription();
    other.setId(UUID.randomUUID()); // Different ID

    // When & Then
    assertNotEquals(subscription.hashCode(), other.hashCode());
  }

  @Test
  void toString_ShouldContainAllFields() {
    // When
    String result = subscription.toString();

    // Then
    assertTrue(result.contains("id=" + testId));
    assertTrue(result.contains("currencyPair='GBP/USD'"));
    assertTrue(result.contains("threshold=1.25"));
    assertTrue(result.contains("direction=ABOVE"));
    assertTrue(result.contains("notificationsChannels=[email, sms]"));
    assertTrue(result.contains("status=ACTIVE"));
  }

  @Test
  void toString_WithNullValues_ShouldHandleNullsGracefully() {
    // Given
    Subscription subscriptionWithNulls = new Subscription();
    subscriptionWithNulls.setId(testId);
    // Leave other fields as null

    // When
    String result = subscriptionWithNulls.toString();

    // Then
    assertTrue(result.contains("id=" + testId));
    assertTrue(result.contains("currencyPair='null'"));
    assertTrue(result.contains("threshold=null"));
    assertTrue(result.contains("direction=null"));
    assertTrue(result.contains("notificationsChannels=[]"));
    assertTrue(result.contains("status=null"));
  }
}
