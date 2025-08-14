package com.example.fx.subscription.service.ai.tool;

import com.example.fx.subscription.service.dto.subscription.SubscriptionDeleteResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionListResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

    SubscriptionResponse createdSubscriptionResponse = new SubscriptionResponse(
            SUBSCRIPTION_ID,
            null,
            GBP_USD,
            BigDecimal.valueOf(1.25),
            ThresholdDirection.ABOVE,
            List.of(EMAIL, SMS),
            SubscriptionStatus.ACTIVE,
            null,
            null
    );

    when(subscriptionService.createSubscription(any(), eq(UUID.fromString(USER_ID))))
            .thenReturn(createdSubscriptionResponse);

    // When
    String result = fxSubscriptionTool.createSubscriptionTool(USER_ID, GBP_USD, thresholdValue, direction, EMAIL);

    // Then
    assertThat(result)
            .contains("Subscription for GBP/USD at threshold 1.25 with direction ABOVE created successfully")
            .contains("Your subscription ID is: " + SUBSCRIPTION_ID)
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

    SubscriptionResponse updatedSubscriptionResponse = new SubscriptionResponse(
            SUBSCRIPTION_ID,
            null,
            GBP_USD,
            BigDecimal.valueOf(1.30),
            ThresholdDirection.BELOW,
            List.of(SMS),
            SubscriptionStatus.ACTIVE,
            null,
            null
    );

    when(subscriptionService.updateSubscriptionById(eq(SUBSCRIPTION_ID), any(SubscriptionUpdateRequest.class)))
            .thenReturn(updatedSubscriptionResponse);

    // When
    String result = fxSubscriptionTool.updateSubscriptionTool(
            SUBSCRIPTION_ID,
            GBP_USD,
            newThresholdValue,
            direction,
            SubscriptionStatus.ACTIVE.name(),
            List.of(SMS));

    // Then
    assertThat(result)
            .contains("Subscription: " + SUBSCRIPTION_ID + " updated successfully")
            .contains("New threshold: 1.3")
            .contains("New direction: BELOW")
            .contains("New notification methods: [sms]");

    verify(subscriptionService).updateSubscriptionById(eq(SUBSCRIPTION_ID), any(SubscriptionUpdateRequest.class));
  }

  @Test
  void updateSubscriptionTool_WithPartialParameters_ShouldUpdateOnlyProvidedFields() {
    // Given
    double newThresholdValue = 1.30;
    // direction and notificationMethod are null

    SubscriptionResponse updatedSubscriptionResponse = new SubscriptionResponse(
            SUBSCRIPTION_ID,
            null,
            GBP_USD,
            BigDecimal.valueOf(1.30),
            ThresholdDirection.ABOVE,
            List.of(EMAIL, SMS),
            SubscriptionStatus.ACTIVE,
            null,
            null
    );

    when(subscriptionService.updateSubscriptionById(eq(SUBSCRIPTION_ID), any(SubscriptionUpdateRequest.class)))
            .thenReturn(updatedSubscriptionResponse);

    // When
    String result = fxSubscriptionTool.updateSubscriptionTool(
            SUBSCRIPTION_ID,
            GBP_USD,
            newThresholdValue,
            null,
            null,
            List.of(EMAIL, SMS)
    );

    // Then
    assertThat(result)
            .contains("Subscription: " + SUBSCRIPTION_ID + " updated successfully")
            .contains("New threshold: 1.3")
            .contains("New direction: ABOVE")
            .contains("New notification methods: [email, sms]");

    verify(subscriptionService).updateSubscriptionById(eq(SUBSCRIPTION_ID), any(SubscriptionUpdateRequest.class));
  }

  @Test
  void updateSubscriptionTool_WithNonExistentSubscription_ShouldReturnNotFoundMessage() {
    // Given
    double newThresholdValue = 1.30;
    String direction = "BELOW";

    when(subscriptionService.updateSubscriptionById(eq(SUBSCRIPTION_ID), any(SubscriptionUpdateRequest.class)))
            .thenThrow(new SubscriptionNotFoundException("Subscription not found with ID: " + SUBSCRIPTION_ID, SUBSCRIPTION_ID));

    // When
    String result = fxSubscriptionTool.updateSubscriptionTool(
            SUBSCRIPTION_ID,
            GBP_USD,
            newThresholdValue,
            direction,
            SubscriptionStatus.ACTIVE.name(),
            List.of(SMS));

    // Then
    assertThat(result).isEqualTo("Subscription " + SUBSCRIPTION_ID + " not found or no valid updates provided.");

    verify(subscriptionService).updateSubscriptionById(eq(SUBSCRIPTION_ID), any(SubscriptionUpdateRequest.class));
  }

  @Test
  void deleteSubscriptionTool_WithValidSubscriptionId_ShouldDeleteSubscription() {
    // Given
    SubscriptionDeleteResponse deleteResponse = new SubscriptionDeleteResponse(
            USER_ID,
            SUBSCRIPTION_ID,
            "Subscription deleted successfully"
    );

    when(subscriptionService.deleteSubscriptionById(SUBSCRIPTION_ID)).thenReturn(deleteResponse);

    // When
    String result = fxSubscriptionTool.deleteSubscriptionTool(SUBSCRIPTION_ID);

    // Then
    assertThat(result).isEqualTo("Subscription " + SUBSCRIPTION_ID + " deleted successfully.");

    verify(subscriptionService).deleteSubscriptionById(SUBSCRIPTION_ID);
  }

  @Test
  void deleteSubscriptionTool_WithNonExistentSubscription_ShouldReturnNotFoundMessage() {
    // Given
    when(subscriptionService.deleteSubscriptionById(SUBSCRIPTION_ID))
            .thenThrow(new SubscriptionNotFoundException("Subscription not found with ID: " + SUBSCRIPTION_ID, SUBSCRIPTION_ID));

    // When
    String result = fxSubscriptionTool.deleteSubscriptionTool(SUBSCRIPTION_ID);

    // Then
    assertThat(result).isEqualTo("Subscription " + SUBSCRIPTION_ID + " not found.");

    verify(subscriptionService).deleteSubscriptionById(SUBSCRIPTION_ID);
  }

  @Test
  void getFxSubscriptionsForUserTool_WithSubscriptions_ShouldReturnFormattedList() {
    List<SubscriptionResponse> subscriptions = List.of(
            new SubscriptionResponse(
                    "6f0ad90b-8b07-4342-a918-6866ce3b72d3",
                    null,
                    GBP_USD,
                    BigDecimal.valueOf(1.25),
                    ThresholdDirection.ABOVE,
                    List.of(EMAIL, SMS),
                    SubscriptionStatus.ACTIVE,
                    null,
                    null),
            new SubscriptionResponse(
                    "af6ce3bc-39ad-44e4-a6a8-8314b52f8fa2",
                    null,
                    "EUR/USD",
                    BigDecimal.valueOf(1.10),
                    ThresholdDirection.BELOW,
                    List.of(EMAIL),
                    SubscriptionStatus.ACTIVE,
                    null,
                    null)
    );
    SubscriptionListResponse subscriptionListResponse = new SubscriptionListResponse(subscriptions, subscriptions.size());

    when(subscriptionService.findSubscriptionResponsesByUserId(USER_ID)).thenReturn(subscriptionListResponse);

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
    when(subscriptionService.findSubscriptionResponsesByUserId(USER_ID)).thenReturn(
            new SubscriptionListResponse(new ArrayList<>(), 0));

    // When
    String result = fxSubscriptionTool.getFxSubscriptionsForUserTool(USER_ID);

    // Then
    assertThat(result).isEqualTo("No active subscriptions found for the user " + USER_ID + ".");

    verify(subscriptionService).findSubscriptionResponsesByUserId(USER_ID);
  }
} 