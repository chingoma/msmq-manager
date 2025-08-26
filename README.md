# MSMQ Manager - Enterprise Spring Boot Application

## Overview

MSMQ Manager is a production-ready, enterprise-grade Spring Boot application designed to manage Microsoft Message Queuing (MSMQ) operations. The application provides comprehensive REST APIs for queue management, message operations, and system monitoring, following enterprise best practices and security standards.

## Features

### Core Functionality
- **Queue Management**: Create, delete, update, and monitor MSMQ queues
- **Message Operations**: Send, receive, peek, and purge messages
- **Connection Management**: Establish, monitor, and manage MSMQ connections
- **Message Parsing**: Transform messages between API and MSMQ formats
- **Monitoring & Health**: Comprehensive system health checks and performance metrics

### Enterprise Features
- **Response Code System**: Custom response codes starting from 600 for business logic
- **Comprehensive Logging**: Structured logging with request tracking
- **Error Handling**: Graceful error handling with detailed error information
- **Security**: Spring Security integration with configurable authentication
- **Monitoring**: Actuator endpoints for health checks and metrics
- **Profiles**: Environment-specific configurations (dev, test, staging, prod)

## Architecture

### Layered Architecture
```
┌─────────────────┐
│   Controllers   │  ← REST API endpoints
├─────────────────┤
│    Services     │  ← Business logic layer
├─────────────────┤
│   Utilities     │  ← MSMQ operations, parsing, connection management
├─────────────────┤
│      DTOs       │  ← Data transfer objects
└─────────────────┘
```

### Key Components
- **MsmqController**: REST API endpoints for all MSMQ operations
- **MsmqService**: Business logic implementation
- **MsmqConnectionManager**: MSMQ connection management
- **MsmqQueueManager**: Queue operations management
- **MsmqMessageParser**: Message parsing and validation

## Technology Stack

- **Java 17**: Latest LTS version for enterprise applications
- **Spring Boot 3.2.0**: Latest stable Spring Boot version
- **Spring Security**: Authentication and authorization
- **Spring Actuator**: Health checks and monitoring
- **Jackson**: JSON processing and serialization
- **SLF4J + Logback**: Structured logging
- **Micrometer**: Metrics collection and monitoring
- **Maven**: Build and dependency management

## Response Code System

All API responses return HTTP 200 status code with business status indicated in the response body:

### Success Codes (600-609)
- `600`: Operation completed successfully
- `601`: Queue created successfully
- `602`: Message sent to queue successfully
- `603`: Message received from queue successfully

### Error Codes (610+)
- `610-619`: Validation errors
- `620-629`: Business logic errors
- `630-639`: System errors
- `640-649`: Authentication/Authorization errors
- `650-659`: Resource errors
- `660-669`: MSMQ-specific errors

## API Endpoints

### Queue Management
```
POST   /api/v1/msmq/queues                    # Create queue
GET    /api/v1/msmq/queues                    # List all queues
GET    /api/v1/msmq/queues/{queueName}        # Get queue details
DELETE /api/v1/msmq/queues/{queueName}        # Delete queue
```

### Message Operations
```
POST   /api/v1/msmq/queues/{queueName}/messages     # Send message
GET    /api/v1/msmq/queues/{queueName}/messages     # Receive message
GET    /api/v1/msmq/queues/{queueName}/messages/peek # Peek message
```

### Connection Management
```
GET    /api/v1/msmq/connection/status        # Get connection status
POST   /api/v1/msmq/connection/connect       # Establish connection
```

### Health & Monitoring
```
GET    /api/v1/msmq/health                   # Health check
GET    /api/v1/msmq/metrics/performance      # Performance metrics
```

## Configuration

### Profiles
The application supports multiple environment profiles:

- **dev**: Development environment with debug logging
- **test**: Testing environment
- **staging**: Staging environment
- **prod**: Production environment with optimized settings

