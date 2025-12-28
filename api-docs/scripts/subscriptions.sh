#!/usr/bin/env sh
set -e
. ./env.sh

TOKEN="$1"
if [ -z "$TOKEN" ]; then
  echo "Usage: $0 <TOKEN> {create|by-user|mine|all|get|update|delete}" >&2
  exit 1
fi
TOKEN=$(echo "$TOKEN" | tr -d '\r\n')
AUTH_HEADER="Authorization: Bearer $TOKEN"

call_api() {
  METHOD="$1"
  URL="$2"
  DATA="$3"

  echo "=== $METHOD $URL ===" >&2
  if [ "$METHOD" = "GET" ]; then
    HTTP_CODE=$(curl -sS -k -o /tmp/resp.json -w "%{http_code}" "$URL" -H "$AUTH_HEADER")
  else
    HTTP_CODE=$(curl -sS -k -o /tmp/resp.json -w "%{http_code}" -X "$METHOD" "$URL" -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$DATA")
  fi

  if [ "$HTTP_CODE" -ge 400 ]; then
    echo "API error $HTTP_CODE" >&2
    cat /tmp/resp.json >&2
    exit 1
  fi

  cat /tmp/resp.json
}

create() {
  DATA='{"currencyPair":"EUR/USD","threshold":1.105,"direction":"ABOVE","notificationChannels":["EMAIL","PUSH"]}'
  call_api POST "$BASE_URL/api/v1/subscriptions" "$DATA"
}

by_user() {
  call_api GET "$BASE_URL/api/v1/subscriptions?userId=$USER_ID"
}

mine() {
  call_api GET "$BASE_URL/api/v1/subscriptions/my"
}

all() {
  call_api GET "$BASE_URL/api/v1/subscriptions/all"
}

get() {
  call_api GET "$BASE_URL/api/v1/subscriptions/$SUBSCRIPTION_ID"
}

update() {
  DATA='{"currencyPair":"GBP/USD","threshold":1.275,"direction":"BELOW","status":"ACTIVE","notificationChannels":["EMAIL"]}'
  call_api PUT "$BASE_URL/api/v1/subscriptions/$SUBSCRIPTION_ID" "$DATA"
}

delete_sub() {
  call_api DELETE "$BASE_URL/api/v1/subscriptions/$SUBSCRIPTION_ID"
}

case "$2" in
  create) create ;;
  by-user) by_user ;;
  mine) mine ;;
  all) all ;;
  get) get ;;
  update) update ;;
  delete) delete_sub ;;
  *)
    echo "Usage: $0 <TOKEN> {create|by-user|mine|all|get|update|delete}" >&2
    exit 1
    ;;
esac
