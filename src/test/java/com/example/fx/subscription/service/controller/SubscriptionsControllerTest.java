package com.example.fx.subscription.service.controller;

import com.example.fx.subscription.service.config.JwtTokenProvider;
import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.helper.WebSecurityTestConfig;
import com.example.fx.subscription.service.helper.WithMockFxUser;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import com.example.fx.subscription.service.service.SubscriptionsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@WebMvcTest(SubscriptionsController.class)
@Import(WebSecurityTestConfig.class)
class SubscriptionsControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private SubscriptionsService subscriptionsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "ADMIN")
    void whenInvalidUri_shouldReturn404() {
        assertThat(mockMvc.get().uri("/api/v1/subscriptions/123"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockFxUser()
    void whenValidUserId_shouldListAllUserSubscriptions() {
        List<SubscriptionResponse> subscriptions = List.of(
                new SubscriptionResponse(
                        UUID.fromString("6f0ad90b-8b07-4342-a918-6866ce3b72d3"),
                        null,
                        "GBP/USD",
                        BigDecimal.valueOf(1.20),
                        ThresholdDirection.ABOVE,
                        List.of("email", "sms"),
                        SubscriptionStatus.ACTIVE,
                        null,
                        null),
                new SubscriptionResponse(
                        UUID.fromString("af6ce3bc-39ad-44e4-a6a8-8314b52f8fa2"),
                        null,
                        "GBP/INR",
                        BigDecimal.valueOf(110),
                        ThresholdDirection.BELOW,
                        List.of("email"),
                        SubscriptionStatus.ACTIVE,
                        null,
                        null)
        );

        when(subscriptionsService.findSubscriptionResponsesByUserId(anyString()))
                .thenReturn(subscriptions);

        assertThat(mockMvc.get().uri("/api/v1/subscriptions?userId=7ca3517a-1930-4e18-916e-cae40f5dcfbe"))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON_VALUE)
                .bodyJson()
                .isEqualTo("{\"subscriptions\":" +
                        "[{\"id\":\"6f0ad90b-8b07-4342-a918-6866ce3b72d3\",\"user\":null,\"currencyPair\":\"GBP/USD\",\"threshold\":1.2,\"direction\":\"ABOVE\",\"notificationsChannels\":[\"email\",\"sms\"],\"status\":\"ACTIVE\",\"createdAt\":null,\"updatedAt\":null}," +
                        "{\"id\":\"af6ce3bc-39ad-44e4-a6a8-8314b52f8fa2\",\"user\":null,\"currencyPair\":\"GBP/INR\",\"threshold\":110,\"direction\":\"BELOW\",\"notificationsChannels\":[\"email\"],\"status\":\"ACTIVE\",\"createdAt\":null,\"updatedAt\":null}],\"totalCount\":2}");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void whenInvalidUserId_shouldReturn404() {
        when(subscriptionsService.findSubscriptionResponsesByUserId(anyString()))
                .thenReturn(List.of());

        assertThat(mockMvc.get().uri("/api/v1/subscriptions?userId=test_user_id"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("No Subscriptions found for the user ID: test_user_id, please try with a different user!");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSubscriptionById_WithValidId_ShouldReturnSubscription() {
        // Given
        String subscriptionId = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";
        SubscriptionResponse subscriptionResponse = new SubscriptionResponse(
                UUID.fromString(subscriptionId),
                null,
                "GBP/USD",
                BigDecimal.valueOf(1.25),
                ThresholdDirection.ABOVE,
                List.of("email", "sms"),
                SubscriptionStatus.ACTIVE,
                null,
                null
        );

        when(subscriptionsService.findSubscriptionById(subscriptionId)).thenReturn(Optional.of(subscriptionResponse));
        

        // When & Then
        assertThat(mockMvc.get().uri("/api/v1/subscriptions/" + subscriptionId))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON_VALUE)
                .bodyJson()
                .isEqualTo("{\"id\":\"" + subscriptionId + "\",\"user\":null,\"currencyPair\":\"GBP/USD\",\"threshold\":1.25,\"direction\":\"ABOVE\",\"notificationsChannels\":[\"email\",\"sms\"],\"status\":\"ACTIVE\",\"createdAt\":null,\"updatedAt\":null}");

        verify(subscriptionsService).findSubscriptionById(subscriptionId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSubscriptionById_WithInvalidId_ShouldReturn404() {
        // Given
        String subscriptionId = "invalid-id";

        when(subscriptionsService.findSubscriptionById(subscriptionId)).thenReturn(Optional.empty());

        // When & Then
        assertThat(mockMvc.get().uri("/api/v1/subscriptions/" + subscriptionId))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("No Subscriptions found for the ID: " + subscriptionId + ", please try with a different id!");

        verify(subscriptionsService).findSubscriptionById(subscriptionId);
    }

    @Test
    @WithMockFxUser(email = "alice@example.com")
    void getMySubscriptions_WithValidUser_ShouldReturnSubscriptions() {
        // Given
        List<SubscriptionResponse> subscriptions = List.of(
                new SubscriptionResponse(
                        UUID.fromString("6f0ad90b-8b07-4342-a918-6866ce3b72d3"),
                        null,
                        "GBP/USD",
                        BigDecimal.valueOf(1.25),
                        ThresholdDirection.ABOVE,
                        List.of("email", "sms"),
                        SubscriptionStatus.ACTIVE,
                        null,
                        null)
        );

        when(subscriptionsService.findSubscriptionResponsesByUserId(anyString())).thenReturn(subscriptions);

        // When & Then
        assertThat(mockMvc.get().uri("/api/v1/subscriptions/my"))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON_VALUE)
                .bodyJson()
                .isEqualTo("{\"subscriptions\":[{\"id\":\"6f0ad90b-8b07-4342-a918-6866ce3b72d3\",\"user\":null,\"currencyPair\":\"GBP/USD\",\"threshold\":1.25,\"direction\":\"ABOVE\",\"notificationsChannels\":[\"email\",\"sms\"],\"status\":\"ACTIVE\",\"createdAt\":null,\"updatedAt\":null}],\"totalCount\":1}");

        verify(subscriptionsService).findSubscriptionResponsesByUserId(anyString());
    }

    @Test
    @WithMockFxUser(email = "jon@example.com")
    void getMySubscriptions_WithEmptyList_ShouldReturn404() {
        // Given
        when(subscriptionsService.findSubscriptionResponsesByUserId(anyString())).thenReturn(List.of());

        // When & Then
        assertThat(mockMvc.get().uri("/api/v1/subscriptions/my"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("No Subscriptions found for your account");

        verify(subscriptionsService).findSubscriptionResponsesByUserId(anyString());
    }

    @Test
    @WithMockFxUser(email = "amy@example.com")
    void createSubscription_WithValidRequest_ShouldCreateSubscription() throws Exception {
        // Given
        SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
                "GBP/USD",
                BigDecimal.valueOf(1.25),
                ThresholdDirection.ABOVE.name(),
                List.of("email", "sms"));
        
        Subscription createdSubscription = createTestSubscription();

        when(subscriptionsService.createSubscription(any(SubscriptionCreateRequest.class), any(UUID.class)))
                .thenReturn(createdSubscription);

        // When & Then
        assertThat(mockMvc.post().uri("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(createRequest)))
                .hasStatus(HttpStatus.CREATED)
                .hasContentType(MediaType.APPLICATION_JSON_VALUE);

        verify(subscriptionsService).createSubscription(any(SubscriptionCreateRequest.class), any(UUID.class));
    }

    @Test
    @WithMockFxUser(email = "bob@example.com")
    void createSubscription_WithInvalidRequest_ShouldReturn400() throws Exception {
        // Given
        SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
                "", // invalid currency pair
                BigDecimal.valueOf(-1.25), // invalid threshold
                "INVALID_DIRECTION",
                List.of()); // empty notifications

        // When & Then
        assertThat(mockMvc.post().uri("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(createRequest)))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(subscriptionsService, never()).createSubscription(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSubscriptionById_WithValidId_ShouldUpdateSubscription() throws Exception {
        // Given
        String subscriptionId = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";
        SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
                "EUR/USD",
                BigDecimal.valueOf(1.10),
                ThresholdDirection.BELOW.name(),
                SubscriptionStatus.ACTIVE.name(),
                List.of("email"));
        
        Subscription existingSubscription = createTestSubscription();
        Subscription updatedSubscription = createTestSubscription();

        when(subscriptionsService.findSubscriptionEntityById(subscriptionId))
                .thenReturn(Optional.of(existingSubscription));
        when(subscriptionsService.updateSubscriptionById(any(Subscription.class), any(SubscriptionUpdateRequest.class)))
                .thenReturn(updatedSubscription);

        // When & Then
        assertThat(mockMvc.put().uri("/api/v1/subscriptions/" + subscriptionId)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON_VALUE);

        verify(subscriptionsService).findSubscriptionEntityById(subscriptionId);
        verify(subscriptionsService).updateSubscriptionById(any(Subscription.class), any(SubscriptionUpdateRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSubscriptionById_WithInvalidId_ShouldReturn404() throws Exception {
        // Given
        String subscriptionId = "invalid-id";
        SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
                "EUR/USD",
                BigDecimal.valueOf(1.10),
                ThresholdDirection.BELOW.name(),
                SubscriptionStatus.ACTIVE.name(),
                List.of("email"));

        when(subscriptionsService.findSubscriptionEntityById(subscriptionId)).thenReturn(Optional.empty());

        // When & Then
        assertThat(mockMvc.put().uri("/api/v1/subscriptions/" + subscriptionId)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("No Subscriptions found for the ID: " + subscriptionId + ", please try with a different id!");

        verify(subscriptionsService).findSubscriptionEntityById(subscriptionId);
        verify(subscriptionsService, never()).updateSubscriptionById(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSubscriptionById_WithInvalidRequest_ShouldReturn400() throws Exception {
        // Given
        String subscriptionId = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";
        SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
                "", // invalid currency pair
                BigDecimal.valueOf(-1.10), // invalid threshold
                "INVALID_DIRECTION",
                "INVALID_STATUS",
                List.of()); // empty notifications

        // When & Then
        assertThat(mockMvc.put().uri("/api/v1/subscriptions/" + subscriptionId)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(subscriptionsService, never()).findSubscriptionEntityById(any());
        verify(subscriptionsService, never()).updateSubscriptionById(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteSubscriptionById_WithValidId_ShouldDeleteSubscription() {
        // Given
        String subscriptionId = "6f0ad90b-8b07-4342-a918-6866ce3b72d3";

        doNothing().when(subscriptionsService).deleteSubscriptionById(subscriptionId);

        // When & Then
        assertThat(mockMvc.delete().uri("/api/v1/subscriptions/" + subscriptionId))
                .hasStatus(HttpStatus.NO_CONTENT);

        verify(subscriptionsService).deleteSubscriptionById(subscriptionId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllSubscriptions_ShouldReturnAllSubscriptions() {
        // Given
        List<SubscriptionResponse> allSubscriptions = List.of(
                new SubscriptionResponse(
                        UUID.fromString("6f0ad90b-8b07-4342-a918-6866ce3b72d3"),
                        null,
                        "GBP/USD",
                        BigDecimal.valueOf(1.25),
                        ThresholdDirection.ABOVE,
                        List.of("email", "sms"),
                        SubscriptionStatus.ACTIVE,
                        null,
                        null),
                new SubscriptionResponse(
                        UUID.fromString("af6ce3bc-39ad-44e4-a6a8-8314b52f8fa2"),
                        null,
                        "EUR/USD",
                        BigDecimal.valueOf(1.10),
                        ThresholdDirection.BELOW,
                        List.of("email"),
                        SubscriptionStatus.ACTIVE,
                        null,
                        null)
        );

        when(subscriptionsService.findAllSubscriptionResponses()).thenReturn(allSubscriptions);

        // When & Then
        assertThat(mockMvc.get().uri("/api/v1/subscriptions/all"))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON_VALUE)
                .bodyJson()
                .isEqualTo("{\"subscriptions\":[{\"id\":\"6f0ad90b-8b07-4342-a918-6866ce3b72d3\",\"user\":null,\"currencyPair\":\"GBP/USD\",\"threshold\":1.25,\"direction\":\"ABOVE\",\"notificationsChannels\":[\"email\",\"sms\"],\"status\":\"ACTIVE\",\"createdAt\":null,\"updatedAt\":null},{\"id\":\"af6ce3bc-39ad-44e4-a6a8-8314b52f8fa2\",\"user\":null,\"currencyPair\":\"EUR/USD\",\"threshold\":1.1,\"direction\":\"BELOW\",\"notificationsChannels\":[\"email\"],\"status\":\"ACTIVE\",\"createdAt\":null,\"updatedAt\":null}],\"totalCount\":2}");

        verify(subscriptionsService).findAllSubscriptionResponses();
    }

    @Test
    @WithMockUser
    void getAllSubscriptions_WithNonAdminUser_ShouldReturn403() {
        // When & Then
        assertThat(mockMvc.get().uri("/api/v1/subscriptions/all"))
                .hasStatus(HttpStatus.FORBIDDEN);

        verify(subscriptionsService, never()).findAllSubscriptionResponses();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllSubscriptions_WithEmptyList_ShouldReturnEmptyResponse() {
        // Given
        when(subscriptionsService.findAllSubscriptionResponses()).thenReturn(List.of());

        // When & Then
        assertThat(mockMvc.get().uri("/api/v1/subscriptions/all"))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON_VALUE)
                .bodyJson()
                .isEqualTo("{\"subscriptions\":[],\"totalCount\":0}");

        verify(subscriptionsService).findAllSubscriptionResponses();
    }

    // Helper methods
    private Subscription createTestSubscription() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setCurrencyPair("GBP/USD");
        subscription.setThreshold(BigDecimal.valueOf(1.25));
        subscription.setDirection(ThresholdDirection.ABOVE);
        subscription.setNotificationsChannels(List.of("email", "sms"));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }
}
