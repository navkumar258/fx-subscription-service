import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { randomString, randomItem, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export let options = {
  insecureSkipTLSVerify: true,
  scenarios: {
    signup_login_and_subscription: {
      executor: 'ramping-vus',
      exec: 'userScenario',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 50 },   // Ramp up to 50 VUs over 1 min
        { duration: '1m', target: 100 },  // Then ramp to 100 VUs in next 1 min
        { duration: '2m', target: 200 },  // Then ramp further to 200 VUs in 2 min
        { duration: '4m', target: 200 },  // Hold steady at 200 VUs for 4 min
        { duration: '2m', target: 0 },    // Then ramp-down to 0 VUs in 2 min
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<250'],
    custom_failure_rate: ['rate<0.02'],
  }
};

// Custom metrics
const signupDuration = new Trend('signup_duration');
const loginDuration = new Trend('login_duration');
const subscriptionCreateDuration = new Trend('subscription_create_duration');
const fetchSubsDuration = new Trend('fetch_subscriptions_duration');
const failureRate = new Rate('custom_failure_rate');
const requestCount = new Counter('custom_request_count');

const currencyPairs = ['GBP/USD', 'USD/EUR', 'EUR/JPY', 'USD/JPY', 'AUD/USD'];
const directions = ['ABOVE', 'BELOW'];
const channelsPool = [
  ['email'],
  ['sms'],
  ['email', 'sms'],
  ['push'],
];

function randomSubscriptionPayload() {
  return {
    currencyPair: randomItem(currencyPairs),
    threshold: parseFloat((Math.random() * 2 + 0.5).toFixed(4)), // threshold between 0.5 and 2.5
    direction: randomItem(directions),
    notificationChannels: randomItem(channelsPool),
  };
}

function randomEmail() {
  return `user${Math.floor(Math.random() * 1e7)}@example.com`;
}

export function userScenario() {
  let jwtToken;

  group('Authentication', function () {
    // Signup
    const signupBody = JSON.stringify({
      email: randomEmail(),
      password: 'password123',
      mobile: '+1234567890',
      admin: false
    });
    let signupRes = http.post(
      'https://localhost:8443/api/v1/auth/signup',
      signupBody,
      { headers: { 'Content-Type': 'application/json' } }
    );
    signupDuration.add(signupRes.timings.duration);
    requestCount.add(1);
    failureRate.add(signupRes.status !== 201);
    check(signupRes, {
      'signup status is 201': (r) => r.status === 201,
    });
    sleep(1);

    // Login
    const loginBody = JSON.stringify({
      username: JSON.parse(signupBody).email,
      password: 'password123'
    });
    let loginRes = http.post(
      'https://localhost:8443/api/v1/auth/login',
      loginBody,
      { headers: { 'Content-Type': 'application/json' } }
    );
    loginDuration.add(loginRes.timings.duration);
    requestCount.add(1);
    failureRate.add(loginRes.status !== 200);
    check(loginRes, {
      'login status is 200': (r) => r.status === 200,
      'token present': (r) => r.json('token') !== undefined,
    });
    jwtToken = loginRes.json('token');
    sleep(1);
  });

  group('Subscription Management', function () {
    // Create Subscription
    const subBody = JSON.stringify(randomSubscriptionPayload());
    let subRes = http.post(
      'https://localhost:8443/api/v1/subscriptions',
      subBody,
      {
        headers: {
          'Authorization': `Bearer ${jwtToken}`,
          'Content-Type': 'application/json',
        }
      }
    );
    subscriptionCreateDuration.add(subRes.timings.duration);
    requestCount.add(1);
    failureRate.add(subRes.status !== 201);
    check(subRes, {
      'subscription status is 201': (r) => r.status === 201,
    });
    sleep(1);

    // Get Subscriptions
    let fetchRes = http.get(
      'https://localhost:8443/api/v1/subscriptions/my',
      { headers: { 'Authorization': `Bearer ${jwtToken}` } }
    );
    fetchSubsDuration.add(fetchRes.timings.duration);
    requestCount.add(1);
    failureRate.add(fetchRes.status !== 200);
    check(fetchRes, {
      'fetch subscriptions status is 200': (r) => r.status === 200,
    });
    sleep(1);
  });
}
