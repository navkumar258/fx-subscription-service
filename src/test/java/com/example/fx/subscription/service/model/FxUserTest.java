package com.example.fx.subscription.service.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FxUserTest {

  @Mock
  private Subscription mockSubscription;

  private FxUser fxUser;
  private UUID testId;

  @BeforeEach
  void setUp() {
    testId = UUID.randomUUID();
    fxUser = new FxUser();
    fxUser.setId(testId);
    fxUser.setEmail("test@example.com");
    fxUser.setMobile("+1234567890");
    fxUser.setPassword("password123");
    fxUser.setEnabled(true);
    fxUser.setRole(UserRole.USER);
    fxUser.setPushDeviceToken("device-token-123");
  }

  @Test
  void constructor_ShouldCreateUserWithId() {
    // When
    FxUser user = new FxUser(testId);

    // Then
    assertEquals(testId, user.getId());
  }

  @Test
  void constructor_ShouldCreateUserWithParameters() {
    // When
    FxUser user = new FxUser(testId, "user@example.com", "password", UserRole.ADMIN);

    // Then
    assertEquals(testId, user.getId());
    assertEquals("user@example.com", user.getEmail());
    assertEquals("password", user.getPassword());
    assertEquals(UserRole.ADMIN, user.getRole());
  }

  @Test
  void getAuthorities_ShouldReturnRoleBasedAuthority() {
    // When
    Collection<? extends GrantedAuthority> authorities = fxUser.getAuthorities();

    // Then
    assertEquals(1, authorities.size());
    SimpleGrantedAuthority authority = (SimpleGrantedAuthority) authorities.iterator().next();
    assertEquals("ROLE_USER", authority.getAuthority());
  }

  @Test
  void getAuthorities_ShouldReturnCorrectRoleForAdmin() {
    // Given
    fxUser.setRole(UserRole.ADMIN);

    // When
    Collection<? extends GrantedAuthority> authorities = fxUser.getAuthorities();

    // Then
    assertEquals(1, authorities.size());
    SimpleGrantedAuthority authority = (SimpleGrantedAuthority) authorities.iterator().next();
    assertEquals("ROLE_ADMIN", authority.getAuthority());
  }

  @Test
  void getUsername_ShouldReturnEmail() {
    // When
    String username = fxUser.getUsername();

    // Then
    assertEquals("test@example.com", username);
  }

  @Test
  void isEnabled_ShouldReturnUserEnabledStatus() {
    // Given
    fxUser.setEnabled(false);

    // When & Then
    assertFalse(fxUser.isEnabled());

    // Given
    fxUser.setEnabled(true);

    // When & Then
    assertTrue(fxUser.isEnabled());
  }

  @Test
  void addSubscription_ShouldAddToSubscriptionsAndSetUser() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    Subscription newSubscription = new Subscription();
    newSubscription.setId(subscriptionId);
    int initialSize = fxUser.getSubscriptions().size();

    // When
    fxUser.addSubscription(newSubscription);

    // Then
    assertEquals(initialSize + 1, fxUser.getSubscriptions().size());
    assertTrue(fxUser.getSubscriptions().contains(newSubscription));
    assertEquals(fxUser, newSubscription.getUser());
  }

  @Test
  void removeSubscription_ShouldRemoveFromSubscriptionsAndClearUser() {
    // Given
    Subscription subscriptionToRemove = new Subscription();
    subscriptionToRemove.setId(UUID.randomUUID());
    fxUser.addSubscription(subscriptionToRemove);
    int sizeAfterAdd = fxUser.getSubscriptions().size();

    // When
    fxUser.removeSubscription(subscriptionToRemove);

    // Then
    assertEquals(sizeAfterAdd - 1, fxUser.getSubscriptions().size());
    assertFalse(fxUser.getSubscriptions().contains(subscriptionToRemove));
    assertNull(subscriptionToRemove.getUser());
  }

  @Test
  void removeSubscription_WhenSubscriptionNotInList_ShouldNotThrowException() {
    // Given
    Subscription nonExistentSubscription = new Subscription();
    nonExistentSubscription.setId(UUID.randomUUID());
    int initialSize = fxUser.getSubscriptions().size();

    // When & Then
    assertDoesNotThrow(() -> fxUser.removeSubscription(nonExistentSubscription));
    assertEquals(initialSize, fxUser.getSubscriptions().size());
  }

  @Test
  void equals_WhenSameValues_ShouldReturnTrue() {
    // Given
    FxUser other = new FxUser();
    other.setId(testId);
    other.setEmail("test@example.com");
    other.setMobile("+1234567890");
    other.setPushDeviceToken("device-token-123");

    // When & Then
    assertEquals(fxUser, other);
  }

  @Test
  void equals_WhenDifferentValues_ShouldReturnFalse() {
    // Given
    FxUser other = new FxUser();
    other.setId(UUID.randomUUID()); // Different ID

    // When & Then
    assertNotEquals(fxUser, other);
  }

  @Test
  void hashCode_WhenSameValues_ShouldReturnSameHashCode() {
    // Given
    FxUser other = new FxUser();
    other.setId(testId);
    other.setEmail("test@example.com");
    other.setMobile("+1234567890");
    other.setPushDeviceToken("device-token-123");

    // When & Then
    assertEquals(fxUser.hashCode(), other.hashCode());
  }

  @Test
  void hashCode_WhenDifferentValues_ShouldReturnDifferentHashCode() {
    // Given
    FxUser other = new FxUser();
    other.setId(UUID.randomUUID()); // Different ID

    // When & Then
    assertNotEquals(fxUser.hashCode(), other.hashCode());
  }

  @Test
  void toString_WithNullValues_ShouldHandleNullsGracefully() {
    // Given
    FxUser userWithNulls = new FxUser();
    userWithNulls.setId(testId);
    // Leave other fields as null

    // When
    String result = userWithNulls.toString();

    // Then
    assertTrue(result.contains("id=" + testId));
    assertTrue(result.contains("email='null'"));
    assertTrue(result.contains("mobile='null'"));
    assertTrue(result.contains("pushDeviceToken='null'"));
  }

  @Test
  void toString_ShouldContainAllFields() {
    // When
    String result = fxUser.toString();

    // Then
    assertTrue(result.contains("id=" + testId));
    assertTrue(result.contains("email='test@example.com'"));
    assertTrue(result.contains("mobile='+1234567890'"));
    assertTrue(result.contains("pushDeviceToken='device-token-123'"));
  }
}
