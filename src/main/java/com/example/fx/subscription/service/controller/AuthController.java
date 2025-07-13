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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
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
  public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthRequest authRequest) {
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

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("User logged in successfully: username={}", authRequest.username());
      }

      return ResponseEntity.ok(new AuthLoginResponse(token, "Login successful"));

    } catch (org.springframework.security.core.AuthenticationException ex) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Authentication failed: username={}, message={}",
                authRequest.username(), ex.getMessage());
      }

      // Let the ControllerAdvice handle this exception
      throw new org.springframework.security.core.AuthenticationException("Invalid username/password") {
        @Override
        public String getMessage() {
          return "Invalid username/password";
        }
      };
    }
  }

  @PostMapping("/signup")
  public ResponseEntity<AuthSignupResponse> signup(@Valid @RequestBody UserSignUpRequest userSignUpRequest) {
    if (fxUserRepository.existsByEmail(userSignUpRequest.email())) {
      throw new UserAlreadyExistsException("Email is already registered", userSignUpRequest.email());
    }

    FxUser user = new FxUser();
    user.setEmail(userSignUpRequest.email());
    user.setPassword(passwordEncoder.encode(userSignUpRequest.password()));
    user.setMobile(userSignUpRequest.mobile());
    user.setEnabled(true);

    user.setRole(userSignUpRequest.admin() ? UserRole.ADMIN : UserRole.USER);

    FxUser savedUser = fxUserRepository.save(user);

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("User registered successfully: email={}, userId={}",
              userSignUpRequest.email(), savedUser.getId());
    }

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AuthSignupResponse(savedUser.getId(),
                    userSignUpRequest.admin() ?
                            "Admin registered successfully" : "User registered successfully"));
  }

}

