package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.dto.user.UserSubscriptionsResponse;
import com.example.fx.subscription.service.dto.user.UserUpdateRequest;
import com.example.fx.subscription.service.exception.UserNotFoundException;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FxUsersService {

  private static final String USER_NOT_FOUND_MESSAGE = "User not found with ID: ";

  private final FxUserRepository fxUserRepository;
  private final SubscriptionRepository subscriptionRepository;

  public FxUsersService(FxUserRepository fxUserRepository, SubscriptionRepository subscriptionRepository) {
    this.fxUserRepository = fxUserRepository;
    this.subscriptionRepository = subscriptionRepository;
  }

  public Page<FxUser> findAllUsers(Pageable pageable) {
    return fxUserRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Optional<FxUser> findUserById(String id) {
    return fxUserRepository.findByIdWithSubscriptions(UUID.fromString(id));
  }

  public Page<FxUser> searchUsers(String email, String mobile, boolean enabled, Pageable pageable) {
    return fxUserRepository.searchUsers(email, mobile, enabled, pageable);
  }

  @Transactional
  public FxUser updateUser(String id, UserUpdateRequest userUpdateRequest) {
    FxUser user = fxUserRepository.findByIdWithSubscriptions(UUID.fromString(id))
            .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + id, id));

    if (StringUtils.hasText(userUpdateRequest.email())) {
      user.setEmail(userUpdateRequest.email());
    }
    
    if (StringUtils.hasText(userUpdateRequest.mobile())) {
      user.setMobile(userUpdateRequest.mobile());
    }
    
    if (StringUtils.hasText(userUpdateRequest.pushDeviceToken())) {
      user.setPushDeviceToken(userUpdateRequest.pushDeviceToken());
    }

    return fxUserRepository.save(user);
  }

  @Transactional
  public FxUser updateUserStatus(String id, boolean enabled) {
    FxUser user = fxUserRepository.findByIdWithSubscriptions(UUID.fromString(id))
            .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + id, id));

    user.setEnabled(enabled);
    return fxUserRepository.save(user);
  }

  @Transactional
  public void deleteUser(String id) {
    FxUser user = fxUserRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + id, id));

    // Check if user has active subscriptions
    List<Subscription> activeSubscriptions = subscriptionRepository.findAllByUserId(user.getId());
    if (!activeSubscriptions.isEmpty()) {
      throw new IllegalStateException("Cannot delete user with active subscriptions. Please delete subscriptions first.");
    }

    fxUserRepository.delete(user);
  }

  @Transactional(readOnly = true)
  public UserSubscriptionsResponse getUserSubscriptions(String userId) {
    FxUser user = fxUserRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + userId, userId));

    List<Subscription> subscriptions = subscriptionRepository.findAllByUserId(user.getId());
    
    List<SubscriptionResponse> subscriptionResponses =
            subscriptions.stream()
                    .map(SubscriptionResponse::fromSubscription)
                    .toList();

    return new UserSubscriptionsResponse(
            user.getId(),
            subscriptionResponses,
            subscriptions.size()
    );
  }
}
