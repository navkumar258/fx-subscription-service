package com.example.fx.subscription.service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogSanitizerTest {

  @Test
  void testNullInput() {
    assertEquals("", LogSanitizer.sanitizeForLog(null));
  }

  @Test
  void testNoSpecialChars() {
    assertEquals("normal", LogSanitizer.sanitizeForLog("normal"));
  }

  @Test
  void testReplaceNewline() {
    assertEquals("abc_123", LogSanitizer.sanitizeForLog("abc\n123"));
  }

  @Test
  void testReplaceCarriageReturn() {
    assertEquals("abc_123", LogSanitizer.sanitizeForLog("abc\r123"));
  }

  @Test
  void testMultipleReplacements() {
    assertEquals("_abc_123_", LogSanitizer.sanitizeForLog("\nabc\r123\n"));
  }

}
