# Build the selected service using Java 25 and the Gradle Wrapper.
FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace
COPY . .

ARG MODULE

# Normalize Windows line endings, build the executable Spring Boot JAR,
# and copy it to a fixed path for the runtime stage.
RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew --no-daemon ":services:${MODULE}:bootJar" \
    && mkdir /output \
    && find "services/${MODULE}/build/libs" \
       -name '*.jar' ! -name '*-plain.jar' \
       -exec cp {} /output/app.jar \;

# Run the application without build tools or source files.
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build --chown=app:app /output/app.jar app.jar

# Use the same internal HTTP port for every service container.
ENV SERVER_PORT=8080
EXPOSE 8080

USER app

# Start Java directly so it receives container shutdown signals.
ENTRYPOINT ["java", "-jar", "app.jar"]