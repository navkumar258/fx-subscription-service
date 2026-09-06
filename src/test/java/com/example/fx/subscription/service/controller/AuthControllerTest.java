package com.example.fx.subscription.service.controller;

import com.example.fx.subscription.service.ai.tool.FxSubscriptionTool;
import com.example.fx.subscription.service.config.JwtTokenProvider;
import com.example.fx.subscription.service.dto.auth.AuthLoginResponse;
import com.example.fx.subscription.service.dto.auth.AuthRequest;
import com.example.fx.subscription.service.dto.auth.AuthSignupResponse;
import com.example.fx.subscription.service.dto.user.UserSignUpRequest;
import com.example.fx.subscription.service.exception.UserAlreadyExistsException;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.UserRole;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.service.FxUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

	@MockitoBean
	private FxSubscriptionTool fxSubscriptionTool;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private FxUserDetailsService fxUserDetailsService;

	@MockitoBean
	private FxUserRepository fxUserRepository;

	@MockitoBean
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private CacheManager cacheManager;

	private AuthController authController;

	@BeforeEach
	void setUp() {
		authController = new AuthController(
				authenticationManager,
				jwtTokenProvider,
				fxUserDetailsService,
				fxUserRepository,
				passwordEncoder
		);
	}

	@Test
	void login_WithValidCredentials_ShouldReturnToken() {
		// Given
		AuthRequest authRequest = new AuthRequest("testuser", "password123");
		String expectedToken = "jwt.token.here";

		UserDetails userDetails = User.builder()
				.username("testuser")
				.password("encodedPassword")
				.authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
				.build();

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(null);
		when(fxUserDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
		when(jwtTokenProvider.createToken(anyString(), any())).thenReturn(expectedToken);

		// When
		AuthLoginResponse response = authController.login(authRequest);

		// Then
		assertNotNull(response);
		assertEquals(expectedToken, response.token());
		assertEquals("Login successful", response.message());

		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(fxUserDetailsService).loadUserByUsername("testuser");
		verify(jwtTokenProvider).createToken("testuser", Set.of("ROLE_USER"));
	}

	@Test
	void login_WithInvalidCredentials_ShouldThrowBadCredentialsException() {
		// Given
		AuthRequest authRequest = new AuthRequest("testuser", "wrongpassword");

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("Invalid credentials"));

		// When & Then
		BadCredentialsException exception = assertThrows(BadCredentialsException.class, () ->
				authController.login(authRequest));

		assertEquals("Invalid username/password", exception.getMessage());

		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(fxUserDetailsService, never()).loadUserByUsername(anyString());
		verify(jwtTokenProvider, never()).createToken(anyString(), any());
	}

	@Test
	void login_WithAdminUser_ShouldCreateTokenWithAdminRole() {
		// Given
		AuthRequest authRequest = new AuthRequest("adminuser", "password123");
		String expectedToken = "admin.jwt.token";

		UserDetails userDetails = User.builder()
				.username("adminuser")
				.password("encodedPassword")
				.authorities(List.of(
						new SimpleGrantedAuthority("ROLE_ADMIN"),
						new SimpleGrantedAuthority("ROLE_USER")
				))
				.build();

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(null);
		when(fxUserDetailsService.loadUserByUsername("adminuser")).thenReturn(userDetails);
		when(jwtTokenProvider.createToken(anyString(), any())).thenReturn(expectedToken);

		// When
		AuthLoginResponse response = authController.login(authRequest);

		// Then
		assertNotNull(response);
		assertEquals(expectedToken, response.token());

		verify(jwtTokenProvider).createToken("adminuser", Set.of("ROLE_ADMIN", "ROLE_USER"));
	}

	@Test
	void signup_WithValidUserData_ShouldCreateUser() {
		// Given
		UserSignUpRequest signUpRequest = new UserSignUpRequest(
				"test@example.com", "password123", "+1234567890", false);

		FxUser savedUser = new FxUser();
		savedUser.setId(UUID.randomUUID());
		savedUser.setEmail("test@example.com");
		savedUser.setRole(UserRole.USER);

		when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
		when(fxUserRepository.save(any(FxUser.class))).thenReturn(savedUser);

		// When
		AuthSignupResponse response = authController.signup(signUpRequest);

		// Then
		assertNotNull(response);
		assertEquals(savedUser.getId().toString(), response.userId());
		assertEquals("User registered successfully", response.message());

		verify(passwordEncoder).encode("password123");
		verify(fxUserRepository).save(any(FxUser.class));
	}

	@Test
	void signup_WithAdminUser_ShouldCreateAdminUser() {
		// Given
		UserSignUpRequest signUpRequest = new UserSignUpRequest(
				"admin@example.com", "password123", "+1234567890", true);

		FxUser savedUser = new FxUser();
		savedUser.setId(UUID.randomUUID());
		savedUser.setEmail("admin@example.com");
		savedUser.setRole(UserRole.ADMIN);

		when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
		when(fxUserRepository.save(any(FxUser.class))).thenReturn(savedUser);

		// When
		AuthSignupResponse response = authController.signup(signUpRequest);

		// Then
		assertNotNull(response);
		assertEquals("Admin registered successfully", response.message());

		verify(fxUserRepository).save(argThat(user ->
				user.getRole() == UserRole.ADMIN &&
						user.isEnabled() &&
						user.getEmail().equals("admin@example.com")
		));
	}

	@Test
	void signup_WithExistingEmail_ShouldThrowUserAlreadyExistsException() {
		// Given
		UserSignUpRequest signUpRequest = new UserSignUpRequest(
				"existing@example.com", "password123", "+1234567890", false);

		when(fxUserRepository.save(any(FxUser.class)))
				.thenThrow(new DataIntegrityViolationException("Unique constraint failed"));

		// When & Then
		UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class,
				() -> authController.signup(signUpRequest));

		assertEquals("Email is already registered", exception.getMessage());
		assertEquals("existing@example.com", exception.getEmail());
	}

	@Test
	void signup_ShouldSetAllUserFieldsCorrectly() {
		// Given
		UserSignUpRequest signUpRequest = new UserSignUpRequest(
				"test@example.com", "password123", "+1234567890", false);

		FxUser savedUser = new FxUser();
		savedUser.setId(UUID.randomUUID());

		when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
		when(fxUserRepository.save(any(FxUser.class))).thenReturn(savedUser);

		// When
		authController.signup(signUpRequest);

		// Then
		verify(fxUserRepository).save(argThat(user ->
				user.getEmail().equals("test@example.com") &&
						Objects.equals(user.getPassword(), "encodedPassword") &&
						user.getMobile().equals("+1234567890") &&
						user.isEnabled() &&
						user.getRole() == UserRole.USER
		));
	}
}