package com.example.fx.subscription.service.config;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSecurityConfigTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationConfiguration authConfig;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private HttpSecurity httpSecurity;

    @Mock
    private SecurityFilterChain securityFilterChain;

    private WebSecurityConfig webSecurityConfig;

    @BeforeEach
    void setUp() {
        webSecurityConfig = new WebSecurityConfig(jwtTokenProvider);
    }

    @Test
    void securityFilterChain_ShouldConfigureSecurityCorrectly() throws Exception {
        // Given
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.headers(any())).thenReturn(httpSecurity);
        when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
        when(httpSecurity.exceptionHandling(any())).thenReturn(httpSecurity);
        when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
        when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
        when(httpSecurity.redirectToHttps(any())).thenReturn(httpSecurity);
        doReturn(securityFilterChain).when(httpSecurity).build();

        // When
        SecurityFilterChain result = webSecurityConfig.securityFilterChain(httpSecurity);

        // Then
        assertNotNull(result);
        assertEquals(securityFilterChain, result);
        
        // Verify all security configurations were applied
        verify(httpSecurity).csrf(any());
        verify(httpSecurity).headers(any());
        verify(httpSecurity).sessionManagement(any());
        verify(httpSecurity).exceptionHandling(any());
        verify(httpSecurity).authorizeHttpRequests(any());
        verify(httpSecurity).addFilterBefore(any(JwtTokenFilter.class), eq(UsernamePasswordAuthenticationFilter.class));
        verify(httpSecurity).redirectToHttps(any());
        verify(httpSecurity).build();
    }

    @Test
    void authenticationManager_ShouldReturnAuthenticationManager() throws Exception {
        // Given
        when(authConfig.getAuthenticationManager()).thenReturn(authenticationManager);

        // When
        AuthenticationManager result = webSecurityConfig.authenticationManager(authConfig);

        // Then
        assertNotNull(result);
        assertEquals(authenticationManager, result);
        verify(authConfig).getAuthenticationManager();
    }

    @Test
    void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
        // When
        PasswordEncoder result = webSecurityConfig.passwordEncoder();

        // Then
        assertNotNull(result);
        assertInstanceOf(BCryptPasswordEncoder.class, result);
    }

    @Test
    void passwordEncoder_ShouldEncodeAndMatchPasswords() {
        // Given
        PasswordEncoder passwordEncoder = webSecurityConfig.passwordEncoder();
        String rawPassword = "testPassword123";

        // When
        String encodedPassword = passwordEncoder.encode(rawPassword);
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // Then
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(matches);
    }

    @Test
    void passwordEncoder_ShouldNotMatchDifferentPasswords() {
        // Given
        PasswordEncoder passwordEncoder = webSecurityConfig.passwordEncoder();
        String password1 = "testPassword123";
        String password2 = "differentPassword456";

        // When
        String encodedPassword1 = passwordEncoder.encode(password1);
        boolean matches = passwordEncoder.matches(password2, encodedPassword1);

        // Then
        assertFalse(matches);
    }

    @Test
    void unauthorizedEntryPoint_ShouldReturnAuthenticationEntryPoint() {
        // When
        AuthenticationEntryPoint result = webSecurityConfig.unauthorizedEntryPoint();

        // Then
        assertNotNull(result);
    }

    @Test
    void unauthorizedEntryPoint_ShouldSendUnauthorizedResponse() throws Exception {
        // Given
        AuthenticationEntryPoint entryPoint = webSecurityConfig.unauthorizedEntryPoint();
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        org.springframework.security.core.AuthenticationException authException = 
            mock(org.springframework.security.core.AuthenticationException.class);

        // When
        entryPoint.commence(request, response, authException);

        // Then
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }

    @Test
    void constructor_ShouldInjectJwtTokenProvider() {
        // Given
        JwtTokenProvider injectedProvider = (JwtTokenProvider) ReflectionTestUtils.getField(webSecurityConfig, "jwtTokenProvider");

        // Then
        assertNotNull(injectedProvider);
        assertEquals(jwtTokenProvider, injectedProvider);
    }

    @Test
    void securityFilterChain_ShouldCreateJwtTokenFilter() throws Exception {
        // Given
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.headers(any())).thenReturn(httpSecurity);
        when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
        when(httpSecurity.exceptionHandling(any())).thenReturn(httpSecurity);
        when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
        when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
        when(httpSecurity.redirectToHttps(any())).thenReturn(httpSecurity);
        doReturn(securityFilterChain).when(httpSecurity).build();

        // When
        webSecurityConfig.securityFilterChain(httpSecurity);

        // Then
        verify(httpSecurity).addFilterBefore(any(JwtTokenFilter.class), eq(UsernamePasswordAuthenticationFilter.class));
    }

    @Test
    void passwordEncoder_ShouldGenerateDifferentEncodingsForSamePassword() {
        // Given
        PasswordEncoder passwordEncoder = webSecurityConfig.passwordEncoder();
        String rawPassword = "testPassword123";

        // When
        String encodedPassword1 = passwordEncoder.encode(rawPassword);
        String encodedPassword2 = passwordEncoder.encode(rawPassword);

        // Then
        assertNotEquals(encodedPassword1, encodedPassword2); // BCrypt generates different salts
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword1));
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword2));
    }

    @Test
    void passwordEncoder_ShouldHandleNullPassword() {
        // Given
        PasswordEncoder passwordEncoder = webSecurityConfig.passwordEncoder();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> passwordEncoder.encode(null));
        assertThrows(IllegalArgumentException.class, () -> passwordEncoder.matches(null, "encoded"));
        assertFalse(passwordEncoder.matches("raw", null));
    }

    @Test
    void passwordEncoder_ShouldHandleEmptyPassword() {
        // Given
        PasswordEncoder passwordEncoder = webSecurityConfig.passwordEncoder();
        String emptyPassword = "";

        // When
        String encodedPassword = passwordEncoder.encode(emptyPassword);
        boolean matches = passwordEncoder.matches(emptyPassword, encodedPassword);

        // Then
        assertNotNull(encodedPassword);
        assertTrue(matches);
    }

    @Test
    void authenticationManager_ShouldThrowExceptionWhenAuthConfigFails() throws Exception {
        // Given
        when(authConfig.getAuthenticationManager()).thenThrow(new RuntimeException("Auth config error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> webSecurityConfig.authenticationManager(authConfig));
        verify(authConfig).getAuthenticationManager();
    }

    @Test
    void securityFilterChain_ShouldThrowExceptionWhenHttpSecurityFails() throws Exception {
        // Given
        when(httpSecurity.csrf(any())).thenThrow(new RuntimeException("Security config error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> webSecurityConfig.securityFilterChain(httpSecurity));
        verify(httpSecurity).csrf(any());
    }
} 