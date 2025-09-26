#!/bin/bash

USER="navkumar258"
PACKAGE="fx-subscription-service"
TOKEN="${FX_GITHUB_TOKEN}"

# API endpoint: use /orgs or /users as needed
API="https://api.github.com/users/$USER/packages/container/$PACKAGE/versions?per_page=100"

# Get all but the last 4 version IDs
version_ids=$(curl -s -H "Authorization: Bearer $TOKEN" "$API" | jq '.[10:] | .[].id')

for id in $version_ids; do
  echo "Deleting version id: $id"
  curl -X DELETE -H "Authorization: Bearer $TOKEN" \
    "https://api.github.com/users/$USER/packages/container/$PACKAGE/versions/$id"
done
