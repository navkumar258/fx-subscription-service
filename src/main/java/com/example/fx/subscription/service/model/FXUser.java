package com.example.fx.subscription.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "fx_users")
public class FXUser implements Serializable {
  @Id
  @GeneratedValue
  private UUID id;

  @Column(unique = true)
  @JsonIgnore
  private String email;

  @Column(nullable = false)
  @JsonIgnore
  private String mobile;

  @JsonIgnore
  private String pushDeviceToken;

  @CreationTimestamp
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;

  @OneToMany(
          mappedBy = "user",
          cascade = CascadeType.ALL,
          fetch = FetchType.LAZY,
          orphanRemoval = true
  )
  private Set<Subscription> subscriptions = new HashSet<>();

  public FXUser() {}

  public FXUser(UUID userId) {
    this.id = userId;
  }

  FXUser(String email, String mobile, String pushDeviceToken) {
    this.email = email;
    this.mobile = mobile;
    this.pushDeviceToken = pushDeviceToken;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID uuid) {
    this.id = uuid;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public String getPushDeviceToken() {
    return pushDeviceToken;
  }

  public void setPushDeviceToken(String pushDeviceToken) {
    this.pushDeviceToken = pushDeviceToken;
  }

  public void addSubscription(Subscription subscription) {
    this.subscriptions.add(subscription);
    subscription.setUser(this); // Set the inverse side
  }

  public void removeSubscription(Subscription subscription) {
    this.subscriptions.remove(subscription);
    subscription.setUser(null); // Remove the inverse side reference
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FXUser that)) return false;
    return Objects.equals(getId(), that.getId())
            && Objects.equals(getEmail(), that.getEmail())
            && Objects.equals(getMobile(), that.getMobile())
            && Objects.equals(getPushDeviceToken(), that.getPushDeviceToken());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getEmail(), getMobile(), getPushDeviceToken());
  }

  @Override
  public String toString() {
    return "FXUser{" +
            "id=" + id +
            ", email='" + email + '\'' +
            ", mobile='" + mobile + '\'' +
            ", pushDeviceToken='" + pushDeviceToken + '\'' +
            '}';
  }
}
