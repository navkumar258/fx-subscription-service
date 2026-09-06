package com.example.fx.subscription.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSubscriptionUpdateException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -231087696633273842L;

	public InvalidSubscriptionUpdateException(String message) {
		super(message);
	}
}
