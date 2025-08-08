package com.example.fx.subscription.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "fx_users")
public class FxUser implements Serializable, UserDetails {
  @Serial
  private static final long serialVersionUID = -2164347536884263543L;

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false, unique = true)
  @JsonIgnore
  private String email;

  @Column(nullable = false)
  @JsonIgnore
  private String mobile;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private boolean enabled;

  @JsonIgnore
  private String pushDeviceToken;

  @CreationTimestamp
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role = UserRole.USER;

  @OneToMany(
          mappedBy = "user",
          cascade = CascadeType.ALL,
          fetch = FetchType.LAZY,
          orphanRemoval = true
  )
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private Set<Subscription> subscriptions = new HashSet<>();

  public FxUser() {
  }

  public FxUser(UUID userId) {
    this.id = userId;
  }

  public FxUser(UUID userId, String email, String password, UserRole role) {
    this.id = userId;
    this.email = email;
    this.password = password;
    this.role = role;
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

  public Set<Subscription> getSubscriptions() {
    return subscriptions;
  }

  public void setSubscriptions(Set<Subscription> subscriptions) {
    this.subscriptions = subscriptions;
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
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
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
    if (!(o instanceof FxUser that)) return false;
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
    return "FxUser{" +
            "id=" + id +
            ", email='" + email + '\'' +
            ", mobile='" + mobile + '\'' +
            ", pushDeviceToken='" + pushDeviceToken + '\'' +
            '}';
  }
}
