package com.example.fx.subscription.service.config;

import com.example.fx.subscription.service.service.FxUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterTest {

    @Mock
    private FxUserDetailsService fxUserDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    private JwtTokenProvider jwtTokenProvider;
    private JwtTokenFilter jwtTokenFilter;

    // Using the same values from test application.properties
    private static final String SECRET_KEY = "/yq8RuTZgYPfpX4XdPYsy9DohY9EAFg+qQ6iNENWZOXHkLLhPqIYbhtQfEesQvdcjg9RvOjHb30N7/PlHlnL3w==";
    private static final long VALIDITY_IN_MILLISECONDS = 60000; // 60 seconds

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(fxUserDetailsService);
        ReflectionTestUtils.setField(jwtTokenProvider, "secret", SECRET_KEY);
        ReflectionTestUtils.setField(jwtTokenProvider, "validityInMilliseconds", VALIDITY_IN_MILLISECONDS);
        jwtTokenProvider.init();
        
        jwtTokenFilter = new JwtTokenFilter(jwtTokenProvider);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void doFilterInternal_WithValidToken_ShouldSetAuthentication() throws ServletException, IOException {
        // Given
        String username = "testuser";
        Set<String> roles = Set.of("USER");
        String validToken = jwtTokenProvider.createToken(username, roles);
        String bearerToken = "Bearer " + validToken;
        
        UserDetails userDetails = User.builder()
                .username(username)
                .password("password")
                .authorities(roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList())
                .build();

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(fxUserDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(fxUserDetailsService).loadUserByUsername(username);
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithNullToken_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(fxUserDetailsService, never()).loadUserByUsername(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithEmptyHeader_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("");

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(fxUserDetailsService, never()).loadUserByUsername(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidBearerToken_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Given
        String invalidBearerToken = "InvalidToken";
        when(request.getHeader("Authorization")).thenReturn(invalidBearerToken);

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(fxUserDetailsService, never()).loadUserByUsername(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidToken_ShouldReturnFalse()
            throws ServletException, IOException {
        // Given
        String invalidToken = "invalid.jwt.token";
        String bearerToken = "Bearer " + invalidToken;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);

        // When & Then
        jwtTokenFilter.doFilterInternal(request, response, filterChain);
        
        verify(fxUserDetailsService, never()).loadUserByUsername(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithExpiredToken_ShouldReturnFalse()
            throws ServletException, IOException {
        // Given - Create a token with very short validity
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(fxUserDetailsService);
        ReflectionTestUtils.setField(shortLivedProvider, "secret", SECRET_KEY);
        ReflectionTestUtils.setField(shortLivedProvider, "validityInMilliseconds", 1L); // 1 millisecond
        shortLivedProvider.init();
        
        String username = "testuser";
        Set<String> roles = Set.of("USER");
        String expiredToken = shortLivedProvider.createToken(username, roles);
        String bearerToken = "Bearer " + expiredToken;
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        when(request.getHeader("Authorization")).thenReturn(bearerToken);

        // When & Then
        jwtTokenFilter.doFilterInternal(request, response, filterChain);
        
        verify(fxUserDetailsService, never()).loadUserByUsername(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithValidTokenButUserNotFound_ShouldThrowRuntimeException()
            throws ServletException, IOException {
        // Given
        String username = "nonexistentuser";
        Set<String> roles = Set.of("USER");
        String validToken = jwtTokenProvider.createToken(username, roles);
        String bearerToken = "Bearer " + validToken;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(fxUserDetailsService.loadUserByUsername(username)).thenThrow(new RuntimeException("User not found"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            try {
                jwtTokenFilter.doFilterInternal(request, response, filterChain);
            } catch (ServletException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        
        verify(fxUserDetailsService).loadUserByUsername(username);
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithFilterChainException_ShouldPropagateException() throws ServletException, IOException {
        // Given
        String username = "testuser";
        Set<String> roles = Set.of("USER");
        String validToken = jwtTokenProvider.createToken(username, roles);
        String bearerToken = "Bearer " + validToken;
        
        UserDetails userDetails = User.builder()
                .username(username)
                .password("password")
                .authorities(roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList())
                .build();

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(fxUserDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        doThrow(new ServletException("Filter chain error")).when(filterChain).doFilter(request, response);

        // When & Then
        assertThrows(ServletException.class, () -> 
            jwtTokenFilter.doFilterInternal(request, response, filterChain));
        
        verify(fxUserDetailsService).loadUserByUsername(username);
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithIOException_ShouldPropagateException() throws ServletException, IOException {
        // Given
        String username = "testuser";
        Set<String> roles = Set.of("USER");
        String validToken = jwtTokenProvider.createToken(username, roles);
        String bearerToken = "Bearer " + validToken;
        
        UserDetails userDetails = User.builder()
                .username(username)
                .password("password")
                .authorities(roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList())
                .build();

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(fxUserDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        doThrow(new IOException("IO error")).when(filterChain).doFilter(request, response);

        // When & Then
        assertThrows(IOException.class, () -> 
            jwtTokenFilter.doFilterInternal(request, response, filterChain));
        
        verify(fxUserDetailsService).loadUserByUsername(username);
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithDifferentUserRoles_ShouldSetCorrectAuthentication() throws ServletException, IOException {
        // Given
        String username = "admin";
        Set<String> roles = Set.of("ADMIN", "USER");
        String validToken = jwtTokenProvider.createToken(username, roles);
        String bearerToken = "Bearer " + validToken;
        
        UserDetails adminUserDetails = User.builder()
                .username(username)
                .password("password")
                .authorities(roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList())
                .build();

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(fxUserDetailsService.loadUserByUsername(username)).thenReturn(adminUserDetails);

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(fxUserDetailsService).loadUserByUsername(username);
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }
} 