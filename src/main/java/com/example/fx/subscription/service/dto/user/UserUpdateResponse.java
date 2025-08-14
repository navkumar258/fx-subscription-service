package com.example.fx.subscription.service.dto.user;

import com.example.fx.subscription.service.model.FxUser;

public record UserUpdateResponse(
        String userId,
        String message,
        UserDetailResponse user
) {
  public static UserUpdateResponse fromFxUser(FxUser fxUser) {
    return new UserUpdateResponse(
            fxUser.getId().toString(),
            "User updated successfully",
            UserDetailResponse.fromFxUser(fxUser)
    );
  }
}