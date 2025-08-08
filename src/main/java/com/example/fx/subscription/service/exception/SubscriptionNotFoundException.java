package com.example.fx.subscription.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SubscriptionNotFoundException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1488659236727262493L;

  private final String subscriptionId;
  private final String errorCode;

  public SubscriptionNotFoundException(String message) {
    super(message);
    this.subscriptionId = null;
    this.errorCode = "SUBSCRIPTION_NOT_FOUND";
  }

  public SubscriptionNotFoundException(String message, String subscriptionId) {
    super(message);
    this.subscriptionId = subscriptionId;
    this.errorCode = "SUBSCRIPTION_NOT_FOUND";
  }

  public SubscriptionNotFoundException(String message, String subscriptionId, String errorCode) {
    super(message);
    this.subscriptionId = subscriptionId;
    this.errorCode = errorCode;
  }

  public String getSubscriptionId() {
    return subscriptionId;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
