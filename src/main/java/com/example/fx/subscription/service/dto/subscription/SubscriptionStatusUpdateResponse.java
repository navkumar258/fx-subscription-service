package com.example.fx.subscription.service.dto.subscription;

import com.example.fx.subscription.service.model.Subscription;

public record SubscriptionStatusUpdateResponse(
        String subscriptionId,
        String status,
        String message
) {
  public static SubscriptionStatusUpdateResponse fromSubscription(Subscription subscription) {
    return new SubscriptionStatusUpdateResponse(
            subscription.getId().toString(),
            subscription.getStatus().name(),
            "Subscription status updated successfully"
    );
  }
} 