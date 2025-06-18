package com.example.fx.subscription.service.dto;

import java.math.BigDecimal;
import java.util.List;

public class SubscriptionUpdateRequest {

  private String userId;

  private String currencyPair;

  private BigDecimal threshold;

  private String direction;

  private String status;

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<String> getNotificationChannels() {
    return notificationChannels;
  }

  public void setNotificationChannels(List<String> notificationChannels) {
    this.notificationChannels = notificationChannels;
  }
}