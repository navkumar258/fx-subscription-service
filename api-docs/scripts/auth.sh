#!/usr/bin/env sh
set -e
. ./env.sh

signup() {
  echo "=== Signing up user ===" >&2

  HTTP_CODE=$(curl -sS -k -o /tmp/signup.json -w "%{http_code}" "$BASE_URL/api/v1/auth/signup" \
    -H "Content-Type: application/json" \
    -d '{
      "email": "user@example.com",
      "password": "Admin@321",
      "mobile": "+447700900123",
      "admin": false
    }')

  if [ "$HTTP_CODE" -eq 200 ]; then
    echo "Signup successful" >&2
    login
  elif [ "$HTTP_CODE" -eq 409 ]; then
    echo "User already exists, logging in instead..." >&2
    login
  else
    echo "Signup failed with HTTP $HTTP_CODE" >&2
    cat /tmp/signup.json >&2
    exit 1
  fi
}

login() {
  echo "=== Logging in user ===" >&2

  HTTP_CODE=$(curl -sS -k -o /tmp/login.json -w "%{http_code}" "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
      "username": "user@example.com",
      "password": "Admin@321"
    }')

  if [ "$HTTP_CODE" -ne 200 ]; then
    echo "Login failed with HTTP $HTTP_CODE" >&2
    cat /tmp/login.json >&2
    exit 1
  fi

  TOKEN=$(jq -r '.token' /tmp/login.json)
  if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "JWT token not found!" >&2
    exit 1
  fi

  # Output only JWT to stdout for capturing
  echo "$TOKEN"
}

case "$1" in
  signup) signup ;;
  login) login ;;
  *)
    echo "Usage: $0 {signup|login}" >&2
    exit 1
    ;;
esac
