package com.example.fx.subscription.service.dto.subscription;

import com.example.fx.subscription.service.model.Subscription;

import java.util.UUID;

public record SubscriptionStatusUpdateResponse(
        UUID subscriptionId,
        String status,
        String message
) {
  public static SubscriptionStatusUpdateResponse fromSubscription(Subscription subscription) {
    return new SubscriptionStatusUpdateResponse(
            subscription.getId(),
            subscription.getStatus().name(),
            "Subscription status updated successfully"
    );
  }
} 