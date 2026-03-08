# Production image optimized with: Spring AOT processing (-Pnative),
# extracted layered JAR (for cache-friendly Docker layers), and Java AOT cache training.
# This follows Spring Boot 4 packaging guidance for efficient deployments and AOT cache usage.

ARG RUNTIME_IMAGE=eclipse-temurin:25-jre

FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /workspace

ENV TZ=America/Sao_Paulo

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN set -eux; \
    mvn -B -DskipTests -DskipITs -Pnative package; \
    JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' ! -name '*-stubs.jar' | head -n 1)"; \
    test -n "${JAR_FILE}"; \
    cp "${JAR_FILE}" target/application.jar

FROM ${RUNTIME_IMAGE} AS extract
WORKDIR /opt/app

ENV TZ=America/Sao_Paulo

COPY --from=build /workspace/target/application.jar application.jar

# Extracts Spring Boot layers to improve Docker caching in rebuilds.
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM ${RUNTIME_IMAGE} AS train
WORKDIR /opt/app

ENV TZ=America/Sao_Paulo

COPY --from=extract /opt/app/extracted/dependencies/ ./
COPY --from=extract /opt/app/extracted/spring-boot-loader/ ./
COPY --from=extract /opt/app/extracted/snapshot-dependencies/ ./
COPY --from=extract /opt/app/extracted/application/ ./
COPY docker/training-collection.sh /opt/app/training-collection.sh
COPY docker/run-training.sh /opt/app/run-training.sh

# Optional hook to pass profile/properties to the training run.
# Example:
# docker build --build-arg TRAINING_APP_ARGS="--spring.profiles.active=aot-training" ...
ARG TRAINING_MODE=basic
ARG TRAINING_APP_ARGS=
ARG TRAINING_WRITER_URL=jdbc:h2:mem:aot-training;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
ARG TRAINING_WRITER_USERNAME=sa
ARG TRAINING_WRITER_PASSWORD=
ARG TRAINING_READER_URL=jdbc:h2:mem:aot-training;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
ARG TRAINING_READER_USERNAME=sa
ARG TRAINING_READER_PASSWORD=

# Training run generates app.aot.
# spring.context.exit=onRefresh exits as soon as context is refreshed.
RUN set -eux; \
    case "${TRAINING_MODE}" in \
      basic) \
        java \
          -XX:+UseZGC \
          -XX:MaxRAMPercentage=75.0 \
          -XX:InitialRAMPercentage=50.0 \
          -XX:+ExitOnOutOfMemoryError \
          -Duser.timezone=America/Sao_Paulo \
          -Dspring.aot.enabled=true \
          -XX:AOTCacheOutput=app.aot \
          -Dspring.context.exit=onRefresh \
          -jar application.jar ${TRAINING_APP_ARGS} \
        ;; \
      full) \
        apt-get update \
          && apt-get install -y --no-install-recommends curl tzdata \
          && rm -rf /var/lib/apt/lists/*; \
        chmod +x /opt/app/run-training.sh /opt/app/training-collection.sh; \
        APP_DATASOURCE_WRITER_URL="${TRAINING_WRITER_URL}" \
        APP_DATASOURCE_WRITER_USERNAME="${TRAINING_WRITER_USERNAME}" \
        APP_DATASOURCE_WRITER_PASSWORD="${TRAINING_WRITER_PASSWORD}" \
        APP_DATASOURCE_READER_URL="${TRAINING_READER_URL}" \
        APP_DATASOURCE_READER_USERNAME="${TRAINING_READER_USERNAME}" \
        APP_DATASOURCE_READER_PASSWORD="${TRAINING_READER_PASSWORD}" \
        SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop \
        TRAINING_JAR=application.jar \
        TRAINING_PROFILE=aot-training \
        TRAINING_EXTRA_JAVA_OPTS="-Dspring.aot.enabled=true" \
        /opt/app/run-training.sh \
        ;; \
      *) \
        echo "Unsupported TRAINING_MODE='${TRAINING_MODE}'. Use basic or full." >&2; \
        exit 1 \
        ;; \
    esac

FROM ${RUNTIME_IMAGE} AS runtime
WORKDIR /opt/app

ENV TZ=America/Sao_Paulo

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system appuser \
    && useradd --system --gid appuser --home-dir /opt/app appuser

COPY --from=extract --chown=appuser:appuser /opt/app/extracted/dependencies/ ./
COPY --from=extract --chown=appuser:appuser /opt/app/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=appuser:appuser /opt/app/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=appuser:appuser /opt/app/extracted/application/ ./
COPY --from=train --chown=appuser:appuser /opt/app/app.aot /opt/app/app.aot

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# AOTMode=auto keeps startup resilient if cache is incompatible with runtime.
ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Duser.timezone=America/Sao_Paulo", \
  "-Dspring.aot.enabled=true", \
  "-XX:AOTMode=auto", \
  "-XX:AOTCache=/opt/app/app.aot", \
  "-jar", \
  "application.jar"]
