package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class SubscriptionChangePublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionChangePublisher.class);

  @Value(value = "${spring.kafka.topic.subscription-changes}")
  private String subscriptionChangesTopic;

  private final KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;
  private final EventsOutboxService eventsOutboxService;

  @Autowired
  public SubscriptionChangePublisher(KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate,
                                     EventsOutboxService eventsOutboxService) {
    this.kafkaTemplate = kafkaTemplate;
    this.eventsOutboxService = eventsOutboxService;
  }

  public void sendMessage(SubscriptionChangeEvent subscriptionChangeEvent) {
    CompletableFuture<SendResult<String, SubscriptionChangeEvent>> future = kafkaTemplate.send(
            subscriptionChangesTopic,
            subscriptionChangeEvent.payload().id().toString(),
            subscriptionChangeEvent
    );

    future.whenComplete((result, exception) -> {
      if (exception != null) {
        // handle failure
        LOGGER.error("[SubscriptionChangePublisher] Unable to send message: [{}] due to: [{}]",
                subscriptionChangeEvent,
                exception.getMessage());

        // Will implement send to DLQ later
        eventsOutboxService.updateOutboxStatus(subscriptionChangeEvent.eventId(), "FAILED");
      } else {
        // handle success
        LOGGER.info("[SubscriptionChangePublisher] Sent message: [{}] with offset: [{}]",
                subscriptionChangeEvent,
                result.getRecordMetadata().offset());

        eventsOutboxService.updateOutboxStatus(subscriptionChangeEvent.eventId(), "SENT");
      }
    });
  }
}
