import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

export const options = {
  insecureSkipTLSVerify: true,
  scenarios: {
    ramping_api_load: {
      executor: 'ramping-arrival-rate',
      startRate: 1,          // initial requests per second
      timeUnit: '1s',         // time unit
      preAllocatedVUs: 100,   // k6 will pre-initialize these VUs
      maxVUs: 500,            // k6 can increase up to this limit as needed
      stages: [
        { target: 2, duration: '1m' }, // Ramp up to 100 RPS over 1 minute
        { target: 5, duration: '2m' }, // Ramp up to 300 RPS over 2 minutes
        { target: 5, duration: '2m' }, // Hold steady at 300 RPS for 2 minutes
        { target: 1, duration: '1m' },  // Ramp down to 50 RPS over 1 minute
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<250'],   // 95% of requests under 250ms
    checks: ['rate>0.95'],              // 95%+ of checks must pass
  },
};

// Trends and Counters for each API
const signupTrend = new Trend('signup_duration', true);
const signupStatus = new Counter('signup_status_code');

const loginTrend = new Trend('login_duration', true);
const loginStatus = new Counter('login_status_code');

const createSubTrend = new Trend('create_subscription_duration', true);
const createSubStatus = new Counter('create_subscription_status_code');

const getSubTrend = new Trend('get_my_subscriptions_duration', true);
const getSubStatus = new Counter('get_my_subscriptions_status_code');

export default function userScenario() {
  // Generate test email
  const email = `user_${__VU}_${__ITER}_${Math.floor(Math.random() * 10000)}@example.com`;

  // 1. SIGNUP
  const signupRes = http.post('https://localhost:8443/api/v1/auth/signup', JSON.stringify({
    email, password: 'password123', mobile: '+1234567890', admin: false
  }), { headers: { 'Content-Type': 'application/json' } });
  signupTrend.add(signupRes.timings.duration, { status: signupRes.status });
  signupStatus.add(signupRes.status);
  check(signupRes, { 'register status is 201 or 200': r => r.status === 201 || r.status === 200 });
  sleep(1);

  // 2. LOGIN
  const loginRes = http.post('https://localhost:8443/api/v1/auth/login', JSON.stringify({
    username: email, password: 'password123'
  }), { headers: { 'Content-Type': 'application/json' } });
  loginTrend.add(loginRes.timings.duration, { status: loginRes.status });
  loginStatus.add(loginRes.status);
  check(loginRes, { 'login status is 200': r => r.status === 200, 'login has token': r => r.json('token') !== undefined });
  const jwt = loginRes.json('token');
  sleep(1);

  // 3. CREATE SUBSCRIPTION
  const subRes = http.post('https://localhost:8443/api/v1/subscriptions', JSON.stringify({
    currencyPair: 'GBP/USD', threshold: 1.25, direction: 'ABOVE', notificationChannels: ['email', 'sms']
  }), { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${jwt}` } });
  createSubTrend.add(subRes.timings.duration, { status: subRes.status });
  createSubStatus.add(subRes.status);
  check(subRes, { 'create subscription status is 201 or 200': r => r.status === 201 || r.status === 200 });
  sleep(1);

  // 4. GET MY SUBSCRIPTIONS
  const getSubRes = http.get('https://localhost:8443/api/v1/subscriptions/my', { headers: { 'Authorization': `Bearer ${jwt}` } });
  getSubTrend.add(getSubRes.timings.duration, { status: getSubRes.status });
  getSubStatus.add(getSubRes.status);
  check(getSubRes, { 'get my subscriptions status is 200': r => r.status === 200, 'subscriptions is array': r => Array.isArray(r.json('subscriptions')) });
  sleep(1);
}
