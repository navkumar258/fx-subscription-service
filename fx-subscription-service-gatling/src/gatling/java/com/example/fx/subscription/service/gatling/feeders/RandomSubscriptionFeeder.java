package com.example.fx.subscription.service.gatling.feeders;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("java:S2272")
public class RandomSubscriptionFeeder implements Iterator<Map<String, Object>> {

  private static final String[] CURRENCY_PAIRS = {"EUR/USD", "GBP/USD", "USD/JPY", "AUD/USD", "NZD/CAD"};
  private static final String[] DIRECTIONS = {"ABOVE", "BELOW"};
  private static final String[] NOTIFICATION_CHANNELS = {"email", "sms", "push"};
  private final Random random = new Random();

  @Override
  public boolean hasNext() {
    // This feeder never runs out of data
    return true;
  }

  @Override
  public Map<String, Object> next() {
    // Generate a random currency pair
    String currencyPair = CURRENCY_PAIRS[random.nextInt(CURRENCY_PAIRS.length)];

    // Generate a random threshold between 1.00 and 1.50
    double threshold = 1.00 + (1.50 - 1.00) * random.nextDouble();

    // Generate a random direction
    String direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];

    // Randomize notification channels
    int numberOfChannels = random.nextInt(NOTIFICATION_CHANNELS.length) + 1;
    List<String> shuffledChannels = Arrays.asList(NOTIFICATION_CHANNELS);
    Collections.shuffle(shuffledChannels, random);

    // Format the selected channels into a JSON array string
    String jsonChannels = shuffledChannels.stream()
            .limit(numberOfChannels) // Take a random subset of channels
            .map(channel -> "\"" + channel + "\"")
            .collect(Collectors.joining(", ", "[", "]"));

    // Return a map with the generated random data
    return Map.of(
            "currencyPair", currencyPair,
            "threshold", "%.2f".formatted(threshold),
            "direction", direction,
            "notificationChannels", jsonChannels
    );
  }
}
