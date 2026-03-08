# 1) Build do JAR
FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /workspace
ENV TZ=America/Sao_Paulo
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn -DskipTests -DskipITs package -B

# 2) Extrai o JAR em camadas
FROM eclipse-temurin:25-jre AS extract
WORKDIR /opt/app
ENV TZ=America/Sao_Paulo
COPY --from=build /workspace/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# 3) Training — Gera o AOT cache executando collection de testes
FROM eclipse-temurin:25-jre AS train
WORKDIR /opt/app
ENV TZ=America/Sao_Paulo

# Instala curl, procps e tzdata para executar a collection
RUN apt-get update && apt-get install -y curl procps tzdata && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/*.jar app.jar
COPY docker/training-collection.sh .
COPY docker/run-training.sh .
RUN chmod +x training-collection.sh run-training.sh

# Executa o training completo
RUN ./run-training.sh

# 4) Runtime (usa o AOT cache com layers cacheáveis)
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /opt/app

# Dependência para HEALTHCHECK HTTP
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Segurança: usuário não-root
RUN groupadd --system appuser && \
  useradd --system --gid appuser --home-dir /opt/app appuser

ENV TZ=America/Sao_Paulo

COPY --from=extract --chown=appuser:appuser /opt/app/extracted/dependencies/ ./
COPY --from=extract --chown=appuser:appuser /opt/app/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=appuser:appuser /opt/app/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=appuser:appuser /opt/app/extracted/application/ ./
COPY --from=train   --chown=appuser:appuser /opt/app/app.aot app.aot

USER appuser

EXPOSE 8080

# Health check aponta para o endpoint do Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:AOTCache=/opt/app/app.aot", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Duser.timezone=America/Sao_Paulo", \
  "-cp", \
  "/opt/app/*:/opt/app/lib/*", \
  "io.github.wesleyosantos91.Application"]
