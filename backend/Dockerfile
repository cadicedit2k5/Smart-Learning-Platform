FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

ARG SERVICE

COPY . .

RUN if [ -f "${SERVICE}/src/main/resources/application.properties.example" ]; then \
        cp "${SERVICE}/src/main/resources/application.properties.example" \
           "${SERVICE}/src/main/resources/application.properties"; \
    fi \
    && if [ -f "${SERVICE}/src/main/resources/application.yml.example" ]; then \
        cp "${SERVICE}/src/main/resources/application.yml.example" \
           "${SERVICE}/src/main/resources/application.yml"; \
    fi \
    && chmod +x mvnw \
    && ./mvnw -pl "${SERVICE}" -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG SERVICE

RUN addgroup -S spring \
    && adduser -S spring -G spring

COPY --from=builder \
    --chown=spring:spring \
    /workspace/${SERVICE}/target/${SERVICE}-*.jar \
    /app/app.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]