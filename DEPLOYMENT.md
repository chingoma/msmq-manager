# MSMQ Manager - Deployment Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Docker Deployment](#docker-deployment)
4. [Production Deployment](#production-deployment)
5. [Configuration Management](#configuration-management)
6. [Monitoring Setup](#monitoring-setup)
7. [Security Configuration](#security-configuration)
8. [Troubleshooting](#troubleshooting)

## Prerequisites

### System Requirements
- **Java**: OpenJDK 17 or Oracle JDK 17
- **Maven**: 3.9.5 or higher
- **Memory**: Minimum 2GB RAM, Recommended 4GB RAM
- **Disk Space**: Minimum 1GB free space
- **Operating System**: Windows, Linux, or macOS

### Software Dependencies
- **MSMQ**: Microsoft Message Queuing (Windows only)
- **Database**: H2 (development), PostgreSQL/MySQL (production)
- **Monitoring**: Prometheus, Grafana (optional)

## Local Development Setup

### 1. Clone and Build
```bash
# Clone the repository
git clone <repository-url>
cd msmq-manager

# Build the application
mvn clean install
```

### 2. Run Application
```bash
# Run with default profile (dev)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

### 3. Access Application
- **Application**: http://localhost:8080/msmq-manager
- **Health Check**: http://localhost:8080/msmq-manager/actuator/health
- **Metrics**: http://localhost:8080/msmq-manager/actuator/prometheus

## Docker Deployment

### 1. Build and Run with Docker Compose
```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f msmq-manager

# Stop services
docker-compose down
```

### 2. Individual Docker Commands
```bash
# Build the image
docker build -t msmq-manager .

# Run the container
docker run -d \
  --name msmq-manager \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  msmq-manager
```

### 3. Docker Compose Services
- **msmq-manager**: Main application (port 8080)
- **prometheus**: Metrics collection (port 9090)
- **grafana**: Metrics visualization (port 3000)
- **h2-database**: Development database (port 1521, 8181)

## Production Deployment

### 1. Environment Variables
```bash
# Required environment variables
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/msmq_manager
export SPRING_DATASOURCE_USERNAME=msmq_user
export SPRING_DATASOURCE_PASSWORD=secure_password
export MSMQ_CONNECTION_HOST=msmq-server.company.com
export MSMQ_CONNECTION_PORT=1801
```

### 2. JVM Configuration
```bash
# Production JVM options
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxGCPauseMillis=200"
```

### 3. Systemd Service (Linux)
```ini
# /etc/systemd/system/msmq-manager.service
[Unit]
Description=MSMQ Manager Application
After=network.target

[Service]
Type=simple
User=msmq
WorkingDirectory=/opt/msmq-manager
ExecStart=/usr/bin/java $JAVA_OPTS -jar msmq-manager.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 4. Windows Service
```cmd
# Install as Windows service using NSSM
nssm install MSMQManager "C:\Program Files\Java\jdk-17\bin\java.exe" "-jar msmq-manager.jar"
nssm set MSMQManager AppDirectory "C:\msmq-manager"
nssm set MSMQManager AppParameters "-Dspring.profiles.active=prod"
```

## Configuration Management

### 1. Profile-Specific Configuration
```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/msmq_manager
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  msmq:
    connection:
      host: ${MSMQ_HOST}
      port: ${MSMQ_PORT}
      timeout: 30000
      retry-count: 5
```

### 2. External Configuration
```bash
# Use external configuration file
java -jar msmq-manager.jar --spring.config.location=file:/etc/msmq-manager/application.yml

# Use environment-specific properties
java -jar msmq-manager.jar --spring.profiles.active=prod
```

### 3. Configuration Validation
```bash
# Validate configuration
mvn spring-boot:run -Dspring-boot.run.profiles=prod --dry-run
```

## Monitoring Setup

### 1. Prometheus Configuration
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'msmq-manager'
    static_configs:
      - targets: ['msmq-manager:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
```

### 2. Grafana Dashboards
- Import pre-configured dashboards from `monitoring/grafana/dashboards/`
- Configure Prometheus as data source
- Set up alerts for critical metrics

### 3. Key Metrics to Monitor
- **Performance**: Response times, throughput
- **Errors**: Error rates, exception types
- **Resources**: Memory usage, CPU utilization
- **MSMQ**: Connection status, queue depths

## Security Configuration

### 1. Authentication
```yaml
# application-prod.yml
spring:
  security:
    user:
      name: ${ADMIN_USERNAME}
      password: ${ADMIN_PASSWORD}
      roles: ADMIN
```

### 2. HTTPS Configuration
```yaml
# application-prod.yml
server:
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: msmq-manager
  port: 8443
```

### 3. Network Security
```bash
# Firewall rules
ufw allow 8443/tcp  # HTTPS
ufw allow 8080/tcp  # HTTP (if needed)
ufw allow 9090/tcp  # Prometheus
ufw allow 3000/tcp  # Grafana
```

## Troubleshooting

### 1. Common Issues

#### Application Won't Start
```bash
# Check logs
tail -f logs/msmq-manager.log

# Check Java version
java -version

# Check port availability
netstat -tulpn | grep 8080
```

#### MSMQ Connection Issues
```bash
# Verify MSMQ service is running
sc query MSMQ

# Check network connectivity
telnet msmq-server 1801

# Verify firewall settings
netsh advfirewall firewall show rule name="MSMQ"
```

#### Performance Issues
```bash
# Check JVM metrics
jstat -gc <pid>

# Check system resources
top -p <pid>
free -h
```

### 2. Log Analysis
```bash
# Search for errors
grep "ERROR" logs/msmq-manager.log

# Search for specific request
grep "request-id" logs/msmq-manager.log

# Monitor real-time logs
tail -f logs/msmq-manager.log | grep "ERROR\|WARN"
```

### 3. Health Checks
```bash
# Application health
curl -f http://localhost:8080/msmq-manager/actuator/health

# MSMQ connection status
curl -f http://localhost:8080/msmq-manager/msmq/status

# System metrics
curl -f http://localhost:8080/msmq-manager/actuator/metrics
```

## Maintenance

### 1. Backup Procedures
```bash
# Database backup
pg_dump msmq_manager > backup_$(date +%Y%m%d_%H%M%S).sql

# Configuration backup
tar -czf config_backup_$(date +%Y%m%d).tar.gz config/
```

### 2. Update Procedures
```bash
# Stop application
systemctl stop msmq-manager

# Backup current version
cp msmq-manager.jar msmq-manager.jar.backup

# Deploy new version
cp new-msmq-manager.jar msmq-manager.jar

# Start application
systemctl start msmq-manager

# Verify health
curl -f http://localhost:8080/msmq-manager/actuator/health
```

### 3. Log Rotation
```bash
# Configure logrotate
cat > /etc/logrotate.d/msmq-manager << EOF
/opt/msmq-manager/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 msmq msmq
    postrotate
        systemctl reload msmq-manager
    endscript
}
EOF
```

## Support

For additional support:
- **Documentation**: Check the README.md file
- **Issues**: Report bugs via GitHub issues
- **Community**: Join our developer community
- **Enterprise Support**: Contact enterprise support team

---

**Note**: This deployment guide covers the most common scenarios. For enterprise-specific requirements, please consult with your infrastructure team.
