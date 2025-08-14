# FX Subscription Service

A comprehensive Spring Boot microservice for managing foreign exchange (FX) rate subscriptions with real-time
notifications, MCP (Model Context Protocol) server capabilities, and event-driven architecture.

## 🚀 Features

### Core Functionality

- **User Management**: Complete user registration, authentication, and profile management
- **FX Subscription Management**: Create, update, delete, and monitor currency pair subscriptions
- **Real-time Notifications**: Multi-channel notification support (email, SMS, push)
- **MCP Server Integration**: Model Context Protocol server for AI tool interactions
- **Event-Driven Architecture**: Kafka-based event publishing for subscription changes
- **Security**: JWT-based authentication with role-based access control

### Technical Features

- **Database**: PostgreSQL database with JPA/Hibernate
- **Caching**: Redis cache with configurable TTL and Jackson serialization
- **API Documentation**: OpenAPI 3.1.0 specification with Swagger UI and automated documentation generation
- **Monitoring**: Prometheus metrics and health endpoints
- **Observability**: Distributed tracing with Zipkin, Logging with Loki - integrated with Grafana OSS
- **Scheduling**: Automated subscription processing and event publishing
- **Testing**: Comprehensive unit and integration tests with TestContainers for PostgreSQL, Kafka, and Redis

## 📚 Architecture

```
 ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
 │   Web Client    │    │   Mobile App    │    │   AI Chat Bot   │
 └─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
           │                      │                      │
           └──────────────────────┼──────────────────────┘
                                  │
                     ┌────────────▼─────────────-┐
                     │   FX Subscription Service │
                     │       (MCP Server)        │
                     │                           │
                     │  ┌─────────────────────┐  │
                     │  │   REST Controllers  │  │
                     │  └─────────┬───────────┘  │
                     │            │              │
                     │  ┌─────────▼───────────┐  │
                     │  │   Business Logic    │  │
                     │  │   (with Caching)    │  │
                     │  └─────────┬───────────┘  │
                     │            │              │
                     │  ┌─────────▼───────────┐  │
                     │  │   Data Access Layer │  │
                     │  └─────────┬───────────┘  │
                     └────────────┼──────────────┘
                                  │
                     ┌────────────▼─────────────-┐
                     │        Redis Cache        │
                     │    - subscription         │
                     │    - subscriptionsByUser  │
                     └───────────────────────────┘
                                  │
                     ┌────────────▼─────────────-┐
                     │       PostgreSQL DB       │
                     └──────────────────────────-┘
                                  │
                     ┌────────────▼─────────────-┐
                     │        Kafka Topics       │
                     │  - subscription-changes   │
                     └───────────────────────────┘
```

## 📝 Prerequisites

### For Local Development (Docker-based - Recommended)
- Docker and Docker Compose
- A valid PKCS12 keystore file (for SSL/TLS)
- Environment variables configured (see Configuration section)

### For Host-based Development
- Java 21+
- Gradle 8.0+
- PostgreSQL 17.5+
- Redis 8.2.0+
- Apache Kafka 3.9.1+
- Docker Compose (for observability stack)
- FX MCP Client (for AI interactions)

## 🛠️ Installation & Setup

### Option 1: Quick Start with Docker (Recommended)

This is the fastest way to get the entire application stack running locally with minimal setup.

#### 1. Clone the Repository

```bash
git clone <repository-url>
cd fx-subscription-service
```

#### 2. Configure Environment Variables

Create a `.env` file in the project root or set the following environment variables:

```bash
# Required: SSL Keystore Configuration
export FX_KEYSTORE_LOCATION=path/to/your/keystore.p12
export FX_KEYSTORE_PASSWORD=your_keystore_password

# Required: JWT Secret Key
export FX_JWT_SECRET_KEY=your_jwt_secret_key_at_least_256_bits

# Optional: Database Configuration (defaults provided)
# export FX_POSTGRES_DB=fx_subscription_db
# export FX_POSTGRES_USER=postgres
# export FX_POSTGRES_PASSWORD=password

# Optional: Redis Configuration (defaults provided)
# export FX_REDIS_HOST=redis
# export FX_REDIS_PORT=6379
```

