package com.example.fx.subscription.service.model;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;

public record SubscriptionChangeEvent(
        String eventId,
        long timestamp,
        String eventType,
        SubscriptionResponse payload
) {}
