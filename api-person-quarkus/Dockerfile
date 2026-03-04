# =============================================================================
# Multi-stage Dockerfile — Quarkus + JVM AOT Cache (JEP 483)
# AOT-jar layout | JDK 25 | Container best practices
#
# O Quarkus gerencia o JEP 483 nativamente via quarkus.package.jar.aot.enabled:
#   - Gera automaticamente o aot-jar packaging otimizado
#   - Treina o AOT cache durante os integration tests
#   - Produz o app.aot em target/quarkus-app/
# Ref: https://quarkus.io/guides/aot
# =============================================================================

# ============================================================
# Stage 1: BUILD — Compilação com cache de dependências
# ============================================================
FROM maven:3.9.12-eclipse-temurin-25 AS build

WORKDIR /workspace

ENV TZ=America/Sao_Paulo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo"

# Cache de dependências Maven — copia apenas os descritores primeiro
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copia o código-fonte e compila (sem testes nesta etapa)
COPY src src
# Gera o AOT cache durante o build (phase=build, sem integration tests)
RUN mvn package \
  -DskipTests \
  -Dquarkus.package.jar.aot.enabled=true \
  -Dquarkus.package.jar.aot.type=aot \
  -Dquarkus.package.jar.aot.phase=build \
  -B

# ============================================================
# Stage 2: RUNTIME — Imagem final otimizada com AOT cache
# ============================================================
FROM eclipse-temurin:25-jre AS runtime

ARG APP_DIR=/deployments
WORKDIR ${APP_DIR}

# Segurança: usuário não-root
RUN groupadd -r quarkus && useradd -r -g quarkus -d ${APP_DIR} quarkus

ENV TZ=America/Sao_Paulo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo"

# Layers Docker cacheáveis — da menos mutável para a mais mutável
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/lib/            ./lib/
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/quarkus/        ./quarkus/
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/app/            ./app/
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/quarkus-run.jar ./
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/app.aot         ./app.aot

USER quarkus

EXPOSE 8080

ENTRYPOINT ["java",                        \
  "-XX:AOTCache=app.aot",                  \
  "-XX:+UseZGC",                           \
  "-XX:MaxRAMPercentage=75.0",             \
  "-XX:InitialRAMPercentage=75.0",         \
  "-XX:+ExitOnOutOfMemoryError",           \
  "-jar", "quarkus-run.jar"]
