package com.example.fx.subscription.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {

  private final String email;
  private final String errorCode;

  public UserAlreadyExistsException(String message) {
    super(message);
    this.email = null;
    this.errorCode = "USER_ALREADY_EXISTS";
  }

  public UserAlreadyExistsException(String message, String email) {
    super(message);
    this.email = email;
    this.errorCode = "USER_ALREADY_EXISTS";
  }

  public UserAlreadyExistsException(String message, String email, String errorCode) {
    super(message);
    this.email = email;
    this.errorCode = errorCode;
  }

  public String getEmail() {
    return email;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
