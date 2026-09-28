# syntax=docker/dockerfile:1.7

# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Cache Gradle dependencies separately from sources for faster rebuilds
COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY server/build.gradle.kts server/
COPY shared/build.gradle.kts shared/
COPY composeApp/build.gradle.kts composeApp/
RUN chmod +x gradlew && \
    ./gradlew --no-daemon :server:dependencies > /dev/null 2>&1 || true

# Build the fat JAR
COPY server server
COPY shared shared
RUN ./gradlew --no-daemon :server:shadowJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app && \
    mkdir -p /app && chown -R app:app /app
USER app

COPY --from=build /workspace/server/build/libs/*-all.jar /app/server.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/server.jar"]
