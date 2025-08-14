package com.example.fx.subscription.service.dto.subscription;

public record SubscriptionUpdateResponse(
        String subscriptionId,
        String message,
        SubscriptionResponse subscription
) {}
