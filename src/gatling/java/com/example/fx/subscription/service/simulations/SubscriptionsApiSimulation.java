package com.example.fx.subscription.service.simulations;

import com.example.fx.subscription.service.feeders.RandomSubscriptionFeeder;
import com.example.fx.subscription.service.feeders.RandomUserFeeder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class SubscriptionsApiSimulation extends Simulation {
  private static final String EMAIL = "email";
  private static final String PASSWORD = "password";

  // Config Loader
  String env = System.getProperty("env", "local");
  Config config = ConfigFactory.load("gatling.conf").getConfig("api").getConfig(env);

  String baseUrl = config.getString("baseUrl");
  String signupEndpoint = config.getString("signupEndpoint");
  String loginEndpoint = config.getString("loginEndpoint");
  String subscriptionsEndpoint = config.getString("subscriptionsEndpoint");
  String mySubscriptionsEndpoint = config.getString("mySubscriptionsEndpoint");
  String contentType = config.getString("contentType");
  String acceptType = config.getString("acceptType");

  // Base API protocol
  private final HttpProtocolBuilder httpProtocol = http
          .baseUrl(baseUrl)
          .acceptHeader(acceptType)
          .contentTypeHeader(contentType);

  RandomUserFeeder userFeeder = new RandomUserFeeder();
  RandomSubscriptionFeeder randomSubscriptionFeeder = new RandomSubscriptionFeeder();

  // Chain Signup, Login, then Auth CRUD
  private final ChainBuilder userFlow = exec(
          // 1. Signup
          http("Signup")
                  .post(signupEndpoint)
                  .body(StringBody(session -> String.format(
                          "{\"email\":\"%s\", \"password\":\"%s\", \"mobile\":\"%s\"}",
                          session.getString(EMAIL), session.getString(PASSWORD), session.getString("mobile")
                  )))
                  .check(status().is(201))
  ).pause(1)
          // 2. Login
          .exec(
                  http("Login")
                          .post(loginEndpoint)
                          .body(StringBody(session -> String.format(
                                  "{\"username\":\"%s\", \"password\":\"%s\"}",
                                  session.getString(EMAIL), session.getString(PASSWORD)
                          )))
                          .check(status().is(200))
                          .check(jsonPath("$.token").saveAs("jwtToken"))
          ).pause(1)
          // 3. Create Subscription
          .exec(
                  feed(randomSubscriptionFeeder)
                          .exec(
                                  http("Create Subscription")
                                          .post(subscriptionsEndpoint)
                                          .header("Authorization", "Bearer #{jwtToken}")
                                          .body(StringBody(
                                                  "{\"currencyPair\": \"#{currencyPair}\", \"threshold\": #{threshold}, \"direction\": \"#{direction}\", \"notificationChannels\": #{notificationChannels}}"
                                          ))
                                          .check(status().is(201))
                          ).pause(1)
          )
          // 4. Get My Subscriptions
          .exec(
                  http("Get My Subscriptions")
                          .get(mySubscriptionsEndpoint)
                          .header("Authorization", "Bearer #{jwtToken}")
                          .check(status().is(200))
          );

  public SubscriptionsApiSimulation() {
    setUp(
            scenario("Signup, Login, Create & Get Subscriptions Flow")
                    .feed(userFeeder)
                    .exec(userFlow)
                    .injectOpen(
                            rampUsers(300).during(60),        // Ramp up to 300 users over 1 minute
                            nothingFor(20),                    // Hold for 20 seconds
                            constantUsersPerSec(20).during(300), // Maintain 20 new users per second for 5 minutes
                            rampUsersPerSec(20).to(50).during(120) // Ramp up from 20 to 50 users per second over 2 minutes
                    )
    ).protocols(httpProtocol);
  }
}
