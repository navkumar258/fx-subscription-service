package com.example.fx.subscription.service.dto.user;

import com.example.fx.subscription.service.model.FxUser;

import java.util.UUID;

public record UserUpdateResponse(
        UUID userId,
        String message,
        UserDetailResponse user
) {
  public static UserUpdateResponse fromFxUser(FxUser fxUser) {
    return new UserUpdateResponse(
            fxUser.getId(),
            "User updated successfully",
            UserDetailResponse.fromFxUser(fxUser)
    );
  }
}