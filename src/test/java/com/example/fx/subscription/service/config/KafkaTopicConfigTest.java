package com.example.fx.subscription.service.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KafkaTopicConfigTest {

    private KafkaTopicConfig kafkaTopicConfig;

    @BeforeEach
    void setUp() {
        kafkaTopicConfig = new KafkaTopicConfig();
        ReflectionTestUtils.setField(kafkaTopicConfig, "bootstrapAddress", "localhost:9092");
        ReflectionTestUtils.setField(kafkaTopicConfig, "subscriptionChangesTopic", "subscription-changes");
    }

    @Test
    void kafkaAdmin_ShouldCreateKafkaAdminWithCorrectConfig() {
        // When
        KafkaAdmin kafkaAdmin = kafkaTopicConfig.kafkaAdmin();

        // Then
        assertNotNull(kafkaAdmin);
        assertInstanceOf(KafkaAdmin.class, kafkaAdmin);
        
        // Verify the configuration properties
        Map<String, Object> configs = (Map<String, Object>) ReflectionTestUtils.getField(kafkaAdmin, "configs");
        assertNotNull(configs);
        assertEquals("localhost:9092", configs.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));
    }

    @Test
    void topic1_ShouldCreateNewTopicWithCorrectConfig() {
        // When
        NewTopic newTopic = kafkaTopicConfig.topic1();

        // Then
        assertNotNull(newTopic);
        assertInstanceOf(NewTopic.class, newTopic);
        assertEquals("subscription-changes", newTopic.name());
        assertEquals(3, newTopic.numPartitions());
        assertEquals((short) 1, newTopic.replicationFactor());
    }

    @Test
    void kafkaAdmin_WithDifferentBootstrapAddress_ShouldUseCorrectAddress() {
        // Given
        String customBootstrapAddress = "kafka-server:9093";
        ReflectionTestUtils.setField(kafkaTopicConfig, "bootstrapAddress", customBootstrapAddress);

        // When
        KafkaAdmin kafkaAdmin = kafkaTopicConfig.kafkaAdmin();

        // Then
        Map<String, Object> configs = (Map<String, Object>) ReflectionTestUtils.getField(kafkaAdmin, "configs");
        assertEquals(customBootstrapAddress, configs.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));
    }

    @Test
    void topic1_WithDifferentTopicName_ShouldUseCorrectTopicName() {
        // Given
        String customTopicName = "custom-subscription-topic";
        ReflectionTestUtils.setField(kafkaTopicConfig, "subscriptionChangesTopic", customTopicName);

        // When
        NewTopic newTopic = kafkaTopicConfig.topic1();

        // Then
        assertEquals(customTopicName, newTopic.name());
        assertEquals(3, newTopic.numPartitions());
        assertEquals((short) 1, newTopic.replicationFactor());
    }

    @Test
    void topic1_ShouldHaveCorrectPartitionCount() {
        // When
        NewTopic newTopic = kafkaTopicConfig.topic1();

        // Then
        assertEquals(3, newTopic.numPartitions());
    }

    @Test
    void topic1_ShouldHaveCorrectReplicationFactor() {
        // When
        NewTopic newTopic = kafkaTopicConfig.topic1();

        // Then
        assertEquals((short) 1, newTopic.replicationFactor());
    }
} 