package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.model.EventsOutboxStatus;
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
		String key = event.payload().id();

		try {
			kafkaTemplate.send(subscriptionChangesTopic, key, event)
					.whenComplete((result, ex) -> {
						if (ex == null) {
							LOGGER.info("Published SubscriptionChangeEvent with id: [{}] at offset: [{}]",
									key, result.getRecordMetadata().offset());
							eventsOutboxService.updateOutboxStatus(event.eventId(), EventsOutboxStatus.SENT);
						} else {
							handleFailure(event.eventId(), key, ex, "asynchronously");
						}
					});
		} catch (Exception ex) {
			handleFailure(event.eventId(), key, ex, "synchronously");
		}
	}

	private void handleFailure(String eventId, String key, Throwable ex, String mode) {
		LOGGER.error("Failed [{}] to publish SubscriptionChangeEvent with id: [{}]", mode, key, ex);
		eventsOutboxService.updateOutboxStatus(eventId, EventsOutboxStatus.FAILED);
	}
}
