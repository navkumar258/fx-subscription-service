package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.model.EventsOutbox;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class SubscriptionChangePublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionChangePublisher.class);

  @Value(value = "${spring.kafka.topic.subscription-changes}")
  private String subscriptionChangesTopic;

  private final KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;
  private final EventsOutboxRepository eventsOutboxRepository;

  @Autowired
  public SubscriptionChangePublisher(KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate,
                                     EventsOutboxRepository eventsOutboxRepository) {
    this.kafkaTemplate = kafkaTemplate;
    this.eventsOutboxRepository = eventsOutboxRepository;
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

        ProducerRecord<String, EventsOutbox> producerRecord = ((KafkaProducerException) exception).getFailedProducerRecord();
        throw new KafkaProducerException(producerRecord, "Failed to publish message", exception);
      } else {
        // handle success
        LOGGER.info("[SubscriptionChangePublisher] Sent message: [{}] with offset: [{}]",
                subscriptionChangeEvent,
                result.getRecordMetadata().offset());
        Optional<EventsOutbox> eventsOutbox = eventsOutboxRepository.findById(UUID.fromString(subscriptionChangeEvent.eventId()));
        eventsOutbox.ifPresent(this::updateOutboxEventStatus);
      }
    });
  }

  private void updateOutboxEventStatus(EventsOutbox eventsOutbox) {
    eventsOutbox.setStatus("SENT");
    eventsOutboxRepository.save(eventsOutbox);
  }
}
