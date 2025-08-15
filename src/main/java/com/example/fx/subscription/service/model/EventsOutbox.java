package com.example.fx.subscription.service.model;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "events_outbox")
public class EventsOutbox implements Serializable {

  @Serial
  private static final long serialVersionUID = 6192587654480707584L;

  @Id
  @GeneratedValue
  private UUID id;

  private String aggregateType;

  private UUID aggregateId;

  private String eventType;

  @Type(JsonType.class)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private SubscriptionResponse payload;

  private String status;

  private long timestamp;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public void setAggregateType(String aggregateType) {
    this.aggregateType = aggregateType;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public void setAggregateId(UUID aggregateId) {
    this.aggregateId = aggregateId;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public EventsOutbox() {}

  public EventsOutbox(UUID id, String status) {
    this.id = id;
    this.status = status;
  }

  public EventsOutbox(String status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof EventsOutbox that)) return false;
    return Objects.equals(id, that.id)
            && Objects.equals(getAggregateType(), that.getAggregateType())
            && Objects.equals(getAggregateId(), that.getAggregateId())
            && Objects.equals(getEventType(), that.getEventType())
            && Objects.equals(getPayload(), that.getPayload())
            && Objects.equals(getStatus(), that.getStatus())
            && Objects.equals(getTimestamp(), that.getTimestamp());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, getAggregateType(), getAggregateId(), getEventType(), getPayload(), getStatus(), getTimestamp());
  }

  @Override
  public String toString() {
    return "EventsOutbox{" +
            "id=" + id +
            ", aggregateType='" + aggregateType + '\'' +
            ", aggregateId=" + aggregateId +
            ", eventType='" + eventType + '\'' +
            ", payload=" + payload +
            ", status=" + status +
            ", timestamp=" + timestamp +
            '}';
  }
}
