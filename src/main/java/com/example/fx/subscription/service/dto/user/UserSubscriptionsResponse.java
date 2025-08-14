package com.example.fx.subscription.service.dto.user;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;

import java.util.List;

public record UserSubscriptionsResponse(
        String userId,
        List<SubscriptionResponse> subscriptions,
        int totalCount
) {
}