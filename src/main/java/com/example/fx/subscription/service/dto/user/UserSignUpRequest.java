package com.example.fx.subscription.service.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserSignUpRequest(
        @NotBlank(message = "Email is required")
        @Email
        String email,

        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^(?=.*\\w).{8,}$", message = "Password must contain [a-z][A-Z][0-9][_] and be at least 8 characters")
        String password,

        @NotBlank(message = "Mobile is required")
        String mobile,

        Boolean admin) {
        public UserSignUpRequest {
                if (admin == null) {
                        admin = false;
                }
        }
}
