package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.model.EventsOutbox;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableScheduling
public class SubscriptionChangeScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionChangeScheduler.class);

  private final EventsOutboxRepository eventsOutboxRepository;
  private final SubscriptionChangePublisher subscriptionChangePublisher;

  public SubscriptionChangeScheduler(EventsOutboxRepository eventsOutboxRepository,
                                     SubscriptionChangePublisher subscriptionChangePublisher) {
    this.eventsOutboxRepository = eventsOutboxRepository;
    this.subscriptionChangePublisher = subscriptionChangePublisher;
  }

  @Scheduled(initialDelay = 30, fixedRateString = "${outbox.subscriptions.check.rate:30000}")
  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkForOutboxSubscriptions() {
    LOGGER.info("[SubscriptionChangeScheduler] START checking outbox table for subscriptions to publish...");
    int count = 0;

    for (EventsOutbox eventsOutbox : eventsOutboxRepository.findByStatus("PENDING")) {
      subscriptionChangePublisher.sendMessage(createSubscriptionChangeEvent(eventsOutbox));
      count++;
    }

    LOGGER.info("[SubscriptionChangeScheduler] END published {} PENDING subscriptions", count);
  }

  private SubscriptionChangeEvent createSubscriptionChangeEvent(EventsOutbox eventsOutbox) {
    SubscriptionChangeEvent subscriptionChangeEvent = new SubscriptionChangeEvent();
    subscriptionChangeEvent.setEventId(eventsOutbox.getId().toString());
    subscriptionChangeEvent.setEventType(eventsOutbox.getEventType());
    subscriptionChangeEvent.setTimestamp(eventsOutbox.getTimestamp());
    subscriptionChangeEvent.setPayload(eventsOutbox.getPayload());

    return subscriptionChangeEvent;
  }

}
