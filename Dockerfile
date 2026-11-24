# ---------- Stage 1: build the JAR with Gradle ----------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# copy only the build files first: Docker keeps the dependencies in cache
# as long as build.gradle does not change (faster rebuilds)
COPY gradlew settings.gradle* build.gradle* ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# then copy the source code and build the executable JAR (tests already run in Jenkins)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ---------- Stage 2: small runtime image (JRE only) ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# do not run the application as root
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/build/libs/*.jar app.jar
USER app

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
