package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.model.EventsOutbox;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventsOutboxServiceTest {

  @Mock
  private EventsOutboxRepository eventsOutboxRepository;

  @InjectMocks
  private EventsOutboxService eventsOutboxService;

  private EventsOutbox testOutbox;
  private UUID testId;
  private String testIdString;

  @BeforeEach
  void setUp() {
    testId = UUID.randomUUID();
    testIdString = testId.toString();

    testOutbox = new EventsOutbox();
    testOutbox.setId(testId);
    testOutbox.setAggregateType("Subscription");
    testOutbox.setAggregateId(UUID.randomUUID());
    testOutbox.setEventType("SubscriptionCreated");
    testOutbox.setPayload(createTestSubscriptionResponse());
    testOutbox.setStatus("PENDING");
    testOutbox.setTimestamp(System.currentTimeMillis());
  }

  @Test
  void updateOutboxStatus_WhenOutboxExists_ShouldUpdateStatusSuccessfully() {
    // Given
    String newStatus = "SENT";
    when(eventsOutboxRepository.findById(testId)).thenReturn(Optional.of(testOutbox));
    when(eventsOutboxRepository.save(any(EventsOutbox.class))).thenReturn(testOutbox);

    // When
    eventsOutboxService.updateOutboxStatus(testIdString, newStatus);

    // Then
    verify(eventsOutboxRepository).findById(testId);
    verify(eventsOutboxRepository).save(argThat(outbox ->
            outbox.getId().equals(testId) &&
                    newStatus.equals(outbox.getStatus())
    ));
  }

  @Test
  void updateOutboxStatus_WhenOutboxDoesNotExist_ShouldThrowRuntimeException() {
    // Given
    String newStatus = "SENT";
    when(eventsOutboxRepository.findById(testId)).thenReturn(Optional.empty());

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () ->
            eventsOutboxService.updateOutboxStatus(testIdString, newStatus)
    );

    assertEquals("Outbox event not found with id: " + testIdString, exception.getMessage());
    verify(eventsOutboxRepository).findById(testId);
    verify(eventsOutboxRepository, never()).save(any(EventsOutbox.class));
  }

  @Test
  void updateOutboxStatus_WhenInvalidUUID_ShouldThrowIllegalArgumentException() {
    // Given
    String invalidId = "invalid-uuid";
    String newStatus = "SENT";

    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
            eventsOutboxService.updateOutboxStatus(invalidId, newStatus)
    );

    verify(eventsOutboxRepository, never()).findById(any());
    verify(eventsOutboxRepository, never()).save(any(EventsOutbox.class));
  }

  @Test
  void updateOutboxStatus_WhenNullId_ShouldThrowNullPointerException() {
    // Given
    String newStatus = "SENT";

    // When & Then
    assertThrows(NullPointerException.class, () ->
            eventsOutboxService.updateOutboxStatus(null, newStatus)
    );

    verify(eventsOutboxRepository, never()).findById(any());
    verify(eventsOutboxRepository, never()).save(any(EventsOutbox.class));
  }

  @Test
  void updateOutboxStatus_WhenNullStatus_ShouldUpdateWithNullStatus() {
    // Given
    when(eventsOutboxRepository.findById(testId)).thenReturn(Optional.of(testOutbox));
    when(eventsOutboxRepository.save(any(EventsOutbox.class))).thenReturn(testOutbox);

    // When
    eventsOutboxService.updateOutboxStatus(testIdString, null);

    // Then
    verify(eventsOutboxRepository).findById(testId);
    verify(eventsOutboxRepository).save(argThat(outbox ->
            outbox.getId().equals(testId) &&
                    outbox.getStatus() == null
    ));
  }

  @Test
  void updateOutboxStatus_WhenEmptyStatus_ShouldUpdateWithEmptyStatus() {
    // Given
    String emptyStatus = "";
    when(eventsOutboxRepository.findById(testId)).thenReturn(Optional.of(testOutbox));
    when(eventsOutboxRepository.save(any(EventsOutbox.class))).thenReturn(testOutbox);

    // When
    eventsOutboxService.updateOutboxStatus(testIdString, emptyStatus);

    // Then
    verify(eventsOutboxRepository).findById(testId);
    verify(eventsOutboxRepository).save(argThat(outbox ->
            outbox.getId().equals(testId) &&
                    emptyStatus.equals(outbox.getStatus())
    ));
  }

  @Test
  void findOutboxById_WhenOutboxExists_ShouldReturnOutbox() {
    // Given
    when(eventsOutboxRepository.findById(testId)).thenReturn(Optional.of(testOutbox));

    // When
    EventsOutbox result = eventsOutboxService.findOutboxById(testIdString);

    // Then
    assertNotNull(result);
    assertEquals(testOutbox, result);
    verify(eventsOutboxRepository).findById(testId);
  }

  @Test
  void findOutboxById_WhenOutboxDoesNotExist_ShouldThrowRuntimeException() {
    // Given
    when(eventsOutboxRepository.findById(testId)).thenReturn(Optional.empty());

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () ->
            eventsOutboxService.findOutboxById(testIdString)
    );

    assertEquals("Outbox event not found with id: " + testIdString, exception.getMessage());
    verify(eventsOutboxRepository).findById(testId);
  }

  @Test
  void findOutboxById_WhenInvalidUUID_ShouldThrowIllegalArgumentException() {
    // Given
    String invalidId = "invalid-uuid";

    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
            eventsOutboxService.findOutboxById(invalidId)
    );

    verify(eventsOutboxRepository, never()).findById(any());
  }

  @Test
  void findOutboxById_WhenNullId_ShouldThrowNullPointerException() {
    // When & Then
    assertThrows(NullPointerException.class, () ->
            eventsOutboxService.findOutboxById(null)
    );

    verify(eventsOutboxRepository, never()).findById(any());
  }

  @Test
  void updateOutboxStatus_ShouldPreserveOtherFields() {
    // Given
    String newStatus = "FAILED";
    when(eventsOutboxRepository.findById(testId)).thenReturn(Optional.of(testOutbox));
    when(eventsOutboxRepository.save(any(EventsOutbox.class))).thenReturn(testOutbox);

    // When
    eventsOutboxService.updateOutboxStatus(testIdString, newStatus);

    // Then
    verify(eventsOutboxRepository).save(argThat(outbox ->
            outbox.getId().equals(testId) &&
                    outbox.getAggregateType().equals(testOutbox.getAggregateType()) &&
                    outbox.getAggregateId().equals(testOutbox.getAggregateId()) &&
                    outbox.getEventType().equals(testOutbox.getEventType()) &&
                    outbox.getPayload().equals(testOutbox.getPayload()) &&
                    outbox.getTimestamp() == testOutbox.getTimestamp() &&
                    newStatus.equals(outbox.getStatus())
    ));
  }

  @Test
  void updateOutboxStatus_WhenRepositorySaveFails_ShouldPropagateException() {
    // Given
    String newStatus = "SENT";
    RuntimeException saveException = new RuntimeException("Database connection failed");

    when(eventsOutboxRepository.findById(testId)).thenReturn(Optional.of(testOutbox));
    when(eventsOutboxRepository.save(any(EventsOutbox.class))).thenThrow(saveException);

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () ->
            eventsOutboxService.updateOutboxStatus(testIdString, newStatus)
    );

    assertEquals("Database connection failed", exception.getMessage());
    verify(eventsOutboxRepository).findById(testId);
    verify(eventsOutboxRepository).save(any(EventsOutbox.class));
  }

  @Test
  void findOutboxById_ShouldReturnCorrectOutbox() {
    // When
    when(eventsOutboxRepository.findById(testId)).thenReturn(Optional.of(testOutbox));

    EventsOutbox result = eventsOutboxService.findOutboxById(testIdString);

    // Then
    assertNotNull(result);
    assertEquals(testOutbox.getId(), result.getId());
    assertEquals(testOutbox.getStatus(), result.getStatus());
    assertEquals(testOutbox.getAggregateType(), result.getAggregateType());
    assertEquals(testOutbox.getEventType(), result.getEventType());
    assertEquals(testOutbox.getPayload(), result.getPayload());
    assertEquals(testOutbox.getTimestamp(), result.getTimestamp());
  }

  private SubscriptionResponse createTestSubscriptionResponse() {
    Subscription subscription = new Subscription();
    subscription.setId(UUID.randomUUID());
    subscription.setCurrencyPair("GBP/USD");
    subscription.setThreshold(BigDecimal.valueOf(1.25));
    subscription.setDirection(ThresholdDirection.ABOVE);
    subscription.setNotificationsChannels(List.of("email", "sms"));
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    return SubscriptionResponse.fromSubscription(subscription);
  }
}