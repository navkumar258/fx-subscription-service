package com.example.fx.subscription.service.dto.subscription;

import com.example.fx.subscription.service.model.Subscription;

import java.util.List;

public record SubscriptionListResponse(
        List<SubscriptionResponse> subscriptions,
        int totalCount
) {
  public static SubscriptionListResponse fromSubscriptions(List<Subscription> subscriptions) {
    List<SubscriptionResponse> subscriptionResponses = subscriptions.stream()
            .map(SubscriptionResponse::fromSubscription)
            .toList();

    return new SubscriptionListResponse(subscriptionResponses, subscriptions.size());
  }
}
