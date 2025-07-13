package com.example.fx.subscription.service.dto.user;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;

import java.util.List;
import java.util.UUID;

public record UserSubscriptionsResponse(
        UUID userId,
        List<SubscriptionResponse> subscriptions,
        int totalCount
) {} 