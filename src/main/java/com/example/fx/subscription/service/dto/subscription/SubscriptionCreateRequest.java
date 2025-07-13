package com.example.fx.subscription.service.dto.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record SubscriptionCreateRequest(
        @NotBlank(message = "Currency pair is mandatory and should not be blank")
        String currencyPair,
        @Positive(message = "Threshold is mandatory and should be a positive number")
        BigDecimal threshold,
        @NotBlank(message = "Direction is mandatory and should not be blank")
        String direction,
        @NotEmpty(message = "Notification channels are mandatory and should contain at least 1 item")
        List<String> notificationChannels) {}