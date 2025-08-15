package com.example.fx.subscription.service.config;

import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerConfigTest {

  private static final String CONFIGS = "configs";

  private KafkaProducerConfig kafkaProducerConfig;

  @BeforeEach
  void setUp() {
    kafkaProducerConfig = new KafkaProducerConfig();
    ReflectionTestUtils.setField(kafkaProducerConfig, "bootstrapAddress", "localhost:9092");
  }

  @Test
  void producerFactory_ShouldCreateProducerFactoryWithCorrectConfig() {
    // When
    ProducerFactory<String, SubscriptionChangeEvent> producerFactory = kafkaProducerConfig.producerFactory();

    // Then
    assertNotNull(producerFactory);
    assertInstanceOf(DefaultKafkaProducerFactory.class, producerFactory);

    // Verify the configuration properties
    Map<String, Object> configProps = (Map<String, Object>) ReflectionTestUtils.getField(producerFactory, CONFIGS);
    assertNotNull(configProps);
    assertEquals("localhost:9092", configProps.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
    assertEquals(StringSerializer.class, configProps.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
    assertEquals(JsonSerializer.class, configProps.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
    assertEquals(false, configProps.get(JsonSerializer.ADD_TYPE_INFO_HEADERS));
  }

  @Test
  void kafkaTemplate_ShouldCreateKafkaTemplateWithProducerFactory() {
    // When
    KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate = kafkaProducerConfig.kafkaTemplate();

    // Then
    assertNotNull(kafkaTemplate);
    assertInstanceOf(KafkaTemplate.class, kafkaTemplate);

    // Verify it uses the producer factory
    ProducerFactory<String, SubscriptionChangeEvent> producerFactory = kafkaProducerConfig.producerFactory();
    assertNotNull(producerFactory);
  }

  @Test
  void producerFactory_WithDifferentBootstrapAddress_ShouldUseCorrectAddress() {
    // Given
    String customBootstrapAddress = "kafka-server:9093";
    ReflectionTestUtils.setField(kafkaProducerConfig, "bootstrapAddress", customBootstrapAddress);

    // When
    ProducerFactory<String, SubscriptionChangeEvent> producerFactory = kafkaProducerConfig.producerFactory();

    // Then
    Map<String, Object> configProps = (Map<String, Object>) ReflectionTestUtils.getField(producerFactory, CONFIGS);
    assertNotNull(configProps);
    assertEquals(customBootstrapAddress, configProps.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
  }

  @Test
  void producerFactory_ShouldHaveCorrectSerializers() {
    // When
    ProducerFactory<String, SubscriptionChangeEvent> producerFactory = kafkaProducerConfig.producerFactory();

    // Then
    Map<String, Object> configProps = (Map<String, Object>) ReflectionTestUtils.getField(producerFactory, CONFIGS);
    assertNotNull(configProps);
    assertEquals(StringSerializer.class, configProps.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
    assertEquals(JsonSerializer.class, configProps.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
  }

  @Test
  void producerFactory_ShouldDisableTypeInfoHeaders() {
    // When
    ProducerFactory<String, SubscriptionChangeEvent> producerFactory = kafkaProducerConfig.producerFactory();

    // Then
    Map<String, Object> configProps = (Map<String, Object>) ReflectionTestUtils.getField(producerFactory, CONFIGS);
    assertNotNull(configProps);
    assertEquals(false, configProps.get(JsonSerializer.ADD_TYPE_INFO_HEADERS));
  }
} 