package com.example.fx.subscription.service.exception;

import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@org.springframework.web.bind.annotation.ControllerAdvice
public class ControllerAdvice {

  private static final Logger LOGGER = LoggerFactory.getLogger(ControllerAdvice.class);
  private static final String TIMESTAMP = "timestamp";
  private static final String ERROR_CODE = "errorCode";
  private static final String ACCESS_DENIED = "Access denied";
  private static final String BAD_REQUEST = "BAD_REQUEST";

  @ExceptionHandler(SubscriptionNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleSubscriptionNotFoundException(
          SubscriptionNotFoundException e,
          WebRequest request) {
    LOGGER.warn("Subscription not found: subscriptionId={}, path={}, message={}",
            e.getSubscriptionId(), request.getDescription(false), e.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            e.getMessage()
    );

    problemDetail.setProperty(TIMESTAMP, Instant.now());
    problemDetail.setProperty(ERROR_CODE, e.getErrorCode());

    if (e.getSubscriptionId() != null) {
      problemDetail.setProperty("subscriptionId", e.getSubscriptionId());
    }

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleUserNotFoundException(
          UserNotFoundException e,
          WebRequest request) {
    LOGGER.warn("User not found: userId={}, path={}, message={}",
            e.getUserId(), request.getDescription(false), e.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            e.getMessage()
    );

    problemDetail.setProperty(TIMESTAMP, Instant.now());
    problemDetail.setProperty(ERROR_CODE, e.getErrorCode());

    if (e.getUserId() != null) {
      problemDetail.setProperty("userId", e.getUserId());
    }

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemDetail> handleNoResourceFoundException(
          NoResourceFoundException ex,
          WebRequest request
  ) {
    logException(ex.getMessage(), ex, request);

    return createProblemDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            "RESOURCE_NOT_FOUND"
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationExceptions(
          MethodArgumentNotValidException ex,
          WebRequest request) {

    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage()));

    LOGGER.warn("Validation failed: path={}, fieldErrors={}",
            request.getDescription(false), fieldErrors);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Argument validation failed"
    );

    problemDetail.setProperty(TIMESTAMP, Instant.now());
    problemDetail.setProperty(ERROR_CODE, "VALIDATION_ERROR");
    problemDetail.setProperty("fieldErrors", fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ProblemDetail> handleMissingServletRequestParameterException(
          MissingServletRequestParameterException ex,
          WebRequest request
  ) {
    logException(ex.getMessage(), ex, request);

    return createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            BAD_REQUEST
    );
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupportedException(
          HttpRequestMethodNotSupportedException ex,
          WebRequest request
  ) {
    logException(ex.getMessage(), ex, request);

    return createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            BAD_REQUEST
    );
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(
          HttpMessageNotReadableException e,
          WebRequest request
  ) {
    logException(e.getMessage(), e, request);

    return createProblemDetail(
            HttpStatus.BAD_REQUEST,
            e.getMessage(),
            BAD_REQUEST
    );
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDeniedException(
          AccessDeniedException e,
          WebRequest request) {
    logException(ACCESS_DENIED, e, request);

    return createProblemDetail(
            HttpStatus.FORBIDDEN,
            e.getMessage(),
            "ACCESS_DENIED"
    );
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuthenticationException(
          AuthenticationException e,
          WebRequest request) {
    logException("Authentication failed", e, request);

    return createProblemDetail(
            HttpStatus.UNAUTHORIZED,
            e.getMessage(),
            "AUTHENTICATION_ERROR"
    );
  }

  @ExceptionHandler(JwtException.class)
  public ResponseEntity<ProblemDetail> handleJwtException(
          JwtException e,
          WebRequest request
  ) {
    logException("JWT validation failed", e, request);

    return createProblemDetail(
            HttpStatus.UNAUTHORIZED,
            e.getMessage(),
            "JWT_VALIDATION_ERROR"
    );
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ProblemDetail> handleUserAlreadyExistsException(
          UserAlreadyExistsException e,
          WebRequest request) {
    LOGGER.warn("User already exists: email={}, path={}, message={}",
            e.getEmail(), request.getDescription(false), e.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            e.getMessage()
    );

    problemDetail.setProperty(TIMESTAMP, Instant.now());
    problemDetail.setProperty(ERROR_CODE, e.getErrorCode());

    if (e.getEmail() != null) {
      problemDetail.setProperty("email", e.getEmail());
    }

    return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(Exception e, WebRequest request) {
    LOGGER.error("Unexpected error occurred: path={}, message={}",
            request.getDescription(false), e.getMessage(), e);

    return createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            e.getMessage(),
            "INTERNAL_SERVER_ERROR"
    );
  }

  private void logException(String message, Exception e, WebRequest request) {
    LOGGER.warn("{}: path={}, message={}", message, request.getDescription(false), e.getMessage());
  }

  private ResponseEntity<ProblemDetail> createProblemDetail(
          HttpStatus status,
          String detail,
          String errorCode) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);

    problemDetail.setProperty(TIMESTAMP, Instant.now());
    problemDetail.setProperty(ERROR_CODE, errorCode);

    return ResponseEntity.status(status).body(problemDetail);
  }
}
