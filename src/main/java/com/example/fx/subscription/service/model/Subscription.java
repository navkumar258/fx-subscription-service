package com.example.fx.subscription.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
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
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private FxUser user;

  private String currencyPair;

  private BigDecimal threshold;

  private ThresholdDirection direction;

  private List<String> notificationsChannels;

  private SubscriptionStatus status;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(insertable = false)
  private Instant updatedAt;

  public Subscription() {}

  public Subscription(UUID id,
                      String currencyPair,
                      BigDecimal threshold,
                      ThresholdDirection direction,
                      List<String> notificationsChannels,
                      SubscriptionStatus status) {
    this.id = id;
    this.currencyPair = currencyPair;
    this.threshold = threshold;
    this.direction = direction;
    this.notificationsChannels = notificationsChannels;
    this.status = status;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
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

  public FxUser getUser() {
    return user;
  }

  public void setUser(FxUser user) {
    this.user = user;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Subscription that)) return false;
    return Objects.equals(id, that.id)
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
