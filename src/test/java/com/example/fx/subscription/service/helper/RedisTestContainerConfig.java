package com.example.fx.subscription.service.helper;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestConfiguration
public class RedisTestContainerConfig {

  @Bean
  @ServiceConnection
  RedisContainer redisContainer() {
    return new RedisContainer(DockerImageName.parse("redis:8.4.0"));
  }
}
