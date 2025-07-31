# FX Subscription Service

A comprehensive Spring Boot microservice for managing foreign exchange (FX) rate subscriptions with real-time notifications, MCP (Model Context Protocol) server capabilities, and event-driven architecture.

## 🚀 Features

### Core Functionality
- **User Management**: Complete user registration, authentication, and profile management
- **FX Subscription Management**: Create, update, delete, and monitor currency pair subscriptions
- **Real-time Notifications**: Multi-channel notification support (email, SMS, push)
- **MCP Server Integration**: Model Context Protocol server for AI tool interactions
- **Event-Driven Architecture**: Kafka-based event publishing for subscription changes
- **Security**: JWT-based authentication with role-based access control

### Technical Features
- **Database**: H2 in-memory database with JPA/Hibernate
- **API Documentation**: RESTful APIs with comprehensive error handling
- **Monitoring**: Prometheus metrics and health endpoints
- **Observability**: Distributed tracing with Zipkin integration
- **Scheduling**: Automated subscription processing and event publishing
- **Testing**: Comprehensive unit and integration tests

## 📚 Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Client    │    │   Mobile App    │    │   AI Chat Bot   │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
           │                      │                      │
           └──────────────────────┼──────────────────────┘
                                  │
                     ┌─────────────▼─────────────┐
                     │   FX Subscription Service │
                     │       (MCP Server)        │
                     │                           │
                     │  ┌─────────────────────┐  │
                     │  │   REST Controllers  │  │
                     │  └─────────┬───────────┘  │
                     │            │              │
                     │  ┌─────────▼───────────┐  │
                     │  │   Business Logic    │  │
                     │  └─────────┬───────────┘  │
                     │            │              │
                     │  ┌─────────▼───────────┐  │
                     │  │   Data Access Layer │  │
                     │  └─────────┬───────────┘  │
                     └────────────┼──────────────┘
                                  │
                     ┌─────────────▼─────────────┐
                     │        H2 Database        │
                     └───────────────────────────┘
                                  │
                     ┌─────────────▼─────────────┐
                     │        Kafka Topics       │
                     │  - subscription-changes   │
                     └───────────────────────────┘
```

## 📝 Prerequisites

- Java 21+
- Gradle 8.0+
- Docker (for Prometheus monitoring)
- FX MCP Client (for AI interactions)

## 🛠️ Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd fx-subscription-service
```

### 2. Build the Application
```bash
./gradlew build
```

### 3. Run the Application
```bash
./gradlew bootRun
```

The application will start on `https://localhost:8443`

### 4. Start Prometheus (Optional)
```bash
docker run -d --name prometheus -it -p 9090:9090 \
  -v ./prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

### 5. Start FX MCP Client (for AI features)
```bash
# Ensure the FX MCP Client is running and configured to connect to this service
# The MCP client will connect via SSE endpoint at /sse
```

## ⚙️ Configuration

### Environment Variables
```bash
# SSL Configuration
KEYSTORE_PASSWORD=your_keystore_password
KEY_PASSWORD=your_key_password

# JWT Configuration
JWT_SECRET_KEY=your_jwt_secret_key

# Database (H2 - default)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=password

# Kafka Configuration
spring.kafka.bootstrap-servers=broker:29092
spring.kafka.topic.subscription-changes=subscription-change-events

# MCP Server Configuration
#spring.ai.mcp.server.name=fx-mcp-server
```

## 📄 API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/auth/signup
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
POST /api/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password123"
}
```

### Subscription Endpoints

#### Create Subscription
```http
POST /api/subscriptions
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "currencyPair": "GBP/USD",
  "threshold": 1.25,
  "direction": "ABOVE",
  "notificationChannels": ["email", "sms"]
}
```

#### Get My Subscriptions
```http
GET /api/subscriptions/my
Authorization: Bearer <jwt_token>
```

#### Update Subscription
```http
PUT /api/subscriptions/{id}
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
DELETE /api/subscriptions/{id}
Authorization: Bearer <jwt_token>
```

### User Management Endpoints

#### Get All Users (Admin only)
```http
GET /api/users?page=0&size=20
Authorization: Bearer <jwt_token>
```

#### Get User by ID
```http
GET /api/users/{id}
Authorization: Bearer <jwt_token>
```

#### Update User
```http
PUT /api/users/{id}
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

#### MCP Endpoints
```http
GET /mcp/**
```

**Note**: These endpoints are used by the FX MCP Client for AI tool interactions and are not meant for direct human consumption.

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
- Distributed tracing with Zipkin
- Request correlation IDs
- Performance monitoring

### Logging (Loki)
- Loki logging appender
- Available in grafana logs

## 🤖 MCP Server & AI Integration

### MCP Server Features
- **SSE Communication**: Server-Sent Events for real-time MCP client communication
- **Tool Integration**: AI tools for subscription management via MCP protocol
- **Security**: MCP endpoints are publicly accessible for client connections

### Available MCP Tools
The service exposes the following tools for AI clients:
- `createFxSubscription(userId, currencyPair, thresholdValue, direction, notificationMethod)` - Creates a new FX rate subscription
- `updateFxSubscription(subscriptionId, newThresholdValue, direction, newNotificationMethod)` - Updates an existing subscription
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

### Test Coverage
- Unit tests for all services
- Integration tests for controllers
- Repository layer testing
- Security configuration testing

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

### Docker
```bash
# Build Docker image
docker build -t fx-subscription-service .

# Run container
docker run -p 8443:8443 fx-subscription-service
```

### Docker Compose
```bash
docker-compose up -d
```

## 📈 Performance

### Optimizations
- Virtual threads enabled
- Connection pooling
- Lazy loading for associations
- Transaction management

### Benchmarks
- Response time: < 100ms (95th percentile)
- Throughput: 1000+ requests/second
- Database queries: Optimized with indexes

## 🔧 Development

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/example/fx/subscription/service/
│   │       ├── controller/     # REST controllers
│   │       ├── service/        # Business logic
│   │       ├── repository/     # Data access
│   │       ├── model/          # Entities
│   │       ├── dto/            # Data transfer objects
│   │       ├── config/         # Configuration classes
│   │       ├── exception/      # Custom exceptions
│   │       └── ai/             # MCP tool integration
│   │           └── tool/       # AI tools for MCP
│   └── resources/
│       ├── application.properties
│       └── logback-spring.xml
└── test/
    ├── java/                   # Test classes
    └── resources/
        └── application.properties
```

### Code Quality
- SonarQube integration
- Code formatting with Checkstyle
- 95% plus code and line coverage with jacoco

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
- **v2.0.0** - Refactored to MCP server architecture, moved AI chat to separate client

---

**Note**: This service is designed for development and testing purposes. For production deployment, consider using a production-grade database like PostgreSQL and proper infrastructure setup. The AI chat functionality is now handled by the separate FX MCP Client service.
