package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.subscription.*;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.exception.UserNotFoundException;
import com.example.fx.subscription.service.model.*;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@CacheConfig(cacheNames = "subscription")
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
  @Cacheable(key = "#id")
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
  @Cacheable(cacheNames = "subscriptionsByUser", key = "#userId", unless = "#result.totalCount() == 0")
  public SubscriptionListResponse findSubscriptionResponsesByUserId(String userId) {
    List<SubscriptionResponse> subscriptions = subscriptionRepository.findAllByUserId(UUID.fromString(userId))
            .stream()
            .map(SubscriptionResponse::fromSubscription)
            .toList();
    if (subscriptions.isEmpty()) {
      throw new SubscriptionNotFoundException("No subscriptions found for the given user id: %s".formatted(userId));
    }

    return new SubscriptionListResponse(subscriptions, subscriptions.size());
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
    return subscriptionRepository.existsByIdAndUserId(
            UUID.fromString(subscriptionId), userId);
  }

  @Transactional
  @Caching(
          put = {@CachePut(key = "#result.id")},
          evict = {@CacheEvict(cacheNames = "subscriptionsByUser", key = "#userId.toString()")}
  )
  public SubscriptionResponse createSubscription(SubscriptionCreateRequest createRequest, UUID userId) {
    FxUser user = fxUserRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(
                    "User not found with ID: %s, please try with a different user!".formatted(userId)));

    Subscription subscription = subscriptionRepository.saveAndFlush(
            mapSubscriptionCreateRequestToSubscription(createRequest, user));
    eventsOutboxRepository.save(createSubscriptionsOutboxEvent(subscription, "SubscriptionCreated"));

    return SubscriptionResponse.fromSubscription(subscription);
  }

  @Transactional
  @Caching(
          put = {@CachePut(key = "#id")},
          evict = {@CacheEvict(cacheNames = "subscriptionsByUser", key = "#result.user.id")}
  )
  public SubscriptionResponse updateSubscriptionById(String id, SubscriptionUpdateRequest subscriptionUpdateRequest) {
    Subscription subscription = subscriptionRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new SubscriptionNotFoundException(
                    "Subscription not found with Id: %s".formatted(id), id));

    Optional.ofNullable(subscriptionUpdateRequest.currencyPair())
            .ifPresent(subscription::setCurrencyPair);

    Optional.ofNullable(subscriptionUpdateRequest.direction())
            .map(ThresholdDirection::valueOf)
            .ifPresent(subscription::setDirection);

    Optional.ofNullable(subscriptionUpdateRequest.status())
            .map(SubscriptionStatus::valueOf)
            .ifPresent(subscription::setStatus);

    Optional.ofNullable(subscriptionUpdateRequest.threshold())
            .ifPresent(subscription::setThreshold);

    Optional.ofNullable(subscriptionUpdateRequest.notificationChannels())
            .ifPresent(subscription::setNotificationsChannels);

    Subscription updatedSubscription = subscriptionRepository.saveAndFlush(subscription);
    eventsOutboxRepository.save(createSubscriptionsOutboxEvent(updatedSubscription, "SubscriptionUpdated"));

    return SubscriptionResponse.fromSubscription(updatedSubscription);
  }

  @Transactional
  @Caching(
          evict = {
                  @CacheEvict(key = "#id"),
                  @CacheEvict(cacheNames = "subscriptionsByUser", key = "#result.userId")
          }
  )
  public SubscriptionDeleteResponse deleteSubscriptionById(String id) {
    Subscription subscription = subscriptionRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new SubscriptionNotFoundException(
                    "Subscription not found with Id: %s".formatted(id), id));

    subscriptionRepository.deleteById(UUID.fromString(id));
    eventsOutboxRepository.save(createSubscriptionsOutboxEvent(subscription, "SubscriptionDeleted"));

    return SubscriptionDeleteResponse.fromSubscriptionAndUserId(subscription.getUser().getId().toString(), id);
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
