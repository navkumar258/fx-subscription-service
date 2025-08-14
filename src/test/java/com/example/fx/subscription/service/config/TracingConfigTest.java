package com.example.fx.subscription.service.config;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.observation.ServerRequestObservationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TracingConfigTest {

  @Mock
  private ObservationRegistry observationRegistry;

  @Mock
  private HttpServletRequest httpServletRequest;

  private TracingConfig tracingConfig;

  @BeforeEach
  void setUp() {
    tracingConfig = new TracingConfig();
  }

  @Test
  void observedAspect_ShouldCreateObservedAspect() {
    // When
    ObservedAspect observedAspect = tracingConfig.observedAspect(observationRegistry);

    // Then
    assertNotNull(observedAspect);
  }

  @Test
  void excludeActuatorFromTracing_ShouldCreateObservationPredicate() {
    // When
    ObservationPredicate predicate = tracingConfig.excludeActuatorFromTracing();

    // Then
    assertNotNull(predicate);
  }

  @Test
  void excludeActuatorFromTracing_ShouldExcludeActuatorEndpoints() {
    // Given
    ObservationPredicate predicate = tracingConfig.excludeActuatorFromTracing();
    ServerRequestObservationContext context = new ServerRequestObservationContext(httpServletRequest, null);

    when(httpServletRequest.getRequestURI()).thenReturn("/actuator/health");

    // When
    boolean shouldExclude = predicate.test("test", context);

    // Then
    assertFalse(shouldExclude, "Actuator endpoints should be excluded from tracing");
  }

  @Test
  void excludeActuatorFromTracing_ShouldIncludeNonActuatorEndpoints() {
    // Given
    ObservationPredicate predicate = tracingConfig.excludeActuatorFromTracing();
    ServerRequestObservationContext context = new ServerRequestObservationContext(httpServletRequest, null);

    when(httpServletRequest.getRequestURI()).thenReturn("/api/v1/subscriptions");

    // When
    boolean shouldInclude = predicate.test("test", context);

    // Then
    assertTrue(shouldInclude, "Non-actuator endpoints should be included in tracing");
  }

  @Test
  void excludeActuatorFromTracing_ShouldHandleNullContext() {
    // Given
    ObservationPredicate predicate = tracingConfig.excludeActuatorFromTracing();

    // When
    boolean shouldInclude = predicate.test("test", null);

    // Then
    assertTrue(shouldInclude, "Null contexts should be included in tracing");
  }
}
