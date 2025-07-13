package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.UserRole;
import com.example.fx.subscription.service.repository.FxUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FxUserDetailsServiceTest {

    @Mock
    private FxUserRepository fxUserRepository;

    @InjectMocks
    private FxUserDetailsService fxUserDetailsService;

    private FxUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new FxUser();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setMobile("+1234567890");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);
        testUser.setRole(UserRole.USER);
    }

    @Test
    void loadUserByUsername_WhenUserExists_ShouldReturnUserDetails() {
        // Given
        String username = "test@example.com";
        when(fxUserRepository.findByEmail(username))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails result = fxUserDetailsService.loadUserByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertEquals(1, result.getAuthorities().size());
        assertEquals("ROLE_USER", result.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsername_WhenUserDoesNotExist_ShouldThrowUsernameNotFoundException() {
        // Given
        String username = "nonexistent@example.com";
        when(fxUserRepository.findByEmail(username))
                .thenReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> fxUserDetailsService.loadUserByUsername(username));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void loadUserByUsername_WhenUserIsAdmin_ShouldReturnAdminRole() {
        // Given
        String username = "admin@example.com";
        testUser.setRole(UserRole.ADMIN);
        when(fxUserRepository.findByEmail(username))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails result = fxUserDetailsService.loadUserByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getAuthorities().size());
        assertEquals("ROLE_ADMIN", result.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsername_WhenUserIsDisabled_ShouldReturnDisabledUser() {
        // Given
        String username = "disabled@example.com";
        testUser.setEnabled(false);
        when(fxUserRepository.findByEmail(username))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails result = fxUserDetailsService.loadUserByUsername(username);

        // Then
        assertNotNull(result);
        assertFalse(result.isEnabled());
    }
} 