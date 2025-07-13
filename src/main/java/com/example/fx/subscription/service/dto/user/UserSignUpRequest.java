package com.example.fx.subscription.service.dto.user;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserSignUpRequest(
        @NotNull(message = "Email is required")
        String email,

        @NotNull(message = "Password is required")
        @Pattern(regexp = "^(?=.*\\w).{8,}$", message = "Password must contain [a-z][A-Z][0-9][_] and be at least 8 characters")
        String password,

        @NotNull(message = "Mobile is required")
        String mobile,

        boolean admin) {
        public UserSignUpRequest(String email, String password, String mobile) {
                this(email, password, mobile, false);
        }
}
