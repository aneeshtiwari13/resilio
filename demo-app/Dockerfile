# ==========================================
# Stage 1: Build Application
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Cache dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code and package executable JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Stage 2: Production-Conscious Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine

# Security: Create non-privileged system user and group
RUN addgroup -S -g 10001 resilio && adduser -S -u 10001 -G resilio resilio

WORKDIR /app

# Copy jar from builder stage and assign ownership
COPY --from=builder --chown=resilio:resilio /workspace/target/*.jar app.jar

# Configurable environment variables
ENV SERVER_PORT=8080 \
    APP_VERSION=1.0.0 \
    APP_COMMIT_SHA=default-sha \
    APP_BUILD_NUMBER=1 \
    APP_ENVIRONMENT=production \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

USER resilio:resilio

EXPOSE 8080

# Production container health probe
HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
