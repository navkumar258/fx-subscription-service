package com.example.fx.subscription.service.gatling.feeders;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("java:S2272")
public class RandomUserFeeder implements Iterator<Map<String, Object>> {
  private static final String EMAIL = "email";
  private static final String PASSWORD = "password";

  @Override
  public boolean hasNext() {
    return true;
  }

  @Override
  public Map<String, Object> next() {
    String uniqueId = UUID.randomUUID().toString();
    String uniqueEmail = "user-" + uniqueId + "@example.com";
    return Map.of(EMAIL, uniqueEmail, PASSWORD, "password123", "mobile", "+1234567890");
  }
}
