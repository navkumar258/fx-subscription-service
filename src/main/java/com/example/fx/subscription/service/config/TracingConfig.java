package com.example.fx.subscription.service.config;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration(proxyBeanMethods = false)
public class TracingConfig {

  @Bean
  ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
    return new ObservedAspect(observationRegistry);
  }

  @Bean
  public ObservationPredicate excludeActuatorFromTracing() {
    return (name, context) -> {
      if (context instanceof ServerRequestObservationContext srCtx) {
        return !srCtx.getCarrier().getRequestURI().startsWith("/actuator");
      }
      return true;
    };
  }

}
