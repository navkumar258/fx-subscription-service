package com.example.fx.subscription.service.controller;

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
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@Observed(name = "auth.controller")
public class AuthController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final FxUserDetailsService fxUserDetailsService;
	private final FxUserRepository fxUserRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthController(AuthenticationManager authenticationManager,
	                      JwtTokenProvider jwtTokenProvider,
	                      FxUserDetailsService fxUserDetailsService,
	                      FxUserRepository fxUserRepository,
	                      PasswordEncoder passwordEncoder) {
		this.authenticationManager = authenticationManager;
		this.jwtTokenProvider = jwtTokenProvider;
		this.fxUserDetailsService = fxUserDetailsService;
		this.fxUserRepository = fxUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@PostMapping("/login")
	public AuthLoginResponse login(@Valid @RequestBody AuthRequest authRequest) {
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(
							authRequest.username(), authRequest.password()
					)
			);

			UserDetails userDetails = fxUserDetailsService.loadUserByUsername(authRequest.username());

			String token = jwtTokenProvider.createToken(
					userDetails.getUsername(),
					userDetails.getAuthorities().stream()
							.map(GrantedAuthority::getAuthority)
							.collect(Collectors.toSet())
			);

			LOGGER.info("User logged in successfully: username={}", authRequest.username());

			return new AuthLoginResponse(token, "Login successful");

		} catch (AuthenticationException ex) {
			LOGGER.warn("Authentication failed: username={}, message={}",
					authRequest.username(), ex.getMessage());

			throw new BadCredentialsException("Invalid username/password");
		}
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthSignupResponse signup(@Valid @RequestBody UserSignUpRequest userSignUpRequest) {
		FxUser user = createUserFromRequest(userSignUpRequest);
		try {
			FxUser savedUser = fxUserRepository.save(user);
			LOGGER.info("User registered successfully: email={}, userId={}", user.getEmail(), user.getId());

			return buildSignupResponse(savedUser, userSignUpRequest.admin());
		} catch (DataIntegrityViolationException _) {
			throw new UserAlreadyExistsException("Email is already registered", userSignUpRequest.email());
		}
	}

	private FxUser createUserFromRequest(UserSignUpRequest request) {
		FxUser user = new FxUser();
		user.setEmail(request.email());
		user.setPassword(passwordEncoder.encode(request.password()));
		user.setMobile(request.mobile());
		user.setEnabled(true);
		user.setRole(Boolean.TRUE.equals(request.admin()) ? UserRole.ADMIN : UserRole.USER);
		user.setCreatedAt(Instant.now());
		return user;
	}

	private AuthSignupResponse buildSignupResponse(FxUser user, boolean isAdmin) {
		String message = isAdmin ? "Admin registered successfully" : "User registered successfully";
		return new AuthSignupResponse(user.getId().toString(), message);
	}
}

