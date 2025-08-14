package com.example.fx.subscription.service.dto.subscription;

import java.util.List;

public record SubscriptionListResponse(
        List<SubscriptionResponse> subscriptions,
        int totalCount
) {}
