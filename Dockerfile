# ─────────────────────────────────────────
# Stage 1 — Build with Gradle
# ─────────────────────────────────────────
FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /app

# Copy gradle wrapper and dependency files first (layer cache)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Resolve dependencies (cached unless build.gradle changes)
RUN gradle dependencies --no-daemon --quiet || true

# Copy full source
COPY src ./src

# Build jar, skip tests (tests run in GitHub Actions separately)
RUN gradle bootJar --no-daemon -x test

# ─────────────────────────────────────────
# Stage 2 — Minimal JRE runtime image
# ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built jar
COPY --from=build /app/build/libs/shardedsagawallet-0.0.1-SNAPSHOT.jar app.jar

# sharding.yml is loaded from classpath (already bundled inside jar via src/main/resources)
# No need to copy separately

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]