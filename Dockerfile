# Multi-stage build for MSMQ Manager Application
# Stage 1: Build the application
FROM maven:3.9.5-openjdk-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM openjdk:17-jre-slim

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -r msmq && useradd -r -g msmq msmq

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create necessary directories
RUN mkdir -p /app/logs /app/config && \
    chown -R msmq:msmq /app

# Switch to non-root user
USER msmq

# Expose application port
EXPOSE 8080

# Set JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
