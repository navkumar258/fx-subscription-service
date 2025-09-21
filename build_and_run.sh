#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

echo "➡️ Running tests, coverage and checks on host machine..."
./gradlew check
./gradlew generateOpenApiDocs
echo "✅ All Gradle tasks passed successfully."

echo "➡️ Building and running Docker services in detached mode..."
docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build -d
echo "✅ Local setup is up and running!"