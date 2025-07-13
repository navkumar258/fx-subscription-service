package com.example.fx.subscription.service.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void success_WithDataAndMessage_ShouldCreateSuccessResponse() {
        // Given
        String data = "test data";
        String message = "Test message";

        // When
        ApiResponse<String> response = ApiResponse.success(data, message);

        // Then
        assertTrue(response.success());
        assertEquals(message, response.message());
        assertEquals(data, response.data());
        assertNotNull(response.timestamp());
        assertTrue(response.timestamp().isBefore(Instant.now().plusSeconds(1)));
        assertTrue(response.timestamp().isAfter(Instant.now().minusSeconds(1)));
    }

    @Test
    void success_WithDataOnly_ShouldCreateSuccessResponseWithDefaultMessage() {
        // Given
        String data = "test data";

        // When
        ApiResponse<String> response = ApiResponse.success(data);

        // Then
        assertTrue(response.success());
        assertEquals("Operation completed successfully", response.message());
        assertEquals(data, response.data());
        assertNotNull(response.timestamp());
    }

    @Test
    void error_WithMessage_ShouldCreateErrorResponse() {
        // Given
        String errorMessage = "Error occurred";

        // When
        ApiResponse<String> response = ApiResponse.error(errorMessage);

        // Then
        assertFalse(response.success());
        assertEquals(errorMessage, response.message());
        assertNull(response.data());
        assertNotNull(response.timestamp());
    }

    @Test
    void success_WithNullData_ShouldHandleNullData() {
        // When
        ApiResponse<String> response = ApiResponse.success(null, "Test");

        // Then
        assertTrue(response.success());
        assertNull(response.data());
    }

    @Test
    void error_WithNullMessage_ShouldHandleNullMessage() {
        // When
        ApiResponse<String> response = ApiResponse.error(null);

        // Then
        assertFalse(response.success());
        assertNull(response.message());
        assertNull(response.data());
    }
} 