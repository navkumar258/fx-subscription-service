package com.example.fx.subscription.service.config;

import com.example.fx.subscription.service.service.FxUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

  private static final String TEST_USER = "testuser";
  private static final String USER = "USER";
  private static final String AUTHORIZATION = "Authorization";

  @Mock
  private FxUserDetailsService fxUserDetailsService;

  @Mock
  private HttpServletRequest request;

  private JwtTokenProvider jwtTokenProvider;

  private static final String SECRET_KEY = "/yq8RuTZgYPfpX4XdPYsy9DohY9EAFg+qQ6iNENWZOXHkLLhPqIYbhtQfEesQvdcjg9RvOjHb30N7/PlHlnL3w==";
  private static final long VALIDITY_IN_MILLISECONDS = 60000; // 1 hour

  @BeforeEach
  void setUp() {
    jwtTokenProvider = new JwtTokenProvider(fxUserDetailsService);
    ReflectionTestUtils.setField(jwtTokenProvider, "secret", SECRET_KEY);
    ReflectionTestUtils.setField(jwtTokenProvider, "validityInMilliseconds", VALIDITY_IN_MILLISECONDS);
    jwtTokenProvider.init();
  }

  @Test
  void createToken_WithValidUsernameAndRoles_ShouldCreateValidToken() {
    // Given
    Set<String> roles = Set.of(USER, "ADMIN");

    // When
    String token = jwtTokenProvider.createToken(TEST_USER, roles);

    // Then
    assertNotNull(token);
    assertFalse(token.isEmpty());

    // Verify token can be parsed and contains correct claims
    String extractedUsername = jwtTokenProvider.getUsername(token);
    assertEquals(TEST_USER, extractedUsername);

    // Verify token is valid
    assertTrue(jwtTokenProvider.validateToken(token));
  }

  @Test
  void createToken_WithEmptyRoles_ShouldCreateValidToken() {
    // Given
    Set<String> roles = new HashSet<>();

    // When
    String token = jwtTokenProvider.createToken(TEST_USER, roles);

    // Then
    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertEquals(TEST_USER, jwtTokenProvider.getUsername(token));
    assertTrue(jwtTokenProvider.validateToken(token));
  }

  @Test
  void createToken_WithNullRoles_ShouldCreateValidToken() {
    // When
    String token = jwtTokenProvider.createToken(TEST_USER, null);

    // Then
    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertEquals(TEST_USER, jwtTokenProvider.getUsername(token));
    assertTrue(jwtTokenProvider.validateToken(token));
  }

  @Test
  void getAuthentication_WithValidToken_ShouldReturnAuthentication() {
    // Given
    Set<String> roles = Set.of(USER);
    String token = jwtTokenProvider.createToken(TEST_USER, roles);

    UserDetails userDetails = User.builder()
            .username(TEST_USER)
            .password("password")
            .authorities(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList())
            .build();

    when(fxUserDetailsService.loadUserByUsername(TEST_USER)).thenReturn(userDetails);

    // When
    Authentication authentication = jwtTokenProvider.getAuthentication(token);

    // Then
    assertNotNull(authentication);
    assertInstanceOf(UsernamePasswordAuthenticationToken.class, authentication);
    assertEquals(TEST_USER, authentication.getName());
    assertEquals(userDetails, authentication.getPrincipal());
    assertTrue(authentication.isAuthenticated());
  }

  @Test
  void resolveToken_WithValidBearerToken_ShouldReturnToken() {
    // Given
    String bearerToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature";
    when(request.getHeader(AUTHORIZATION)).thenReturn(bearerToken);

    // When
    String token = jwtTokenProvider.resolveToken(request);

    // Then
    assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature", token);
  }

  @Test
  void resolveToken_WithInvalidBearerToken_ShouldReturnNull() {
    // Given
    String invalidToken = "InvalidToken";
    when(request.getHeader(AUTHORIZATION)).thenReturn(invalidToken);

    // When
    String token = jwtTokenProvider.resolveToken(request);

    // Then
    assertNull(token);
  }

  @Test
  void resolveToken_WithNullHeader_ShouldReturnNull() {
    // Given
    when(request.getHeader(AUTHORIZATION)).thenReturn(null);

    // When
    String token = jwtTokenProvider.resolveToken(request);

    // Then
    assertNull(token);
  }

  @Test
  void resolveToken_WithEmptyHeader_ShouldReturnNull() {
    // Given
    when(request.getHeader(AUTHORIZATION)).thenReturn("");

    // When
    String token = jwtTokenProvider.resolveToken(request);

    // Then
    assertNull(token);
  }

  @Test
  void validateToken_WithValidToken_ShouldReturnTrue() {
    // Given
    Set<String> roles = Set.of(USER);
    String token = jwtTokenProvider.createToken(TEST_USER, roles);

    // When
    boolean isValid = jwtTokenProvider.validateToken(token);

    // Then
    assertTrue(isValid);
  }

  @Test
  void validateToken_WithInvalidToken_ShouldReturnFalse() {
    // Given
    String invalidToken = "invalid.token.here";

    // When & Then
    assertFalse(jwtTokenProvider.validateToken(invalidToken));
  }

  @Test
  void validateToken_WithExpiredToken_ShouldReturnFalse() {
    // Given - Create a token with very short validity
    JwtTokenProvider shortLivedProvider = new JwtTokenProvider(fxUserDetailsService);
    ReflectionTestUtils.setField(shortLivedProvider, "secret", SECRET_KEY);
    ReflectionTestUtils.setField(shortLivedProvider, "validityInMilliseconds", 1L); // 1 millisecond
    shortLivedProvider.init();

    Set<String> roles = Set.of(USER);
    String token = shortLivedProvider.createToken(TEST_USER, roles);

    // Wait for token to expire
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // When & Then
    assertFalse(jwtTokenProvider.validateToken(token));
  }

  @Test
  void getUsername_WithValidToken_ShouldReturnUsername() {
    // Given
    Set<String> roles = Set.of(USER);
    String token = jwtTokenProvider.createToken(TEST_USER, roles);

    // When
    String extractedUsername = jwtTokenProvider.getUsername(token);

    // Then
    assertEquals(TEST_USER, extractedUsername);
  }

  @Test
  void getUsername_WithInvalidToken_ShouldThrowJwtException() {
    // Given
    String invalidToken = "invalid.token.here";

    // When & Then
    assertThrows(JwtException.class, () -> jwtTokenProvider.getUsername(invalidToken));
  }

  @Test
  void createToken_WithDifferentUsernames_ShouldCreateDifferentTokens() {
    // Given
    String username1 = "user1";
    String username2 = "user2";
    Set<String> roles = Set.of(USER);

    // When
    String token1 = jwtTokenProvider.createToken(username1, roles);
    String token2 = jwtTokenProvider.createToken(username2, roles);

    // Then
    assertNotEquals(token1, token2);
    assertEquals(username1, jwtTokenProvider.getUsername(token1));
    assertEquals(username2, jwtTokenProvider.getUsername(token2));
  }

  @Test
  void createToken_WithSameUsernameAndRoles_ShouldCreateValidTokens() {
    // Given
    Set<String> roles = Set.of(USER);

    // When
    String token1 = jwtTokenProvider.createToken(TEST_USER, roles);
    String token2 = jwtTokenProvider.createToken(TEST_USER, roles);

    // Then - both tokens should be valid and secure
    assertTrue(jwtTokenProvider.validateToken(token1));
    assertTrue(jwtTokenProvider.validateToken(token2));
    assertEquals(TEST_USER, jwtTokenProvider.getUsername(token1));
    assertEquals(TEST_USER, jwtTokenProvider.getUsername(token2));

    // Security test: Verify tokens cannot be tampered with
    String tamperedToken = token1.substring(0, token1.length() - 10) + "tampered";
    assertFalse(jwtTokenProvider.validateToken(tamperedToken));
  }
}