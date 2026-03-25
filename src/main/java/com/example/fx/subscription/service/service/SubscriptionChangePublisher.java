package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionChangePublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionChangePublisher.class);

  private final KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;
  private final EventsOutboxService eventsOutboxService;
  private final String subscriptionChangesTopic;

  public SubscriptionChangePublisher(KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate,
                                     EventsOutboxService eventsOutboxService,
                                     @Value(value = "${spring.kafka.topic.subscription-changes}") String subscriptionChangesTopic) {
    this.kafkaTemplate = kafkaTemplate;
    this.eventsOutboxService = eventsOutboxService;
    this.subscriptionChangesTopic = subscriptionChangesTopic;
  }

  public void sendMessage(SubscriptionChangeEvent event) {
    final String key = event.payload().id();

    try {
      var result = kafkaTemplate.send(subscriptionChangesTopic, key, event).join();

      LOGGER.info("Published SubscriptionChangeEvent with id: [{}] at offset: [{}]", key, result.getRecordMetadata().offset());
      eventsOutboxService.updateOutboxStatus(event.eventId(), "SENT");
    } catch (Exception ex) {
      LOGGER.error("Failed to publish SubscriptionChangeEvent with id: [{}]", key, ex);
      eventsOutboxService.updateOutboxStatus(event.eventId(), "FAILED");

      if (ex.getCause() instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