### Key Configuration Properties
```yaml
msmq:
  connection:
    host: localhost
    port: 1801
    timeout: 30000
    retry-attempts: 3
  queue:
    default-timeout: 60000
    max-message-size: 4194304
  monitoring:
    enabled: true
    interval: 5000
```

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- MSMQ service (for production use)

### Building the Application
```bash
# Build with default profile (dev)
mvn clean install

# Build with specific profile
mvn clean install -Pprod

# Run tests
mvn test
```

### Running the Application
```bash
# Run with default profile
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring.profiles.active=prod

# Run JAR file
java -jar target/msmq-manager-1.0.0.jar --spring.profiles.active=prod
```

### Docker Support
```bash
# Build Docker image
docker build -t msmq-manager .

# Run Docker container
docker run -p 8080:8080 msmq-manager
```

## Development

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/enterprise/msmq/
│   │       ├── controller/          # REST controllers
│   │       ├── service/             # Business logic
│   │       ├── dto/                 # Data transfer objects
│   │       ├── enums/               # Enumerations
│   │       ├── exception/           # Custom exceptions
│   │       └── util/                # Utility classes
│   └── resources/
│       ├── application.yml          # Main configuration
│       └── logback-spring.xml      # Logging configuration
└── test/                            # Test classes
```

### Adding New Features
1. **DTOs**: Create data transfer objects in `dto` package
2. **Services**: Implement business logic in `service` package
3. **Controllers**: Add REST endpoints in `controller` package
4. **Utilities**: Create helper classes in `util` package
5. **Tests**: Add comprehensive tests in `test` package

### Code Quality
The project includes several quality assurance tools:
- **Checkstyle**: Code style enforcement
- **SpotBugs**: Static analysis for bug detection
- **JaCoCo**: Code coverage reporting
- **Maven Surefire**: Test execution

## Monitoring & Observability

### Health Checks
- Application health status
- MSMQ connection health
- Queue availability
- System resource monitoring

### Metrics
- Message processing rates
- Queue performance metrics
- Error rates and statistics
- System resource utilization

### Logging
- Structured logging with correlation IDs
- Request/response logging
- Error logging with stack traces
- Performance logging

## Security

### Authentication
- Basic authentication (configurable)
- Role-based access control
- Secure password handling

### Authorization
- Endpoint-level security
- Queue-level permissions
- Audit logging

## Performance & Scalability

### Optimizations
- Connection pooling
- Message batching
- Asynchronous processing
- Caching strategies

### Monitoring
- Performance metrics collection
- Resource utilization tracking
- Alert thresholds
- Capacity planning

## Troubleshooting

### Common Issues
1. **Connection Failures**: Check MSMQ service availability and network connectivity
2. **Queue Access Denied**: Verify user permissions and authentication
3. **Message Processing Errors**: Check message format and validation rules
4. **Performance Issues**: Monitor system resources and queue depths

### Debug Mode
Enable debug logging for detailed troubleshooting:
```yaml
logging:
  level:
    com.enterprise.msmq: DEBUG
```

## Contributing

### Development Guidelines
1. Follow Java coding standards
2. Write comprehensive unit tests
3. Document all public APIs
4. Use meaningful commit messages
5. Follow the established architecture patterns

### Testing Strategy
- Unit tests for all business logic
- Integration tests for API endpoints
- Performance tests for critical operations
- Security tests for authentication/authorization

## License

This project is licensed under the Enterprise License. See LICENSE file for details.

## Support

For enterprise support and questions:
- Email: support@enterprise.com
- Documentation: https://docs.enterprise.com/msmq-manager
- Issue Tracking: https://issues.enterprise.com/msmq-manager

## Version History

- **1.0.0**: Initial release with core MSMQ functionality
- Future versions will include additional features and improvements

---

**Note**: This application is designed for enterprise use and includes comprehensive error handling, logging, and monitoring capabilities. For production deployment, ensure proper security configurations and monitoring setup.
