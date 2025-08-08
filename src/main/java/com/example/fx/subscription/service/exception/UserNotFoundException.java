package com.example.fx.subscription.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1006298327459706365L;

  private final String userId;
  private final String errorCode;

  public UserNotFoundException(String message) {
    super(message);
    this.userId = null;
    this.errorCode = "USER_NOT_FOUND";
  }

  public UserNotFoundException(String message, String userId) {
    super(message);
    this.userId = userId;
    this.errorCode = "USER_NOT_FOUND";
  }

  public UserNotFoundException(String message, String userId, String errorCode) {
    super(message);
    this.userId = userId;
    this.errorCode = errorCode;
  }

  public String getUserId() {
    return userId;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
