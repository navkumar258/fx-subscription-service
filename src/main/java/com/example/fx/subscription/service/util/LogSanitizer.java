package com.example.fx.subscription.service.util;

public final class LogSanitizer {

  private LogSanitizer() {
  }

  public static String sanitizeForLog(String input) {
    if (input == null) {
      return "";
    }
    return input.replace('\n', '_').replace('\r', '_');
  }
}
