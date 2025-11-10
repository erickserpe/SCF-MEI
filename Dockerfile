# ===================================
# Multi-stage build for optimized image size
# ===================================
# Stage 1: Build the application
# Uses Maven with Eclipse Temurin JDK 17 to compile the application
# ===================================

FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first to leverage Docker layer caching
# Dependencies will only be re-downloaded if pom.xml changes
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
# -DskipTests: Skip tests for faster builds (run tests in CI/CD)
# -B: Batch mode (non-interactive)
# -e: Show errors
# -T 1C: Use 1 thread per CPU core for parallel builds
RUN mvn clean package -DskipTests -B -e -T 1C

# ===================================
# Stage 2: Runtime image
# Uses minimal Alpine Linux with JRE 17 for smaller image size
# ===================================

FROM eclipse-temurin:17-jre-alpine

# Metadata labels (best practice for production)
LABEL maintainer="SCF-MEI Team"
LABEL description="Sistema de Controle Financeiro para MEI"
LABEL version="1.0.0"

WORKDIR /app

# Install curl for healthchecks (useful for Docker Compose and Kubernetes)
RUN apk add --no-cache curl

# Create non-root user for security
# CRITICAL: Never run applications as root in production!
RUN addgroup -S spring && adduser -S spring -G spring

# Create directories with proper permissions
# /app/uploads: Para arquivos de upload (comprovantes, etc)
# /var/log/ellomei: Para logs da aplicacao em producao
RUN mkdir -p /app/uploads /var/log/ellomei && \
    chown -R spring:spring /app /var/log/ellomei

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership of the JAR
RUN chown spring:spring app.jar

# Switch to non-root user
# All subsequent commands and the application will run as this user
USER spring:spring

# Expose port 8080
EXPOSE 8080

# Environment variables for JVM optimization
# These can be overridden in docker-compose.yml or at runtime
#
# Default values (suitable for development):
# -Xms512m: Initial heap size (512MB)
# -Xmx1024m: Maximum heap size (1GB)
# -XX:+UseG1GC: Use G1 Garbage Collector (better for containers)
# -XX:MaxGCPauseMillis=200: Target max GC pause time
# -XX:+UseContainerSupport: Respect container memory limits
# -XX:InitialRAMPercentage=50.0: Use 50% of container memory for initial heap
# -XX:MaxRAMPercentage=80.0: Use up to 80% of container memory for max heap
# -Djava.security.egd=file:/dev/./urandom: Faster startup (non-blocking random)
# -Dfile.encoding=UTF-8: Ensure UTF-8 encoding
# -Duser.timezone=America/Sao_Paulo: Set timezone
ENV JAVA_OPTS="-Xms512m \
    -Xmx1024m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=50.0 \
    -XX:MaxRAMPercentage=80.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=America/Sao_Paulo"

# Healthcheck (optional, but recommended for production)
# Checks if the application is responding on port 8080
# Interval: Check every 30 seconds
# Timeout: Wait 10 seconds for response
# Start period: Wait 60 seconds before first check (app startup time)
# Retries: Mark unhealthy after 3 failed checks
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application with JAVA_OPTS support
# Using 'exec' form to ensure proper signal handling (SIGTERM for graceful shutdown)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

