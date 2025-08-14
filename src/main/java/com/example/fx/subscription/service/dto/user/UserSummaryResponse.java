package com.example.fx.subscription.service.dto.user;

import com.example.fx.subscription.service.model.FxUser;

import java.time.Instant;

public record UserSummaryResponse(
        String id,
        String email,
        String mobile,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
  public static UserSummaryResponse fromFxUser(FxUser fxUser) {
    return new UserSummaryResponse(
            fxUser.getId().toString(),
            fxUser.getEmail(),
            fxUser.getMobile(),
            fxUser.isEnabled(),
            fxUser.getCreatedAt(),
            fxUser.getUpdatedAt()
    );
  }
}