#### 3. Run the Complete Setup Script

```bash
# This script runs tests, builds, and starts all services
chmod +x build_and_run.sh
./build_and_run.sh
```

**What this script does:**
- ✅ Runs all tests on the host machine
- ✅ Generates OpenAPI documentation
- ✅ Runs JaCoCo coverage verification
- ✅ Performs code quality checks
- ✅ Builds and starts all Docker services including:
  - FX Subscription Service (main application)
  - PostgreSQL database
  - Apache Kafka (message broker)
  - Observability stack (Grafana, Prometheus, Loki, Zipkin)

#### 4. Verify Services Are Running

```bash
# Check all services status
docker compose ps

# View logs
docker compose logs web-service
docker compose logs postgres
docker compose logs kafka
```

### Option 2: Step-by-Step Docker Setup

If you prefer more control over the setup process:

#### 1. Clone and Configure (same as above)

#### 2. Build and Run Core Services

```bash
# Build and start main application with database and Kafka
docker compose up --build -d

# Check service status
docker compose ps
```

#### 3. Start Observability Stack (Optional)

```bash
# Start monitoring and observability tools
docker compose -f docker-compose.observability.yml up -d

# Verify observability services
docker compose -f docker-compose.observability.yml ps
```

### Option 3: Host-based Development

For developers who prefer running the application directly on their host machine:

#### 1. Setup Dependencies

```bash
# Start only PostgreSQL and Kafka via Docker
docker compose up postgres kafka -d

# Or install PostgreSQL and Kafka natively on your system
```

#### 2. Configure Application Properties

Update `src/main/resources/application.properties` with your local database and Kafka settings.

#### 3. Build and Run

```bash
# Build the application
./gradlew build

# Run the application
./gradlew bootRun
```

## 🌐 Service Access

After successful setup, the following services will be available:

### Main Application
- **FX Subscription Service**: https://localhost:8443
- **OpenAPI Docs**: https://localhost:8443/v3/api-docs
- **Health Check**: https://localhost:8443/actuator/health
- **Metrics**: https://localhost:8443/actuator/prometheus

### Database, Cache & Messaging
- **PostgreSQL**: localhost:5432
  - Database: `fx_subscription_db`
  - Username: `postgres`
  - Password: `password`
- **Redis**: localhost:6379
  - Cache: `subscription`, `subscriptionsByUser`
- **Kafka**: localhost:9092
  - Topic: `subscription-change-events`

### Observability Stack
- **Grafana Dashboard**: http://localhost:3000 (admin/admin)
- **Prometheus Metrics**: http://localhost:9090
- **Zipkin Traces**: http://localhost:9411
- **Loki Logs**: http://localhost:3100

### MCP Integration
- **SSE Endpoint**: https://localhost:8443/sse (for FX MCP Client)

## 🚀 Development Workflow

### Making Changes

1. **Code Changes**: Make your changes to the source code
2. **Test Locally**: Run tests with `./gradlew test`
3. **Rebuild Service**: 
   ```bash
   # Rebuild just the web service
   docker compose up web-service --build
   
   # Or use the full build script
   ./build_and_run.sh
   ```

### Debugging

```bash
# View application logs
docker compose logs -f web-service

# Access database directly
docker exec -it postgres psql -U postgres -d fx_subscription_db

# Access Redis cache
docker exec -it redis redis-cli

# Check Kafka topics
docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### Stopping Services

```bash
# Stop all services
docker compose down

# Stop with observability stack
docker compose -f docker-compose.yml -f docker-compose.observability.yml down

# Stop and remove volumes (⚠️ This will delete your data)
docker compose down -v
```

## 📁 Docker Configuration Files

The project includes several Docker configuration files for different purposes:

- **`Dockerfile`**: Production-ready container image
- **`Dockerfile.local`**: Multi-stage build for local development
- **`docker-compose.yml`**: Core services (app, database, Kafka)
- **`docker-compose.observability.yml`**: Monitoring and observability stack
- **`build_and_run.sh`**: Automated setup script for complete local development environment
```

