package com.example.fx.subscription.service.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UserUpdateRequest(
        @Email(message = "Invalid email format")
        String email,

        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Invalid mobile number format")
        String mobile,

        String pushDeviceToken
) {
}