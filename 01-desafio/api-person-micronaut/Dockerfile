# =============================================================================
# Multi-stage Dockerfile — Micronaut + JVM AOT Cache (JEP 483)
# Thin JAR (sem fat-jar) | JDK 25 | Container best practices
#
# Duas camadas de AOT complementares:
#   1) Micronaut AOT (build-time) — pré-computa configs, converte YAML→Java,
#      elimina classpath scanning, pré-resolve service loaders, etc.
#      Ref: https://micronaut-projects.github.io/micronaut-maven-plugin/latest/examples/aot.html
#   2) JEP 483 AOT Cache (JVM) — pré-linka classes na JVM, eliminando
#      classloading e verificação de bytecode no startup.
#
# IMPORTANTE: JEP 483 exige que TODOS os elementos do classpath sejam JARs.
#   Diretórios de classes (ex: classes/) NÃO são compatíveis com AOT Cache.
#   Por isso usamos thin JAR (app.jar) + lib/*.jar.
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

# Copia o código-fonte e compila com Micronaut AOT habilitado.
# As otimizações do aot-jar.properties serão aplicadas no artefato final:
#   - cached.environment, precompute.environment.properties
#   - logback.xml → Java, YAML → Java config
#   - service loading antecipado, reactive types, deduce environment
#   - sealed property sources, known missing types
COPY src src
COPY aot-jar.properties micronaut-cli.yml ./
RUN mvn package -DskipTests -Dmicronaut.aot.enabled=true -B

# Copia dependências de runtime para diretório separado
RUN mvn dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory=target/dependency \
  -B

# Renomeia o thin JAR (original, antes do shade) para app.jar.
# O shade plugin gera:
#   - api-person-0.1.jar           → fat-jar (descartado)
#   - original-api-person-0.1.jar  → thin-jar (classes da app + Micronaut AOT)
RUN mv target/original-api-person-*.jar target/app.jar

# ============================================================
# Stage 2: TRAINING — Record (grava perfil de classes carregadas)
# ============================================================
FROM eclipse-temurin:25-jdk AS record

WORKDIR /opt/app

ENV TZ=America/Sao_Paulo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo"

# Thin JAR + dependências (todos JARs — compatível com JEP 483)
COPY --from=build /workspace/target/app.jar       ./app.jar
COPY --from=build /workspace/target/dependency     ./lib

# Training run: a JVM registra as classes carregadas/linkadas.
# O artefato já contém as otimizações Micronaut AOT, então o JEP 483
# vai cachear as classes já otimizadas — efeito cumulativo no startup.
# Inicia o Java em background, aguarda a inicialização completa,
# envia SIGTERM diretamente ao processo e espera a JVM gravar o
# app.aotconf durante o shutdown graceful.
RUN java -XX:AOTMode=record \
  -XX:AOTConfiguration=app.aotconf \
  -cp "app.jar:lib/*" \
  io.github.wesleyosantos91.Application & \
  JAVA_PID=$! && \
  sleep 15 && \
  kill $JAVA_PID && \
  wait $JAVA_PID ; \
  exit 0

# ============================================================
# Stage 3: TRAINING — Create (gera o AOT cache a partir do perfil)
# ============================================================
FROM eclipse-temurin:25-jdk AS train

WORKDIR /opt/app

ENV TZ=America/Sao_Paulo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo"

COPY --from=build  /workspace/target/app.jar       ./app.jar
COPY --from=build  /workspace/target/dependency     ./lib
COPY --from=record /opt/app/app.aotconf             ./app.aotconf

RUN java -XX:AOTMode=create \
  -XX:AOTConfiguration=app.aotconf \
  -XX:AOTCache=app.aot \
  -cp "app.jar:lib/*"

# ============================================================
# Stage 4: RUNTIME — Imagem final otimizada com AOT cache
# ============================================================
FROM eclipse-temurin:25-jre AS runtime

ARG APP_DIR=/opt/app
WORKDIR ${APP_DIR}

# Segurança: usuário não-root
RUN groupadd --system appuser && \
  useradd  --system --gid appuser --home-dir ${APP_DIR} appuser

ENV TZ=America/Sao_Paulo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo"

# Thin JAR + libs — da menos mutável para a mais mutável (layers cacheáveis)
COPY --from=build  --chown=appuser:appuser /workspace/target/dependency  ./lib
COPY --from=build  --chown=appuser:appuser /workspace/target/app.jar     ./app.jar
COPY --from=train  --chown=appuser:appuser /opt/app/app.aot              ./app.aot

USER appuser

EXPOSE 8080

ENTRYPOINT ["java",                        \
  "-XX:AOTCache=/opt/app/app.aot",         \
  "-XX:+UseZGC",                           \
  "-XX:MaxRAMPercentage=75.0",             \
  "-XX:InitialRAMPercentage=75.0",         \
  "-XX:+ExitOnOutOfMemoryError",           \
  "-cp", "app.jar:lib/*",                  \
  "io.github.wesleyosantos91.Application"]
