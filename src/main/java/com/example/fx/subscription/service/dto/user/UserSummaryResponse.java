package com.example.fx.subscription.service.dto.user;

import com.example.fx.subscription.service.model.FxUser;

public record UserSummaryResponse(
        String id,
        String email,
        String mobile,
        boolean enabled,
        String createdAt,
        String updatedAt
) {
  public static UserSummaryResponse fromFxUser(FxUser fxUser) {
    return new UserSummaryResponse(
            fxUser.getId().toString(),
            fxUser.getEmail(),
            fxUser.getMobile(),
            fxUser.isEnabled(),
            fxUser.getCreatedAt().toString(),
            fxUser.getUpdatedAt() != null ? fxUser.getUpdatedAt().toString() : null
    );
  }
}
