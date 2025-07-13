package com.example.fx.subscription.service.model;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;

import java.util.Objects;

public class SubscriptionChangeEvent {

  private String eventId;
  private long timestamp;
  private String eventType;
  private SubscriptionResponse payload;

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public SubscriptionResponse getPayload() {
    return payload;
  }

  public void setPayload(SubscriptionResponse payload) {
    this.payload = payload;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SubscriptionChangeEvent that)) return false;
    return getTimestamp() == that.getTimestamp()
            && Objects.equals(getEventType(), that.getEventType())
            && Objects.equals(getEventId(), that.getEventId())
            && Objects.equals(getPayload(), that.getPayload());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEventId(), getTimestamp(), getPayload(), getEventType());
  }

  @Override
  public String toString() {
    return "SubscriptionChangeEvent{" +
            "eventId='" + eventId + '\'' +
            ", timestamp=" + timestamp +
            ", eventType=" + eventType +
            ", payload=" + payload +
            '}';
  }
}
