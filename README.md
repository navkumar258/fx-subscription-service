# FX Subscription Service

A comprehensive Spring Boot microservice for managing foreign exchange (FX) rate subscriptions with real-time notifications, AI-powered interactions, and event-driven architecture.

## 🚀 Features

### Core Functionality
- **User Management**: Complete user registration, authentication, and profile management
- **FX Subscription Management**: Create, update, delete, and monitor currency pair subscriptions
- **Real-time Notifications**: Multi-channel notification support (email, SMS, push)
- **Spring AI Integration**: Natural language processing for subscription management via (locally via docker) Ollama
- **Event-Driven Architecture**: Kafka-based event publishing for subscription changes
- **Security**: JWT-based authentication with role-based access control

### Technical Features
- **Database**: PostgreSQL database with JPA/Hibernate
- **API Documentation**: RESTful APIs with comprehensive error/exeception handling
- **Monitoring**: Prometheus metrics and health endpoints
- **Observability**: Distributed tracing with Zipkin, Logging with Loki - integrated with Grafana OSS
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
                     │       PostgreSQL DB       │
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
- Docker Compose (for observability stack)
- Ollama (for AI features)

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

### 3(a). Run with Docker Compose (Optional)
```bash
# Build & Start all services including PostgreSQL
docker compose build
docker compose up -d

# Check service status
docker compose ps
```

The application will start on `https://localhost:8443`

### 4. Start Observability Stack (Optional)
```bash
# Start Grafana OSS
docker run -d --name grafana -it -p 3000:3000 grafana/grafana:latest

# Start prometheus for monitoring, metrics
docker run -d --name prometheus -it -p 9090:9090 -v ./prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus

# Start Loki for log aggregation
docker run -d --name loki -it -p 3100:3100 grafana/loki:latest

# Start Zipkin for distributed tracing
docker run -d --name zipkin -it -p 9411:9411 openzipkin/zipkin:latest

# Or use Docker Compose for the entire observability stack
docker-compose -f docker-compose.observability.yml up -d
```

### 5. Access Observability Tools
```bash
# Grafana Dashboard: http://localhost:3000 (admin/admin)
# Loki Logs: http://localhost:3100
# Zipkin Traces: http://localhost:9411
# Prometheus Metrics: http://localhost:9090
```

### 6. Start Ollama (for AI features)
```bash
# Install Ollama first, then run:
ollama run qwen3
```

## ⚙️ Configuration

### Environment Variables
```bash
# SSL Configuration
KEYSTORE_PASSWORD=your_keystore_password
KEY_PASSWORD=your_key_password

# JWT Configuration
JWT_SECRET_KEY=your_jwt_secret_key

# Database (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/fx_subscription_db
spring.datasource.username=postgres
spring.datasource.password=password

# Kafka Configuration
spring.kafka.bootstrap-servers=broker:29092
spring.kafka.topic.subscription-changes=subscription-change-events

# Observability Configuration
management.tracing.sampling.probability=1.0
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
logging.logback.loki.enabled=true
logging.logback.loki.url=http://localhost:3100/loki/api/v1/push

# AI Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=qwen3
```

## 📄 API Documentation

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

#### Get My Subscriptions
```http
GET /api/v1/subscriptions/my
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

### AI Chat Endpoint

#### Natural Language Subscription Management
```http
GET /api/v1/ai/fx?query=Create a subscription for GBP/USD above 1.25 with email notifications
Authorization: Bearer <jwt_token>
```

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

## 📊 Monitoring & Observability

### Health Endpoints
- `GET /actuator/health` - Application health status
- `GET /actuator/prometheus` - Prometheus metrics

### Prometheus Metrics
- HTTP request metrics
- Database connection metrics
- Custom business metrics

### Loki Log Aggregation
- **Structured Logging**: JSON format with correlation IDs
- **Log Queries**: Powerful query language for log analysis
- **Log Retention**: Configurable retention policies

### Zipkin Distributed Tracing (via micrometer)
- **Request Tracing**: End-to-end request flow visualization
- **Performance Analysis**: Latency breakdown by service
- **Error Tracking**: Trace error propagation across services

### Grafana Dashboards
- **Application Metrics**: HTTP requests, response times, error rates
- **Database Metrics**: Connection pool, query performance
- **Business Metrics**: Subscription creation, user activity
- **Custom Dashboards**: FX-specific metrics and alerts

## 🤖 AI Integration

### Features
- Natural language subscription management
- Conversational interface for FX operations
- Automated parameter extraction
- Error handling and validation

### Available AI Tools
- `createFxSubscription` - Create new subscriptions
- `updateFxSubscription` - Update existing subscriptions
- `deleteFxSubscription` - Delete subscriptions
- `getFxSubscriptionsForUser` - Retrieve user subscriptions

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

### Docker Compose (Recommended)
```bash
docker build -t fx-subscription-service .

docker run -p 8443:8443 fx-subscription-service
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
│   │       └── ai/             # AI integration
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
- **v1.2.0** - Enhanced monitoring and observability features with Grafana stack
- **v1.3.0** - Migrated to PostgreSQL with TestContainers for reliable testing

---

**Note**: This service uses PostgreSQL with TestContainers for development and testing, providing a production-ready database setup with isolated test environments.