## ⚙️ Configuration

### Environment Variables for Docker Setup

The Docker-based setup uses the following environment variables:

```bash
# Required: SSL Configuration
FX_KEYSTORE_LOCATION=path/to/your/keystore.p12
FX_KEYSTORE_PASSWORD=your_keystore_password

# Required: JWT Configuration  
FX_JWT_SECRET_KEY=your_jwt_secret_key_at_least_256_bits

# Optional: Database Configuration (defaults shown)
FX_POSTGRES_HOST=postgres:5432
FX_POSTGRES_DB=fx_subscription_db
FX_POSTGRES_USER=postgres
FX_POSTGRES_PASSWORD=password

# Optional: Redis Configuration (defaults shown)
FX_REDIS_HOST=redis:locallhost
FX_REDIS_PORT=6379

# Optional: Kafka Configuration (defaults shown)
FX_KAFKA_HOST=kafka:29092

# Optional: Tracing Configuration (defaults shown)
FX_ZIPKIN_HOST=http://zipkin:9411
```

### Application Properties (for host-based development)

```bash
# SSL Configuration
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${FX_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12

# JWT Configuration
security.jwt.token.secret-key=${FX_JWT_SECRET_KEY}
security.jwt.token.expire-length=3600000

# Database (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/fx_subscription_db
spring.datasource.username=postgres
spring.datasource.password=password

# Redis Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live-seconds=300
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.topic.subscription-changes=subscription-change-events

# MCP Server Configuration
#spring.ai.mcp.server.name=fx-mcp-server
```

## 📄 API Documentation

### OpenAPI Documentation

The service automatically generates OpenAPI 3.1.0 documentation using SpringDoc. The documentation is available at:

- **Swagger UI**: `https://localhost:8443/swagger-ui.html`
- **OpenAPI JSON**: `https://localhost:8443/v3/api-docs`
- **Generated Documentation**: `api-docs/fx-subscription-service.json`

### Authentication Endpoints

#### Register User

```http
POST /api/v1/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "mobile": "+1234567890",
  "admin": false
}
```

#### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password123"
}
```

### Subscription Endpoints

#### Create Subscription

```http
POST /api/v1/subscriptions
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "currencyPair": "GBP/USD",
  "threshold": 1.25,
  "direction": "ABOVE",
  "notificationChannels": ["email", "sms"]
}
```

#### Get All Subscriptions (Admin only)

```http
GET /api/v1/subscriptions/all
Authorization: Bearer <jwt_token>
```

#### Get Subscriptions by User ID

```http
GET /api/v1/subscriptions?userId={userId}
Authorization: Bearer <jwt_token>
```

#### Update Subscription

```http
PUT /api/v1/subscriptions/{id}
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "currencyPair": "EUR/USD",
  "threshold": 1.15,
  "direction": "BELOW",
  "notificationChannels": ["email"]
}
```

#### Delete Subscription

```http
DELETE /api/v1/subscriptions/{id}
Authorization: Bearer <jwt_token>
```

### User Management Endpoints

#### Get All Users (Admin only)

```http
GET /api/v1/users?page=0&size=20
Authorization: Bearer <jwt_token>
```

#### Get User by ID

```http
GET /api/v1/users/{id}
Authorization: Bearer <jwt_token>
```

#### Search users with filters

```http
GET /api/v1/users/search
Authorization: Bearer <jwt_token>
```

#### Get User Subscriptions

```http
GET /api/v1/users/{id}/subscriptions
Authorization: Bearer <jwt_token>
```

#### Update User

```http
PUT /api/v1/users/{id}
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "email": "newemail@example.com",
  "mobile": "+9876543210"
}
```

### MCP Server Endpoints

#### SSE Endpoint (for MCP Client)

```http
GET /sse
```

**Note**: These endpoints are used by the FX MCP Client for AI tool interactions and are not meant for direct human
consumption.

## 📊 Database Schema

### Tables

#### fx_users

- `id` (UUID, Primary Key)
- `email` (String, Unique)
- `mobile` (String)
- `password` (String, Encrypted)
- `enabled` (Boolean)
- `push_device_token` (String)
- `role` (Enum: USER, ADMIN)
- `created_at` (Timestamp)
- `updated_at` (Timestamp)

#### subscriptions

- `id` (UUID, Primary Key)
- `user_id` (UUID, Foreign Key)
- `currency_pair` (String)
- `threshold` (Decimal)
- `direction` (Enum: ABOVE, BELOW)
- `notifications_channels` (JSON Array)
- `status` (Enum: ACTIVE, INACTIVE, EXPIRED)
- `created_at` (Timestamp)
- `updated_at` (Timestamp)

#### events_outbox

- `id` (UUID, Primary Key)
- `aggregate_type` (String)
- `aggregate_id` (UUID)
- `event_type` (String)
- `payload` (JSON)
- `status` (String)
- `timestamp` (Long)

## 🔐 Security

### Authentication

- JWT-based token authentication
- BCrypt password hashing
- Role-based access control (USER, ADMIN)

### Authorization

- Method-level security with `@PreAuthorize` and jwt claims
- Users can only access their own resources
- Admin access for system-wide operations

### SSL/TLS

- HTTPS enabled by default
- Custom keystore(PKCS12) configuration

### MCP Security

- MCP endpoints (`/sse`, `/mcp/**`) are publicly accessible
- No authentication required for MCP client connections
- Tool execution is handled securely within the service

## 📊 Monitoring & Observability

### Health Endpoints

- `GET /actuator/health` - Application health status
- `GET /actuator/prometheus` - Prometheus metrics

### Metrics (Prometheus)

- HTTP request metrics
- Database connection metrics
- Custom business metrics

### Tracing (Zipkin via micrometer)

- **Request Tracing**: End-to-end request flow visualization
- **Performance Analysis**: Latency breakdown by service
- **Error Tracking**: Trace error propagation across services

### Logging (Loki)

- **Structured Logging**: JSON format with correlation IDs
- **Log Queries**: Powerful query language for log analysis
- **Log Retention**: Configurable retention policies

### Grafana Dashboards
- **Application Metrics**: HTTP requests, response times, error rates
- **Database Metrics**: Connection pool, query performance
- **Business Metrics**: Subscription creation, user activity
- **Custom Dashboards**: FX-specific metrics and alerts

## 🤖 MCP Server & AI Integration

### MCP Server Features

- **SSE Communication**: Server-Sent Events for real-time MCP client communication
- **Tool Integration**: AI tools for subscription management via MCP protocol
- **Security**: MCP endpoints are publicly accessible for client connections

### Available MCP Tools

The service exposes the following tools for AI clients:

- `createFxSubscription(userId, currencyPair, thresholdValue, direction, notificationMethod)` - Creates a new FX rate
  subscription
- `updateFxSubscription(subscriptionId, newThresholdValue, direction, newNotificationMethod)` - Updates an existing
  subscription
- `deleteFxSubscription(subscriptionId)` - Deletes a subscription
- `getFxSubscriptionsForUser(userId)` - Retrieves all subscriptions for a user

### AI Integration Architecture

- **MCP Server**: This service acts as an MCP server
- **FX MCP Client**: Separate service that connects to this MCP server
- **Tool Execution**: AI tools are executed through the MCP protocol
- **Natural Language**: AI interactions are handled by the MCP client

## 🔄 Event-Driven Architecture

### Kafka Integration

- **Topic**: `subscription-change-events`
- **Events**: SubscriptionCreated, SubscriptionUpdated, SubscriptionDeleted
- **Outbox Pattern**: Reliable event publishing

### Scheduled Tasks

- **Subscription Processing**: Every 5 minutes (configurable)
- **Event Publishing**: Automatic outbox processing
- **Status Updates**: Subscription lifecycle management

## 🧪 Testing

### Test Infrastructure

- **Unit Tests**: Comprehensive unit tests for all services
- **Integration Tests**: Controller and service integration tests
- **Repository Tests**: Data access layer testing with TestContainers
- **Cache Tests**: Redis cache integration tests with TestContainers
- **Security Tests**: JWT and security configuration testing

### TestContainers Integration

The project uses TestContainers for reliable, isolated testing:

- **PostgreSQL**: `PostgreSQLContainer` for database tests
- **Redis**: `RedisContainer` for cache tests  
- **Kafka**: `KafkaContainer` for messaging tests
- **Test Profiles**: Dedicated test profiles for each container type

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests SubscriptionsControllerIT

# Run with coverage
./gradlew test jacocoTestReport
```

## 🚀 Deployment

### Local Development Deployment

Use the automated build script for complete local setup:

```bash
# Quick setup - runs tests, builds, and starts all services
./build_and_run.sh
```

### Production Docker Deployment

```bash
# Build production Docker image
docker build -t fx-subscription-service .

# Run container with external dependencies
docker run -d \
  --name fx-subscription-service \
  -p 8443:8443 \
  -e FX_POSTGRES_HOST=your-postgres-host:5432 \
  -e FX_KAFKA_HOST=your-kafka-host:9092 \
  -e FX_KEYSTORE_PASSWORD=your_keystore_password \
  -e FX_JWT_SECRET_KEY=your_jwt_secret \
  --mount type=secret,source=keystore_p12 \
  fx-subscription-service
```

### Docker Compose Deployment

For complete stack deployment including dependencies:

```bash
# Local development with full observability
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d

# Production-like setup with core services only
docker compose up -d
```

## 📈 Performance

### Optimizations

- Virtual threads enabled
- Connection pooling
- Lazy loading for associations
- Transaction management

### Benchmarks

- **Response time**: < 100ms (95th percentile)
- **Throughput**: 1000+ requests/second
- **Cache hit ratio**: > 90% for subscription data
- **Database queries**: Optimized with indexes and caching

## 🔧 Development

### Project Structure

```
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/fx/subscription/service/
│   │   │       ├── controller/             # REST controllers
│   │   │       ├── service/                # Business logic
│   │   │       ├── repository/             # Data access
│   │   │       ├── model/                  # Entities
│   │   │       ├── dto/                    # Data transfer objects
│   │   │       ├── config/                 # Configuration classes
│   │   │       │   ├── CacheConfig.java    # Redis cache configuration
│   │   │       │   └── OpenApiConfig.java  # OpenAPI documentation configuration
│   │   │       ├── exception/              # Custom exceptions
│   │   │       └── ai/                     # MCP tool integration
│   │   │           └── tool/               # AI tools for MCP
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback-spring.xml
│   └── test/
│       ├── java/                           # Test classes
│       └── resources/
│           └── application.properties
├── api-docs/                               # Generated OpenAPI documentation
├── build_and_run.sh                        # Automated setup script
├── docker-compose.yml                      # Core services (app, DB, Kafka)
├── docker-compose.observability.yml        # Monitoring stack
├── Dockerfile                              # Production container image
├── Dockerfile.local                        # Local development container
├── prometheus.yml                          # Prometheus configuration
└── gradle/                                 # Gradle wrapper
```

### Code Quality

- SonarQube integration
- Code formatting with Checkstyle
- 95% plus code and line coverage with JaCoCo
- Automated quality checks in build pipeline

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:

- Create an issue in the repository
- Check the documentation
- Review existing issues and discussions

## 📝 Version History

- **v1.0.0** - Initial release with core subscription management
- **v1.1.0** - Added AI integration and improved security
- **v1.2.0** - Enhanced monitoring and observability features with Grafana stack
- **v1.3.0** - Migrated to PostgreSQL with TestContainers for reliable testing
- **v2.0.0** - Refactored to MCP server architecture, moved AI chat to separate client
- **v2.0.3** - Added Redis caching, enhanced TestContainers integration, and improved DTOs
- **v2.1.0** - Enhanced Docker setup with automated build script and comprehensive local development environment

---

**Note**: This service now provides a complete containerized development environment with automated setup via `build_and_run.sh`. The Docker-based setup includes PostgreSQL, Redis, Kafka, and full observability stack (Grafana, Prometheus, Loki, Zipkin) for a production-like local development experience. Redis caching has been implemented for improved performance and reduced database load. The AI chat functionality is handled by a separate FX MCP Client service.
