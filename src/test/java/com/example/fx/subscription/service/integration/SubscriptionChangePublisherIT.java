package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import com.example.fx.subscription.service.service.SubscriptionChangePublisher;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.kafka.test.hamcrest.KafkaMatchers.hasKey;
import static org.springframework.kafka.test.hamcrest.KafkaMatchers.hasValue;

@SpringBootTest
@EmbeddedKafka(kraft = true, partitions = 1, topics = {"${spring.kafka.topic.subscription-changes}"})
@DirtiesContext
class SubscriptionChangePublisherIT {

  @Value(value = "${spring.kafka.topic.subscription-changes}")
  private String subscriptionChangesTopic;

  @Autowired
  EmbeddedKafkaBroker embeddedKafkaBroker;

  @Autowired
  SubscriptionChangePublisher subscriptionChangePublisher;

  private Consumer<String, SubscriptionChangeEvent> consumer;

  @BeforeEach
  void setup() {
    Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.example.fx.subscription.service.model.SubscriptionChangeEvent");

    ConsumerFactory<String, SubscriptionChangeEvent> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
    consumer = consumerFactory.createConsumer();
    consumer.subscribe(List.of(subscriptionChangesTopic));
  }

  @AfterEach
  void cleanup() {
    if (consumer != null) {
      consumer.close();
    }
  }

  @Test
  void sendMessage_WhenSuccessful_ShouldSendToKafka() throws InterruptedException {
    Subscription subscription = new Subscription(
            UUID.randomUUID(),
            "GBP/USD",
            BigDecimal.valueOf(1.20),
            ThresholdDirection.ABOVE,
            List.of("sms"),
            SubscriptionStatus.ACTIVE);

    SubscriptionChangeEvent testEvent = new SubscriptionChangeEvent(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            "SubscriptionCreated",
            SubscriptionResponse.fromSubscription(subscription)
    );

    subscriptionChangePublisher.sendMessage(testEvent);

    // Wait a bit for the async operation to complete
    TimeUnit.MILLISECONDS.sleep(100);

    ConsumerRecords<String, SubscriptionChangeEvent> received = consumer.poll(Duration.ofSeconds(2));
    
    // Verify we received at least one record
    Assertions.assertTrue(received.count() > 0, "No messages received from Kafka");
    
    // Get the first record and verify it
    var record = received.iterator().next();
    assertThat(record, hasKey(testEvent.payload().id().toString()));
    assertThat(record, hasValue(testEvent));
  }

  @Test
  void sendMessage_WhenSubscriptionUpdated_ShouldSendUpdateEvent() throws InterruptedException {
    Subscription subscription = new Subscription(
            UUID.randomUUID(),
            "EUR/USD",
            BigDecimal.valueOf(1.15),
            ThresholdDirection.BELOW,
            List.of("email"),
            SubscriptionStatus.ACTIVE);

    SubscriptionChangeEvent updateEvent = new SubscriptionChangeEvent(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            "SubscriptionUpdated",
            SubscriptionResponse.fromSubscription(subscription)
    );

    subscriptionChangePublisher.sendMessage(updateEvent);

    // Wait a bit for the async operation to complete
    TimeUnit.MILLISECONDS.sleep(100);

    ConsumerRecords<String, SubscriptionChangeEvent> received = consumer.poll(Duration.ofSeconds(2));
    
    // Verify we received at least one record
    Assertions.assertTrue(received.count() > 0, "No messages received from Kafka");
    
    // Get the first record and verify it
    var record = received.iterator().next();
    assertThat(record, hasKey(subscription.getId().toString()));
    assertThat(record, hasValue(updateEvent));

    SubscriptionChangeEvent receivedEvent = record.value();
    Assertions.assertEquals("SubscriptionUpdated", receivedEvent.eventType());
  }

  @Test
  void sendMessage_WhenSubscriptionDeleted_ShouldSendDeleteEvent() throws InterruptedException {
    Subscription subscription = new Subscription(
            UUID.randomUUID(),
            "USD/JPY",
            BigDecimal.valueOf(150.0),
            ThresholdDirection.ABOVE,
            List.of("sms"),
            SubscriptionStatus.INACTIVE);

    SubscriptionChangeEvent deleteEvent = new SubscriptionChangeEvent(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            "SubscriptionDeleted",
            SubscriptionResponse.fromSubscription(subscription)
    );

    subscriptionChangePublisher.sendMessage(deleteEvent);

    // Wait a bit for the async operation to complete
    TimeUnit.MILLISECONDS.sleep(100);

    ConsumerRecords<String, SubscriptionChangeEvent> received = consumer.poll(Duration.ofSeconds(2));
    
    // Verify we received at least one record
    Assertions.assertTrue(received.count() > 0, "No messages received from Kafka");
    
    // Get the first record and verify it
    var record = received.iterator().next();
    assertThat(record, hasKey(subscription.getId().toString()));
    assertThat(record, hasValue(deleteEvent));

    SubscriptionChangeEvent receivedEvent = record.value();
    Assertions.assertEquals("SubscriptionDeleted", receivedEvent.eventType());
  }

  @Test
  void sendMultipleMessages_ShouldSendAllToKafka() throws InterruptedException {
    List<SubscriptionChangeEvent> events = List.of(
            createTestEvent("SubscriptionCreated", "GBP/USD"),
            createTestEvent("SubscriptionUpdated", "EUR/USD"),
            createTestEvent("SubscriptionDeleted", "USD/JPY")
    );

    events.forEach(subscriptionChangePublisher::sendMessage);

    // Wait a bit for the async operations to complete
    TimeUnit.MILLISECONDS.sleep(200);

    ConsumerRecords<String, SubscriptionChangeEvent> received = consumer.poll(Duration.ofSeconds(2));
    Assertions.assertEquals(3, received.count());
  }

  private SubscriptionChangeEvent createTestEvent(String eventType, String currencyPair) {
    Subscription subscription = new Subscription(
            UUID.randomUUID(),
            currencyPair,
            BigDecimal.valueOf(1.20),
            ThresholdDirection.ABOVE,
            List.of("email"),
            SubscriptionStatus.ACTIVE);

    SubscriptionChangeEvent event = new SubscriptionChangeEvent(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            eventType,
            SubscriptionResponse.fromSubscription(subscription)
    );

    return event;
  }
}
