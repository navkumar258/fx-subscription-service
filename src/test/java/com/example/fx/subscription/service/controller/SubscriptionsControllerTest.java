package com.example.fx.subscription.service.controller;

import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

@WebMvcTest(SubscriptionsController.class)
class SubscriptionsControllerTest {

  @Autowired
  private MockMvcTester mockMvc;

  @MockitoBean
  private SubscriptionsService subscriptionsService;

  @Test
  void whenInvalidUri_shouldReturn404() {
    assertThat(mockMvc.get().uri("/subscription"))
            .hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void whenValidUserId_shouldListAllUserSubscriptions() {
    List<Subscription> subscriptions = new ArrayList<>();
    subscriptions.add(new Subscription(
            UUID.fromString("6f0ad90b-8b07-4342-a918-6866ce3b72d3"),
            "GBP/USD",
            BigDecimal.valueOf(1.20),
            ThresholdDirection.ABOVE,
            List.of("email", "sms"),
            SubscriptionStatus.ACTIVE));
    subscriptions.add(new Subscription(
            UUID.fromString("af6ce3bc-39ad-44e4-a6a8-8314b52f8fa2"),
            "GBP/INR",
            BigDecimal.valueOf(110),
            ThresholdDirection.BELOW,
            List.of("email"),
            SubscriptionStatus.ACTIVE));

    Mockito.when(subscriptionsService.findSubscriptionsByUserId(anyString()))
            .thenReturn(subscriptions);

    assertThat(mockMvc.get().uri("/subscriptions?userId=test_user_1"))
            .hasStatusOk()
            .hasContentType(MediaType.APPLICATION_JSON_VALUE)
            .bodyJson()
            .isEqualTo("[{\"id\":\"6f0ad90b-8b07-4342-a918-6866ce3b72d3\",\"user\":null,\"currencyPair\":\"GBP/USD\",\"threshold\":1.2,\"direction\":\"ABOVE\",\"notificationsChannels\":[\"email\",\"sms\"],\"status\":\"ACTIVE\",\"createdAt\":null,\"updatedAt\":null}," +
                    "{\"id\":\"af6ce3bc-39ad-44e4-a6a8-8314b52f8fa2\",\"user\":null,\"currencyPair\":\"GBP/INR\",\"threshold\":110,\"direction\":\"BELOW\",\"notificationsChannels\":[\"email\"],\"status\":\"ACTIVE\",\"createdAt\":null,\"updatedAt\":null}]");
  }

  @Test
  void whenInvalidUserId_shouldReturn404() {
    Mockito.when(subscriptionsService.findSubscriptionsByUserId(anyString()))
            .thenReturn(new ArrayList<>());

    assertThat(mockMvc.get().uri("/subscriptions?userId=test_user_1"))
            .hasStatus(HttpStatus.NOT_FOUND)
            .bodyText()
            .isEqualTo("No Subscriptions found for the user ID: test_user_1, please try with a different user!");
  }
}
