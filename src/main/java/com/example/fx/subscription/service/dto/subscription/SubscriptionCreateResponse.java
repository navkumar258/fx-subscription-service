package com.example.fx.subscription.service.dto.subscription;

import java.util.UUID;

public record SubscriptionCreateResponse(
        UUID subscriptionId,
        String message,
        SubscriptionResponse subscription
) {
  public static SubscriptionCreateResponse fromSubscription(com.example.fx.subscription.service.model.Subscription subscription) {
    return new SubscriptionCreateResponse(
            subscription.getId(),
            "Subscription created successfully",
            SubscriptionResponse.fromSubscription(subscription)
    );
  }
}
