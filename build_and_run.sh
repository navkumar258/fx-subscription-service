#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

echo "➡️ Running tests and checks on host machine..."
./gradlew clean test
./gradlew generateOpenApiDocs
./gradlew jacocoTestCoverageVerification
./gradlew check
echo "✅ All Gradle tasks passed successfully."

echo "➡️ Building and running Docker services in detached mode..."
docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build -d
echo "✅ Local setup is up and running!"