package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.exception.UserNotFoundException;
import com.example.fx.subscription.service.model.*;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Observed(name = "subscriptions.service")
public class SubscriptionsService {

  private final SubscriptionRepository subscriptionRepository;
  private final FxUserRepository fxUserRepository;
  private final EventsOutboxRepository eventsOutboxRepository;

  public SubscriptionsService(SubscriptionRepository subscriptionRepository,
                              FxUserRepository fxUserRepository,
                              EventsOutboxRepository eventsOutboxRepository) {
    this.subscriptionRepository = subscriptionRepository;
    this.fxUserRepository = fxUserRepository;
    this.eventsOutboxRepository = eventsOutboxRepository;
  }

  @Transactional(readOnly = true)
  public Optional<SubscriptionResponse> findSubscriptionById(String id) {
    return subscriptionRepository.findById(UUID.fromString(id))
            .map(SubscriptionResponse::fromSubscription);
  }

  @Transactional(readOnly = true)
  public Optional<Subscription> findSubscriptionEntityById(String id) {
    return subscriptionRepository.findById(UUID.fromString(id));
  }

  @Transactional(readOnly = true)
  public List<Subscription> findSubscriptionsByUserId(String userId) {
    return subscriptionRepository.findAllByUserId(UUID.fromString(userId));
  }

  @Transactional(readOnly = true)
  public List<SubscriptionResponse> findSubscriptionResponsesByUserId(String userId) {
    return subscriptionRepository.findAllByUserId(UUID.fromString(userId))
            .stream()
            .map(SubscriptionResponse::fromSubscription)
            .toList();
  }

  @Transactional(readOnly = true)
  public List<SubscriptionResponse> findAllSubscriptionResponses() {
    return subscriptionRepository.findAll()
            .stream()
            .map(SubscriptionResponse::fromSubscription)
            .toList();
  }

  @Transactional(readOnly = true)
  public boolean isSubscriptionOwner(String subscriptionId, UUID userId) {
    return subscriptionRepository.findById(UUID.fromString(subscriptionId))
            .map(Subscription::getUser)
            .map(FxUser::getId)
            .filter(id -> id.equals(userId))
            .isPresent();
  }

  @Transactional
  public Subscription createSubscription(SubscriptionCreateRequest createRequest, UUID userId) {
    FxUser user = fxUserRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(
                    "User not found with ID: %s, please try with a different user!".formatted(userId)));

    Subscription subscription = subscriptionRepository.saveAndFlush(
            mapSubscriptionCreateRequestToSubscription(createRequest, user));
    eventsOutboxRepository.save(createSubscriptionsOutboxEvent(subscription, "SubscriptionCreated"));

    return subscription;
  }

  @Transactional
  public Subscription updateSubscriptionById(Subscription oldSubscription, SubscriptionUpdateRequest subscriptionUpdateRequest) {
    Optional.ofNullable(subscriptionUpdateRequest.currencyPair())
            .ifPresent(oldSubscription::setCurrencyPair);

    Optional.ofNullable(subscriptionUpdateRequest.direction())
            .map(ThresholdDirection::valueOf)
            .ifPresent(oldSubscription::setDirection);

    Optional.ofNullable(subscriptionUpdateRequest.status())
            .map(SubscriptionStatus::valueOf)
            .ifPresent(oldSubscription::setStatus);

    Optional.ofNullable(subscriptionUpdateRequest.threshold())
            .ifPresent(oldSubscription::setThreshold);

    Optional.ofNullable(subscriptionUpdateRequest.notificationChannels())
            .ifPresent(oldSubscription::setNotificationsChannels);

    Subscription subscription = subscriptionRepository.saveAndFlush(oldSubscription);
    eventsOutboxRepository.save(createSubscriptionsOutboxEvent(subscription, "SubscriptionUpdated"));

    return subscription;
  }

  @Transactional
  public void deleteSubscriptionById(String id) {
    Subscription subscription = subscriptionRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with ID: " + id, id));

    subscriptionRepository.deleteById(UUID.fromString(id));
    eventsOutboxRepository.save(createSubscriptionsOutboxEvent(subscription, "SubscriptionDeleted"));
  }

  private Subscription mapSubscriptionCreateRequestToSubscription(
          SubscriptionCreateRequest createRequest,
          FxUser user) {
    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setCurrencyPair(createRequest.currencyPair());
    subscription.setThreshold(createRequest.threshold());
    subscription.setDirection(ThresholdDirection.valueOf(createRequest.direction()));
    subscription.setNotificationsChannels(createRequest.notificationChannels());
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    return subscription;
  }

  private EventsOutbox createSubscriptionsOutboxEvent(Subscription subscription, String eventType) {
    EventsOutbox eventsOutbox = new EventsOutbox();
    eventsOutbox.setAggregateType("Subscription");
    eventsOutbox.setAggregateId(subscription.getId());
    eventsOutbox.setEventType(eventType);
    eventsOutbox.setPayload(SubscriptionResponse.fromSubscription(subscription));
    eventsOutbox.setStatus("PENDING");
    eventsOutbox.setTimestamp(System.currentTimeMillis());

    return eventsOutbox;
  }
}
