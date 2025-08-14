package com.example.fx.subscription.service.dto.subscription;

public record SubscriptionCreateResponse(
        String subscriptionId,
        String message,
        SubscriptionResponse subscription
) {}
