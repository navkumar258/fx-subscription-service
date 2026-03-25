package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.model.EventsOutbox;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionChangeScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionChangeScheduler.class);

  private final EventsOutboxRepository eventsOutboxRepository;
  private final SubscriptionChangePublisher subscriptionChangePublisher;

  public SubscriptionChangeScheduler(EventsOutboxRepository eventsOutboxRepository,
                                     SubscriptionChangePublisher subscriptionChangePublisher) {
    this.eventsOutboxRepository = eventsOutboxRepository;
    this.subscriptionChangePublisher = subscriptionChangePublisher;
  }

  @Scheduled(
          initialDelayString = "${outbox.subscriptions.check.initial-delay}",
          fixedRateString = "${outbox.subscriptions.check.rate}"
  )
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkForOutboxSubscriptions() {
    LOGGER.info("[SubscriptionChangeScheduler] START checking outbox table for subscriptions to publish...");

    var pendingEvents = eventsOutboxRepository.findByStatus("PENDING").stream()
            .map(this::toEvent)
            .toList();

    if (pendingEvents.isEmpty()) return;

    for (var event : pendingEvents) {
      subscriptionChangePublisher.sendMessage(event);
    }

    LOGGER.info("[SubscriptionChangeScheduler] END published all PENDING subscriptions");
  }

  private SubscriptionChangeEvent toEvent(EventsOutbox eventsOutbox) {
    return new SubscriptionChangeEvent(
            eventsOutbox.getId().toString(),
            eventsOutbox.getTimestamp(),
            eventsOutbox.getEventType(),
            eventsOutbox.getPayload()
    );
  }
}
