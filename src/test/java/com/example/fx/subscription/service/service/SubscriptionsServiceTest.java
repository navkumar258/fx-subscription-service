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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionsServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private FxUserRepository fxUserRepository;

    @Mock
    private EventsOutboxRepository eventsOutboxRepository;

    @InjectMocks
    private SubscriptionsService subscriptionsService;

    private FxUser testUser;
    private Subscription testSubscription;
    private UUID testUserId;
    private UUID testSubscriptionId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testSubscriptionId = UUID.randomUUID();
        
        testUser = new FxUser();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setMobile("+1234567890");
        
        testSubscription = new Subscription();
        testSubscription.setId(testSubscriptionId);
        testSubscription.setUser(testUser);
        testSubscription.setCurrencyPair("GBP/USD");
        testSubscription.setThreshold(BigDecimal.valueOf(1.25));
        testSubscription.setDirection(ThresholdDirection.ABOVE);
        testSubscription.setNotificationsChannels(List.of("email", "sms"));
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);
        testSubscription.setCreatedAt(Instant.now());
        testSubscription.setUpdatedAt(Instant.now());
    }

    @Test
    void findSubscriptionById_WhenSubscriptionExists_ShouldReturnSubscriptionResponse() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

        // When
        Optional<SubscriptionResponse> result = subscriptionsService.findSubscriptionById(testSubscriptionId.toString());

        // Then
        assertTrue(result.isPresent());
        assertEquals(testSubscriptionId, result.get().id());
        assertEquals("GBP/USD", result.get().currencyPair());
        assertEquals(BigDecimal.valueOf(1.25), result.get().threshold());
        assertEquals(ThresholdDirection.ABOVE, result.get().direction());
        assertEquals(SubscriptionStatus.ACTIVE, result.get().status());
    }

    @Test
    void findSubscriptionById_WhenSubscriptionDoesNotExist_ShouldReturnEmpty() {
        // Given
        when(subscriptionRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        // When
        Optional<SubscriptionResponse> result = subscriptionsService.findSubscriptionById(UUID.randomUUID().toString());

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void findSubscriptionEntityById_WhenSubscriptionExists_ShouldReturnSubscription() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

        // When
        Optional<Subscription> result = subscriptionsService.findSubscriptionEntityById(testSubscriptionId.toString());

        // Then
        assertTrue(result.isPresent());
        assertEquals(testSubscription, result.get());
    }

    @Test
    void findSubscriptionsByUserId_ShouldReturnUserSubscriptions() {
        // Given
        List<Subscription> subscriptions = List.of(testSubscription);
        when(subscriptionRepository.findAllByUserId(testUserId))
                .thenReturn(subscriptions);

        // When
        List<Subscription> result = subscriptionsService.findSubscriptionsByUserId(testUserId.toString());

        // Then
        assertEquals(1, result.size());
        assertEquals(testSubscription, result.getFirst());
    }

    @Test
    void findSubscriptionResponsesByUserId_ShouldReturnSubscriptionResponses() {
        // Given
        List<Subscription> subscriptions = List.of(testSubscription);
        when(subscriptionRepository.findAllByUserId(testUserId))
                .thenReturn(subscriptions);

        // When
        List<SubscriptionResponse> result = subscriptionsService.findSubscriptionResponsesByUserId(testUserId.toString());

        // Then
        assertEquals(1, result.size());
        assertEquals(testSubscriptionId, result.getFirst().id());
        assertEquals("GBP/USD", result.getFirst().currencyPair());
    }

    @Test
    void findAllSubscriptionResponses_ShouldReturnAllSubscriptions() {
        // Given
        List<Subscription> subscriptions = List.of(testSubscription);
        when(subscriptionRepository.findAll())
                .thenReturn(subscriptions);

        // When
        List<SubscriptionResponse> result = subscriptionsService.findAllSubscriptionResponses();

        // Then
        assertEquals(1, result.size());
        assertEquals(testSubscriptionId, result.getFirst().id());
    }

    @Test
    void isSubscriptionOwner_WhenUserIsOwner_ShouldReturnTrue() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

        // When
        boolean result = subscriptionsService.isSubscriptionOwner(testSubscriptionId.toString(), testUserId);

        // Then
        assertTrue(result);
    }

    @Test
    void isSubscriptionOwner_WhenUserIsNotOwner_ShouldReturnFalse() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

        // When
        boolean result = subscriptionsService.isSubscriptionOwner(testSubscriptionId.toString(), UUID.randomUUID());

        // Then
        assertFalse(result);
    }

    @Test
    void isSubscriptionOwner_WhenSubscriptionDoesNotExist_ShouldReturnFalse() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.empty());

        // When
        boolean result = subscriptionsService.isSubscriptionOwner(testSubscriptionId.toString(), testUserId);

        // Then
        assertFalse(result);
    }

    @Test
    void createSubscription_WhenUserExists_ShouldCreateAndReturnSubscription() {
        // Given
        SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
                "EUR/USD",
                BigDecimal.valueOf(1.10),
                "BELOW",
                List.of("email")
        );

        when(fxUserRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser));
        when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
                .thenReturn(testSubscription);
        when(eventsOutboxRepository.save(any(EventsOutbox.class)))
                .thenReturn(new EventsOutbox());

        // When
        Subscription result = subscriptionsService.createSubscription(createRequest, testUserId);

        // Then
        assertNotNull(result);
        assertEquals(testSubscriptionId, result.getId());
        verify(subscriptionRepository).saveAndFlush(any(Subscription.class));
        verify(eventsOutboxRepository).save(any(EventsOutbox.class));
    }

    @Test
    void createSubscription_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
        // Given
        SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
                "EUR/USD",
                BigDecimal.valueOf(1.10),
                "BELOW",
                List.of("email")
        );

        when(fxUserRepository.findById(testUserId))
                .thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, 
                () -> subscriptionsService.createSubscription(createRequest, testUserId));
        assertTrue(exception.getMessage().contains("User not found with ID: " + testUserId));
    }

    @Test
    void updateSubscriptionById_ShouldUpdateAndReturnSubscription() {
        // Given
        SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
                "EUR/USD",
                BigDecimal.valueOf(1.15),
                "BELOW",
                "ACTIVE",
                List.of("sms")
        );

        when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
                .thenReturn(testSubscription);
        when(eventsOutboxRepository.save(any(EventsOutbox.class)))
                .thenReturn(new EventsOutbox());

        // When
        Subscription result = subscriptionsService.updateSubscriptionById(testSubscription, updateRequest);

        // Then
        assertNotNull(result);
        verify(subscriptionRepository).saveAndFlush(testSubscription);
        verify(eventsOutboxRepository).save(any(EventsOutbox.class));
    }

    @Test
    void updateSubscriptionById_WithNullValues_ShouldOnlyUpdateProvidedFields() {
        // Given
        SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
                testUserId.toString(),
                BigDecimal.valueOf(1.15),
                null,
                null,
                null
        );

        when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
                .thenReturn(testSubscription);
        when(eventsOutboxRepository.save(any(EventsOutbox.class)))
                .thenReturn(new EventsOutbox());

        // When
        Subscription result = subscriptionsService.updateSubscriptionById(testSubscription, updateRequest);

        // Then
        assertNotNull(result);
        verify(subscriptionRepository).saveAndFlush(testSubscription);
        verify(eventsOutboxRepository).save(any(EventsOutbox.class));
    }

    @Test
    void deleteSubscriptionById_WhenSubscriptionExists_ShouldDeleteSuccessfully() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));
        when(eventsOutboxRepository.save(any(EventsOutbox.class)))
                .thenReturn(new EventsOutbox());

        // When
        subscriptionsService.deleteSubscriptionById(testSubscriptionId.toString());

        // Then
        verify(subscriptionRepository).deleteById(testSubscriptionId);
        verify(eventsOutboxRepository).save(any(EventsOutbox.class));
    }

    @Test
    void deleteSubscriptionById_WhenSubscriptionDoesNotExist_ShouldThrowSubscriptionNotFoundException() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.empty());

        // When & Then
        SubscriptionNotFoundException exception = assertThrows(SubscriptionNotFoundException.class,
                () -> subscriptionsService.deleteSubscriptionById(testSubscriptionId.toString()));
        assertTrue(exception.getMessage().contains("Subscription not found with ID: " + testSubscriptionId));
    }
} 