package com.example.fx.subscription.service.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

public class SubscriptionCreateRequest {

  @NotNull(message = "User ID is mandatory")
  @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
          message = "User ID must be a valid UUID format")
  private String userId;

  @NotNull(message = "Currency pair is mandatory")
  private String currencyPair;

  @NotNull(message = "Threshold value is mandatory")
  private BigDecimal threshold;

  @NotNull(message = "Direction is mandatory")
  private String direction;

  @NotNull(message = "Notification channels are mandatory")
  private List<String> notificationChannels;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getCurrencyPair() {
    return currencyPair;
  }

  public void setCurrencyPair(String currencyPair) {
    this.currencyPair = currencyPair;
  }

  public BigDecimal getThreshold() {
    return threshold;
  }

  public void setThreshold(BigDecimal threshold) {
    this.threshold = threshold;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public List<String> getNotificationChannels() {
    return notificationChannels;
  }

  public void setNotificationChannels(List<String> notificationChannels) {
    this.notificationChannels = notificationChannels;
  }
}