package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.exception.UserNotFoundException;
import com.example.fx.subscription.service.model.*;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
import com.example.fx.subscription.service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionsService {

  private final SubscriptionRepository subscriptionRepository;
  private final UserRepository userRepository;
  private final EventsOutboxRepository eventsOutboxRepository;

  public SubscriptionsService(SubscriptionRepository subscriptionRepository,
                              UserRepository userRepository,
                              EventsOutboxRepository eventsOutboxRepository) {
    this.subscriptionRepository = subscriptionRepository;
    this.userRepository = userRepository;
    this.eventsOutboxRepository = eventsOutboxRepository;
  }

  public Optional<Subscription> findSubscriptionById(String id) {
    return subscriptionRepository.findById(UUID.fromString(id));
  }

  public List<Subscription> findSubscriptionsByUserId(String userId) {
    return subscriptionRepository.findAllByUserId(UUID.fromString(userId));
  }

  @Transactional
  public Subscription createSubscription(SubscriptionCreateRequest createRequest) {
    UUID userId = UUID.fromString(createRequest.getUserId());
    FXUser user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(
                    "User not found with ID: %s, please try with a different user!".formatted(createRequest.getUserId())));

    Subscription subscription = subscriptionRepository.saveAndFlush(
            mapSubscriptionCreateRequestToSubscription(createRequest, user));
    eventsOutboxRepository.save(createSubscriptionsOutboxEvent(subscription, "SubscriptionCreated"));

    return subscription;
  }

  @Transactional
  public Subscription updateSubscriptionById(Subscription oldSubscription, SubscriptionUpdateRequest subscriptionUpdateRequest) {
    if(subscriptionUpdateRequest.getCurrencyPair() != null) oldSubscription.setCurrencyPair(subscriptionUpdateRequest.getCurrencyPair());
    if(subscriptionUpdateRequest.getDirection() != null) oldSubscription.setDirection(ThresholdDirection.valueOf(subscriptionUpdateRequest.getDirection()));
    if(subscriptionUpdateRequest.getStatus() != null) oldSubscription.setStatus(SubscriptionStatus.valueOf(subscriptionUpdateRequest.getStatus()));
    if(subscriptionUpdateRequest.getThreshold() != null) oldSubscription.setThreshold(subscriptionUpdateRequest.getThreshold());
    if(subscriptionUpdateRequest.getNotificationChannels() != null) oldSubscription.setNotificationsChannels(subscriptionUpdateRequest.getNotificationChannels());

    Subscription subscription = subscriptionRepository.saveAndFlush(oldSubscription);
    eventsOutboxRepository.save(createSubscriptionsOutboxEvent(subscription, "SubscriptionUpdated"));

    return subscription;
  }

  @Transactional
  public void deleteSubscriptionById(String id) {
    Optional<Subscription> subscription = findSubscriptionById(id);

    if(subscription.isPresent()) {
      subscriptionRepository.deleteById(UUID.fromString(id));
      eventsOutboxRepository.save(createSubscriptionsOutboxEvent(subscription.get(), "SubscriptionDeleted"));
    }
  }

  private Subscription mapSubscriptionCreateRequestToSubscription(
          SubscriptionCreateRequest createRequest,
          FXUser user) {
    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setCurrencyPair(createRequest.getCurrencyPair());
    subscription.setThreshold(createRequest.getThreshold());
    subscription.setDirection(ThresholdDirection.valueOf(createRequest.getDirection()));
    subscription.setNotificationsChannels(createRequest.getNotificationChannels());
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    return subscription;
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
