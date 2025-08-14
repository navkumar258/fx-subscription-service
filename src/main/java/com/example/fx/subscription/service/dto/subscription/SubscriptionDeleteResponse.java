package com.example.fx.subscription.service.dto.subscription;

public record SubscriptionDeleteResponse(
        String userId,
        String subscriptionId,
        String message
) {
  public static SubscriptionDeleteResponse fromSubscriptionAndUserId(String userId, String subscriptionId) {
    return new SubscriptionDeleteResponse(
            userId,
            subscriptionId,
            "Subscription deleted successfully"
    );
  }
}
