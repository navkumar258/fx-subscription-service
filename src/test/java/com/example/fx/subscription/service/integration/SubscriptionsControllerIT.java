package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.SubscriptionCreateRequest;
import com.example.fx.subscription.service.model.FXUser;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionsControllerIT {

  @Autowired
  MockMvcTester mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  UserRepository userRepository;

  @MockitoBean
  KafkaAdmin kafkaAdmin;

  @MockitoBean
  KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

  @Test
  void whenValidRequest_shouldReturn200() throws JsonProcessingException {
    FXUser user = new FXUser();
    user.setEmail("test_user@mail.com");
    user.setMobile("+447911123456");
    userRepository.save(user);

    String userId = userRepository.findByEmail("test_user@mail.com").get().getId().toString();

    SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest();
    createRequest.setUserId(userId);
    createRequest.setCurrencyPair("GBP/USD");
    createRequest.setThreshold(BigDecimal.valueOf(1.20));
    createRequest.setDirection("ABOVE");
    createRequest.setNotificationChannels(List.of("sms", "email"));

    assertThat(mockMvc.post()
            .uri("/subscriptions")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(createRequest)))
            .hasStatusOk()
            .hasContentType(MediaType.APPLICATION_JSON_VALUE)
            .bodyJson()
            .hasPathSatisfying("$.currencyPair", currencyPairAssert -> currencyPairAssert.assertThat().isEqualTo("GBP/USD"))
            .hasPathSatisfying("$.threshold", thresholdAssert -> thresholdAssert.assertThat().asNumber().isEqualTo(1.20))
            .hasPathSatisfying("$.direction", directionAssert -> directionAssert.assertThat().isEqualTo("ABOVE"))
            .hasPathSatisfying("$.notificationsChannels[0]", notificationChannelsAssert -> notificationChannelsAssert.assertThat().asString().isEqualTo("sms"))
            .hasPathSatisfying("$.notificationsChannels[1]", notificationChannelsAssert -> notificationChannelsAssert.assertThat().asString().isEqualTo("email"))
            .hasPathSatisfying("$.user.id", userIdAssert -> userIdAssert.assertThat().isEqualTo(userId));
  }
}
