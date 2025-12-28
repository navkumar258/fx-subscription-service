#!/usr/bin/env sh
set -e
. ./env.sh

TOKEN="$1"
if [ -z "$TOKEN" ]; then
  echo "Usage: $0 <TOKEN> {all|search|get|update|delete|subs}" >&2
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

get_all() {
  call_api GET "$BASE_URL/api/v1/users?page=$PAGE&size=$SIZE&sort=createdAt,desc"
}

search() {
  call_api GET "$BASE_URL/api/v1/users/search?email=user@example.com&enabled=true&page=$PAGE&size=$SIZE"
}

get_by_id() {
  call_api GET "$BASE_URL/api/v1/users/$USER_ID"
}

update() {
  DATA='{"email":"updated.user@example.com","mobile":"+447700900999","pushDeviceToken":"fcm-device-token"}'
  call_api PUT "$BASE_URL/api/v1/users/$USER_ID" "$DATA"
}

delete_user() {
  call_api DELETE "$BASE_URL/api/v1/users/$USER_ID"
}

subscriptions() {
  call_api GET "$BASE_URL/api/v1/users/$USER_ID/subscriptions"
}

case "$2" in
  all) get_all ;;
  search) search ;;
  get) get_by_id ;;
  update) update ;;
  delete) delete_user ;;
  subs) subscriptions ;;
  *)
    echo "Usage: $0 <TOKEN> {all|search|get|update|delete|subs}" >&2
    exit 1
    ;;
esac
