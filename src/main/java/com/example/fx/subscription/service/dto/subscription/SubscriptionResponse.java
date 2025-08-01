package com.example.fx.subscription.service.dto.subscription;

import com.example.fx.subscription.service.dto.user.UserSummaryResponse;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UserSummaryResponse user,
        String currencyPair,
        BigDecimal threshold,
        ThresholdDirection direction,
        List<String> notificationsChannels,
        SubscriptionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
  public static SubscriptionResponse fromSubscription(com.example.fx.subscription.service.model.Subscription subscription) {
    return new SubscriptionResponse(
            subscription.getId(),
            subscription.getUser() != null ? UserSummaryResponse.fromFxUser(subscription.getUser()) : null,
            subscription.getCurrencyPair(),
            subscription.getThreshold(),
            subscription.getDirection(),
            subscription.getNotificationsChannels(),
            subscription.getStatus(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt()
    );
  }
}