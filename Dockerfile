# ══════════════════════════════════════════════════════════════════
#  BARS — Spring Boot Backend  (multi-stage build)
#  Stage 1 : Maven build  →  fat JAR
#  Stage 2 : Slim JRE runtime
# ══════════════════════════════════════════════════════════════════

# ── Stage 1: Build ────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper + pom first (layer-cache friendly)
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Download dependencies without building source (cached unless pom changes)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and package (skip tests — run them in CI, not Docker build)
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ── Stage 2: Runtime ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S bars && adduser -S bars -G bars

WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Change ownership to the non-root user
RUN chown bars:bars app.jar

USER bars

# Spring Boot listens on 8080 by default
EXPOSE 8080

# Use the production profile; all secrets are injected as env vars at runtime
ENTRYPOINT ["java", \
  "-Dspring.profiles.active=prod", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
