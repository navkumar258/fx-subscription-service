package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.kafka.test.hamcrest.KafkaMatchers.*;

@SpringBootTest
@EmbeddedKafka(kraft = true, partitions = 1, topics = {"${spring.kafka.topic.subscription-changes}"})
class SubscriptionChangePublisherTest {

  @Value(value = "${spring.kafka.topic.subscription-changes}")
  private String subscriptionChangesTopic;

  @Autowired
  EmbeddedKafkaBroker embeddedKafkaBroker;

  @Autowired
  KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

  private Consumer<String, SubscriptionChangeEvent> consumer;

  @BeforeEach
  void createConsumer() {
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
  void closeConsumer() {
    consumer.close();
  }

  @Test
  @DirtiesContext
  void test_sendMessage() {
    UUID uuid  = UUID.randomUUID();
    Subscription subscription = new Subscription(
            uuid,
            "GBP/USD",
            BigDecimal.valueOf(1.20),
            ThresholdDirection.ABOVE,
            List.of("sms"),
            SubscriptionStatus.ACTIVE);

    SubscriptionChangeEvent subscriptionChangeEvent = new SubscriptionChangeEvent();
    subscriptionChangeEvent.setEventId(UUID.randomUUID().toString());
    subscriptionChangeEvent.setEventType("SubscriptionCreated");
    subscriptionChangeEvent.setPayload(subscription);
    subscriptionChangeEvent.setTimestamp(System.currentTimeMillis());

    ProducerRecord<String, SubscriptionChangeEvent> producerRecord = new ProducerRecord<>(
            subscriptionChangesTopic,
            uuid.toString(),
            subscriptionChangeEvent
    );

    kafkaTemplate.send(producerRecord);

    ConsumerRecords<String, SubscriptionChangeEvent> received = consumer.poll(Duration.ofSeconds(1));
    assertThat(received.iterator().next(), hasKey(uuid.toString()));
    assertThat(received.iterator().next(), hasValue(subscriptionChangeEvent));
  }
}
