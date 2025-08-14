package com.example.fx.subscription.service.dto.user;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.model.FxUser;

import java.time.Instant;
import java.util.List;

public record UserDetailResponse(
        String id,
        String email,
        String mobile,
        boolean enabled,
        String pushDeviceToken,
        Instant createdAt,
        Instant updatedAt,
        List<SubscriptionResponse> subscriptions
) {
  public static UserDetailResponse fromFxUser(FxUser fxUser) {
    List<SubscriptionResponse> subscriptions = fxUser.getSubscriptions().stream()
            .map(SubscriptionResponse::fromSubscription)
            .toList();

    return new UserDetailResponse(
            fxUser.getId().toString(),
            fxUser.getEmail(),
            fxUser.getMobile(),
            fxUser.isEnabled(),
            fxUser.getPushDeviceToken(),
            fxUser.getCreatedAt(),
            fxUser.getUpdatedAt(),
            subscriptions
    );
  }
} 