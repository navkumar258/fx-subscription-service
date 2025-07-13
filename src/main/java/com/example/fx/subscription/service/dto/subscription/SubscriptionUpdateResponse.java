package com.example.fx.subscription.service.dto.subscription;

import java.util.UUID;

public record SubscriptionUpdateResponse(
        UUID subscriptionId,
        String message,
        SubscriptionResponse subscription
) {
  public static SubscriptionUpdateResponse fromSubscription(com.example.fx.subscription.service.model.Subscription subscription) {
    return new SubscriptionUpdateResponse(
            subscription.getId(),
            "Subscription updated successfully",
            SubscriptionResponse.fromSubscription(subscription)
    );
  }
}
