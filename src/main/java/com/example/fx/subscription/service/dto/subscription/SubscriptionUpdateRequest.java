package com.example.fx.subscription.service.dto.subscription;

import jakarta.validation.constraints.Positive;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

public record SubscriptionUpdateRequest(
		String currencyPair,
		@Positive(message = "Threshold must be a positive number")
		BigDecimal threshold,
		String direction,
		String status,
		List<String> notificationChannels) {
	public boolean isEmpty() {
		return !StringUtils.hasText(currencyPair)
				&& threshold == null
				&& !StringUtils.hasText(direction)
				&& !StringUtils.hasText(status)
				&& (notificationChannels == null || notificationChannels.isEmpty());
	}
}