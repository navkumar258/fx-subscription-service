package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.model.EventsOutbox;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionsService {

  private final SubscriptionRepository subscriptionRepository;
  private final EventsOutboxRepository eventsOutboxRepository;

  public SubscriptionsService(SubscriptionRepository subscriptionRepository,
                              EventsOutboxRepository eventsOutboxRepository) {
    this.subscriptionRepository = subscriptionRepository;
    this.eventsOutboxRepository = eventsOutboxRepository;
  }

  public Optional<Subscription> getSubscriptionById(String id) {
    return subscriptionRepository.findById(UUID.fromString(id));
  }

  public List<Subscription> getSubscriptionsByUserId(String userId) {
    return subscriptionRepository.findAllByUserId(UUID.fromString(userId));
  }

  public Subscription createSubscription(Subscription subscriptionCreateRequest) {
    Subscription subscription = subscriptionRepository.save(subscriptionCreateRequest);
    eventsOutboxRepository.save(createSubscriptionsOutboxEvent(subscription, "SubscriptionCreated"));

    return subscription;
  }

  public Subscription updateSubscriptionById(Subscription oldSubscription, Subscription subscriptionUpdateRequest) {
    if(subscriptionUpdateRequest.getCurrencyPair() != null) oldSubscription.setCurrencyPair(subscriptionUpdateRequest.getCurrencyPair());
    if(subscriptionUpdateRequest.getDirection() != null) oldSubscription.setDirection(subscriptionUpdateRequest.getDirection());
    if(subscriptionUpdateRequest.getStatus() != null) oldSubscription.setStatus(subscriptionUpdateRequest.getStatus());
    if(subscriptionUpdateRequest.getThreshold() != null) oldSubscription.setThreshold(subscriptionUpdateRequest.getThreshold());
    if(subscriptionUpdateRequest.getNotificationsChannels() != null) oldSubscription.setNotificationsChannels(subscriptionUpdateRequest.getNotificationsChannels());

    Subscription subscription = subscriptionRepository.save(oldSubscription);
    eventsOutboxRepository.save(createSubscriptionsOutboxEvent(subscription, "SubscriptionUpdated"));

    return subscription;
  }

  public void deleteSubscriptionById(String id) {
    Optional<Subscription> subscription = findSubscriptionById(id);

    if(subscription.isPresent()) {
      subscriptionRepository.deleteById(UUID.fromString(id));
      eventsOutboxRepository.save(createSubscriptionsOutboxEvent(subscription.get(), "SubscriptionDeleted"));
    }
  }

  public Optional<Subscription> findSubscriptionById(String id) {
    return subscriptionRepository.findById(UUID.fromString(id));
  }

  private EventsOutbox createSubscriptionsOutboxEvent(Subscription subscription, String eventType) {
    EventsOutbox eventsOutbox = new EventsOutbox();
    eventsOutbox.setAggregateType("Subscription");
    eventsOutbox.setAggregateId(subscription.getId());
    eventsOutbox.setEventType(eventType);
    eventsOutbox.setPayload(subscription);
    eventsOutbox.setStatus("PENDING");
    eventsOutbox.setTimestamp(System.currentTimeMillis());

    return eventsOutbox;
  }

}
