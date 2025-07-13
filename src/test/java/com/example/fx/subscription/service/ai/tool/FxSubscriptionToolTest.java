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
        String userId = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";
        String currencyPair = "GBP/USD";
        double thresholdValue = 1.25;
        String direction = "ABOVE";
        String notificationMethod = "email";

        Subscription createdSubscription = createTestSubscription();
        when(subscriptionService.createSubscription(any(), eq(UUID.fromString(userId))))
                .thenReturn(createdSubscription);

        // When
        String result = fxSubscriptionTool.createSubscriptionTool(userId, currencyPair, thresholdValue, direction, notificationMethod);

        // Then
        assertThat(result)
                .contains("Subscription for GBP/USD at threshold 1.25 with direction ABOVE created successfully")
                .contains("Your subscription ID is: " + createdSubscription.getId())
                .contains("Notifications via [email, sms]");

        verify(subscriptionService).createSubscription(any(), eq(UUID.fromString(userId)));
    }

    @Test
    void createSubscriptionTool_WithInvalidUserId_ShouldThrowException() {
        // Given
        String invalidUserId = "invalid-uuid";
        String currencyPair = "GBP/USD";
        double thresholdValue = 1.25;
        String direction = "ABOVE";
        String notificationMethod = "email";

        // When & Then
        assertThatThrownBy(() -> 
            fxSubscriptionTool.createSubscriptionTool(invalidUserId, currencyPair, thresholdValue, direction, notificationMethod))
                .isInstanceOf(IllegalArgumentException.class);

        verify(subscriptionService, never()).createSubscription(any(), any());
    }

    @Test
    void updateSubscriptionTool_WithAllParameters_ShouldUpdateSubscription() {
        // Given
        String subscriptionId = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";
        double newThresholdValue = 1.30;
        String direction = "BELOW";
        String newNotificationMethod = "sms";

        Subscription existingSubscription = createTestSubscription();
        Subscription updatedSubscription = createTestSubscription();
        updatedSubscription.setThreshold(BigDecimal.valueOf(1.30));
        updatedSubscription.setDirection(ThresholdDirection.BELOW);
        updatedSubscription.setNotificationsChannels(List.of("sms"));

        when(subscriptionService.findSubscriptionEntityById(subscriptionId)).thenReturn(Optional.of(existingSubscription));
        when(subscriptionService.updateSubscriptionById(any(), any())).thenReturn(updatedSubscription);

        // When
        String result = fxSubscriptionTool.updateSubscriptionTool(subscriptionId, newThresholdValue, direction, newNotificationMethod);

        // Then
        assertThat(result)
                .contains("Subscription: " + subscriptionId + " updated successfully")
                .contains("New threshold: 1.3")
                .contains("New direction: BELOW")
                .contains("New notification methods: [sms]");

        verify(subscriptionService).findSubscriptionEntityById(subscriptionId);
        verify(subscriptionService).updateSubscriptionById(any(), any());
    }

    @Test
    void updateSubscriptionTool_WithPartialParameters_ShouldUpdateOnlyProvidedFields() {
        // Given
        String subscriptionId = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";
        double newThresholdValue = 1.30;
        // direction and notificationMethod are null

        Subscription existingSubscription = createTestSubscription();
        Subscription updatedSubscription = createTestSubscription();
        updatedSubscription.setThreshold(BigDecimal.valueOf(1.30));

        when(subscriptionService.findSubscriptionEntityById(subscriptionId)).thenReturn(Optional.of(existingSubscription));
        when(subscriptionService.updateSubscriptionById(any(), any())).thenReturn(updatedSubscription);

        // When
        String result = fxSubscriptionTool.updateSubscriptionTool(subscriptionId, newThresholdValue, null, null);

        // Then
        assertThat(result)
                .contains("Subscription: " + subscriptionId + " updated successfully")
                .contains("New threshold: 1.3")
                .contains("New direction: ABOVE")
                .contains("New notification methods: [email, sms]");

        verify(subscriptionService).findSubscriptionEntityById(subscriptionId);
        verify(subscriptionService).updateSubscriptionById(any(), any());
    }

    @Test
    void updateSubscriptionTool_WithNonExistentSubscription_ShouldReturnNotFoundMessage() {
        // Given
        String subscriptionId = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";
        double newThresholdValue = 1.30;
        String direction = "BELOW";
        String newNotificationMethod = "sms";

        when(subscriptionService.findSubscriptionEntityById(subscriptionId)).thenReturn(Optional.empty());

        // When
        String result = fxSubscriptionTool.updateSubscriptionTool(subscriptionId, newThresholdValue, direction, newNotificationMethod);

        // Then
        assertThat(result).isEqualTo("Subscription " + subscriptionId + " not found or no valid updates provided.");

        verify(subscriptionService).findSubscriptionEntityById(subscriptionId);
        verify(subscriptionService, never()).updateSubscriptionById(any(), any());
    }

    @Test
    void deleteSubscriptionTool_WithValidSubscriptionId_ShouldDeleteSubscription() {
        // Given
        String subscriptionId = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";

        doNothing().when(subscriptionService).deleteSubscriptionById(subscriptionId);

        // When
        String result = fxSubscriptionTool.deleteSubscriptionTool(subscriptionId);

        // Then
        assertThat(result).isEqualTo("Subscription " + subscriptionId + " deleted successfully.");

        verify(subscriptionService).deleteSubscriptionById(subscriptionId);
    }

    @Test
    void getFxSubscriptionsForUserTool_WithSubscriptions_ShouldReturnFormattedList() {
        // Given
        String userId = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";
        List<SubscriptionResponse> subscriptions = List.of(
                new SubscriptionResponse(
                        UUID.fromString("6f0ad90b-8b07-4342-a918-6866ce3b72d3"),
                        null,
                        "GBP/USD",
                        BigDecimal.valueOf(1.25),
                        ThresholdDirection.ABOVE,
                        List.of("email", "sms"),
                        SubscriptionStatus.ACTIVE,
                        null,
                        null),
                new SubscriptionResponse(
                        UUID.fromString("af6ce3bc-39ad-44e4-a6a8-8314b52f8fa2"),
                        null,
                        "EUR/USD",
                        BigDecimal.valueOf(1.10),
                        ThresholdDirection.BELOW,
                        List.of("email"),
                        SubscriptionStatus.ACTIVE,
                        null,
                        null)
        );

        when(subscriptionService.findSubscriptionResponsesByUserId(userId)).thenReturn(subscriptions);

        // When
        String result = fxSubscriptionTool.getFxSubscriptionsForUserTool(userId);

        // Then
        assertThat(result)
                .contains("ID: 6f0ad90b-8b07-4342-a918-6866ce3b72d3, Pair: GBP/USD, Threshold: 1.25, Notify via: [email, sms]")
                .contains("ID: af6ce3bc-39ad-44e4-a6a8-8314b52f8fa2, Pair: EUR/USD, Threshold: 1.10, Notify via: [email]");

        verify(subscriptionService).findSubscriptionResponsesByUserId(userId);
    }

    @Test
    void getFxSubscriptionsForUserTool_WithNoSubscriptions_ShouldReturnEmptyMessage() {
        // Given
        String userId = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";

        when(subscriptionService.findSubscriptionResponsesByUserId(userId)).thenReturn(List.of());

        // When
        String result = fxSubscriptionTool.getFxSubscriptionsForUserTool(userId);

        // Then
        assertThat(result).isEqualTo("No active subscriptions found for the user " + userId + ".");

        verify(subscriptionService).findSubscriptionResponsesByUserId(userId);
    }

    // Helper method
    private Subscription createTestSubscription() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.fromString("6f0ad90b-8b07-4342-a918-6866ce3b72d3"));
        subscription.setCurrencyPair("GBP/USD");
        subscription.setThreshold(BigDecimal.valueOf(1.25));
        subscription.setDirection(ThresholdDirection.ABOVE);
        subscription.setNotificationsChannels(List.of("email", "sms"));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }
} 