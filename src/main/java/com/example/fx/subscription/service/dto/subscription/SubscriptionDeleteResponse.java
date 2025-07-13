package com.example.fx.subscription.service.dto.subscription;

import java.util.UUID;

public record SubscriptionDeleteResponse(
        UUID subscriptionId,
        String message
) {
  public static SubscriptionDeleteResponse fromSubscriptionId(UUID subscriptionId) {
    return new SubscriptionDeleteResponse(
            subscriptionId,
            "Subscription deleted successfully"
    );
  }
}
