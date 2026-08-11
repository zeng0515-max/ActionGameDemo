# ===== Stage 1: Build =====
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy only pom.xml first (cache Maven dependencies)
COPY server/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY server/src ./src
RUN mvn clean package -DskipTests -B

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="ActionGameDemo"
LABEL description="Java Authoritative Battle Server (Netty + Protobuf)"

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy the built JAR
COPY --from=builder /build/target/action-game-server-1.0.0-SNAPSHOT.jar app.jar

# Copy entrypoint and run the server as a non-root user
COPY server/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Create directories for logs and replays
RUN mkdir -p /app/logs /app/replays /app/config
RUN addgroup -S -g 10001 actiongame \
    && adduser -D -u 10001 -G actiongame actiongame \
    && chown -R actiongame:actiongame /app

USER actiongame

# Copy config files if any
COPY server/src/main/resources/config/ /app/config/

# Environment variables (overridable)
ENV SERVER_PORT=9090
ENV METRICS_PORT=9091
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=50"

# Expose WebSocket and metrics ports
EXPOSE 9090
EXPOSE 9091

# Volume for persistent data
VOLUME ["/app/logs", "/app/replays"]

# Healthcheck: verify the metrics health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -sf http://localhost:${METRICS_PORT}/health || exit 1

# Start command
ENTRYPOINT ["/app/docker-entrypoint.sh"]
