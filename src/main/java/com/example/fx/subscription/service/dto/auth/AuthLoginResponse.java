package com.example.fx.subscription.service.dto.auth;

import jakarta.validation.constraints.NotNull;

public record AuthLoginResponse(@NotNull String token, String message) {}
