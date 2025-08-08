package com.example.fx.subscription.service.ai.tool;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FxSubscriptionToolTest {

  private static final String USER_ID = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";
  private static final String SUBSCRIPTION_ID = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";
  private static final String GBP_USD = "GBP/USD";
  private static final String EMAIL = "email";
  private static final String SMS = "sms";

  @Mock
  private SubscriptionsService subscriptionService;

  private FxSubscriptionTool fxSubscriptionTool;

  @BeforeEach
  void setUp() {
    fxSubscriptionTool = new FxSubscriptionTool(subscriptionService);
  }

  @Test
  void createSubscriptionTool_WithValidParameters_ShouldCreateSubscription() {
    // Given
    double thresholdValue = 1.25;
    String direction = "ABOVE";

    Subscription createdSubscription = createTestSubscription();
    when(subscriptionService.createSubscription(any(), eq(UUID.fromString(USER_ID))))
            .thenReturn(createdSubscription);

    // When
    String result = fxSubscriptionTool.createSubscriptionTool(USER_ID, GBP_USD, thresholdValue, direction, EMAIL);

    // Then
    assertThat(result)
            .contains("Subscription for GBP/USD at threshold 1.25 with direction ABOVE created successfully")
            .contains("Your subscription ID is: " + createdSubscription.getId())
            .contains("Notifications via [email, sms]");

    verify(subscriptionService).createSubscription(any(), eq(UUID.fromString(USER_ID)));
  }

  @Test
  void createSubscriptionTool_WithInvalidUserId_ShouldThrowException() {
    // Given
    String invalidUserId = "invalid-uuid";
    double thresholdValue = 1.25;
    String direction = "ABOVE";

    // When & Then
    assertThatThrownBy(() ->
            fxSubscriptionTool.createSubscriptionTool(invalidUserId, GBP_USD, thresholdValue, direction, EMAIL))
            .isInstanceOf(IllegalArgumentException.class);

    verify(subscriptionService, never()).createSubscription(any(), any());
  }

  @Test
  void updateSubscriptionTool_WithAllParameters_ShouldUpdateSubscription() {
    // Given
    double newThresholdValue = 1.30;
    String direction = "BELOW";

    Subscription existingSubscription = createTestSubscription();
    Subscription updatedSubscription = createTestSubscription();
    updatedSubscription.setThreshold(BigDecimal.valueOf(1.30));
    updatedSubscription.setDirection(ThresholdDirection.BELOW);
    updatedSubscription.setNotificationsChannels(List.of("sms"));

    when(subscriptionService.findSubscriptionEntityById(SUBSCRIPTION_ID)).thenReturn(Optional.of(existingSubscription));
    when(subscriptionService.updateSubscriptionById(any(), any())).thenReturn(updatedSubscription);

    // When
    String result = fxSubscriptionTool.updateSubscriptionTool(SUBSCRIPTION_ID, newThresholdValue, direction, SMS);

    // Then
    assertThat(result)
            .contains("Subscription: " + SUBSCRIPTION_ID + " updated successfully")
            .contains("New threshold: 1.3")
            .contains("New direction: BELOW")
            .contains("New notification methods: [sms]");

    verify(subscriptionService).findSubscriptionEntityById(SUBSCRIPTION_ID);
    verify(subscriptionService).updateSubscriptionById(any(), any());
  }

  @Test
  void updateSubscriptionTool_WithPartialParameters_ShouldUpdateOnlyProvidedFields() {
    // Given
    double newThresholdValue = 1.30;
    // direction and notificationMethod are null

    Subscription existingSubscription = createTestSubscription();
    Subscription updatedSubscription = createTestSubscription();
    updatedSubscription.setThreshold(BigDecimal.valueOf(1.30));

    when(subscriptionService.findSubscriptionEntityById(SUBSCRIPTION_ID)).thenReturn(Optional.of(existingSubscription));
    when(subscriptionService.updateSubscriptionById(any(), any())).thenReturn(updatedSubscription);

    // When
    String result = fxSubscriptionTool.updateSubscriptionTool(SUBSCRIPTION_ID, newThresholdValue, null, null);

    // Then
    assertThat(result)
            .contains("Subscription: " + SUBSCRIPTION_ID + " updated successfully")
            .contains("New threshold: 1.3")
            .contains("New direction: ABOVE")
            .contains("New notification methods: [email, sms]");

    verify(subscriptionService).findSubscriptionEntityById(SUBSCRIPTION_ID);
    verify(subscriptionService).updateSubscriptionById(any(), any());
  }

  @Test
  void updateSubscriptionTool_WithNonExistentSubscription_ShouldReturnNotFoundMessage() {
    // Given
    double newThresholdValue = 1.30;
    String direction = "BELOW";

    when(subscriptionService.findSubscriptionEntityById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

    // When
    String result = fxSubscriptionTool.updateSubscriptionTool(SUBSCRIPTION_ID, newThresholdValue, direction, SMS);

    // Then
    assertThat(result).isEqualTo("Subscription " + SUBSCRIPTION_ID + " not found or no valid updates provided.");

    verify(subscriptionService).findSubscriptionEntityById(SUBSCRIPTION_ID);
    verify(subscriptionService, never()).updateSubscriptionById(any(), any());
  }

  @Test
  void deleteSubscriptionTool_WithValidSubscriptionId_ShouldDeleteSubscription() {
    doNothing().when(subscriptionService).deleteSubscriptionById(SUBSCRIPTION_ID);

    // When
    String result = fxSubscriptionTool.deleteSubscriptionTool(SUBSCRIPTION_ID);

    // Then
    assertThat(result).isEqualTo("Subscription " + SUBSCRIPTION_ID + " deleted successfully.");

    verify(subscriptionService).deleteSubscriptionById(SUBSCRIPTION_ID);
  }

  @Test
  void getFxSubscriptionsForUserTool_WithSubscriptions_ShouldReturnFormattedList() {
    List<SubscriptionResponse> subscriptions = List.of(
            new SubscriptionResponse(
                    UUID.fromString("6f0ad90b-8b07-4342-a918-6866ce3b72d3"),
                    null,
                    GBP_USD,
                    BigDecimal.valueOf(1.25),
                    ThresholdDirection.ABOVE,
                    List.of(EMAIL, SMS),
                    SubscriptionStatus.ACTIVE,
                    null,
                    null),
            new SubscriptionResponse(
                    UUID.fromString("af6ce3bc-39ad-44e4-a6a8-8314b52f8fa2"),
                    null,
                    "EUR/USD",
                    BigDecimal.valueOf(1.10),
                    ThresholdDirection.BELOW,
                    List.of(EMAIL),
                    SubscriptionStatus.ACTIVE,
                    null,
                    null)
    );

    when(subscriptionService.findSubscriptionResponsesByUserId(USER_ID)).thenReturn(subscriptions);

    // When
    String result = fxSubscriptionTool.getFxSubscriptionsForUserTool(USER_ID);

    // Then
    assertThat(result)
            .contains("ID: 6f0ad90b-8b07-4342-a918-6866ce3b72d3, Pair: GBP/USD, Threshold: 1.25, Notify via: [email, sms]")
            .contains("ID: af6ce3bc-39ad-44e4-a6a8-8314b52f8fa2, Pair: EUR/USD, Threshold: 1.10, Notify via: [email]");

    verify(subscriptionService).findSubscriptionResponsesByUserId(USER_ID);
  }

  @Test
  void getFxSubscriptionsForUserTool_WithNoSubscriptions_ShouldReturnEmptyMessage() {
    when(subscriptionService.findSubscriptionResponsesByUserId(USER_ID)).thenReturn(List.of());

    // When
    String result = fxSubscriptionTool.getFxSubscriptionsForUserTool(USER_ID);

    // Then
    assertThat(result).isEqualTo("No active subscriptions found for the user " + USER_ID + ".");

    verify(subscriptionService).findSubscriptionResponsesByUserId(USER_ID);
  }

  // Helper method
  private Subscription createTestSubscription() {
    Subscription subscription = new Subscription();
    subscription.setId(UUID.fromString(SUBSCRIPTION_ID));
    subscription.setCurrencyPair(GBP_USD);
    subscription.setThreshold(BigDecimal.valueOf(1.25));
    subscription.setDirection(ThresholdDirection.ABOVE);
    subscription.setNotificationsChannels(List.of(EMAIL, SMS));
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    return subscription;
  }
} 