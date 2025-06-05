package com.example.fx.subscription.service.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription implements Serializable {

  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private FXUser user;

  private String currencyPair;

  private BigDecimal threshold;

  private ThresholdDirection direction;

  private List<String> notificationsChannels;

  private SubscriptionStatus status;

  @CreationTimestamp
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;

  Subscription() {}

  Subscription(String currencyPair,
               BigDecimal threshold,
               ThresholdDirection direction,
               List<String> notificationsChannels,
               SubscriptionStatus status) {
    this.currencyPair = currencyPair;
    this.threshold = threshold;
    this.direction = direction;
    this.notificationsChannels = notificationsChannels;
    this.status = status;
  }

  public UUID getId() {
    return id;
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

  public List<String> getNotificationsChannels() {
    return notificationsChannels;
  }

  public void setNotificationsChannels(List<String> notificationsChannels) {
    this.notificationsChannels = notificationsChannels;
  }

  public ThresholdDirection getDirection() {
    return direction;
  }

  public void setDirection(ThresholdDirection direction) {
    this.direction = direction;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public void setStatus(SubscriptionStatus status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Subscription that)) return false;
    return Objects.equals(id, that.id)
            && Objects.equals(user, that.user)
            && Objects.equals(getCurrencyPair(), that.getCurrencyPair())
            && Objects.equals(getThreshold(), that.getThreshold())
            && getDirection() == that.getDirection()
            && Objects.equals(getNotificationsChannels(), that.getNotificationsChannels())
            && getStatus() == that.getStatus();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, user, getCurrencyPair(), getThreshold(), getDirection(), getNotificationsChannels(), getStatus());
  }

  @Override
  public String toString() {
    return "Subscription{" +
            "id=" + id +
            ", User=" + user +
            ", currencyPair='" + currencyPair + '\'' +
            ", threshold=" + threshold +
            ", direction=" + direction +
            ", notificationsChannels=" + notificationsChannels +
            ", status=" + status +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            '}';
  }
}
