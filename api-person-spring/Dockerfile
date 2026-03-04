# 1) Build do JAR
FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /workspace
ENV TZ=America/Sao_Paulo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo"
COPY . .
RUN mvn -DskipTests -DskipITs package

# 2) Extrai o JAR em camadas (layout AOT cache/CDS friendly)
FROM eclipse-temurin:25-jdk AS extract
WORKDIR /opt/app
ENV TZ=America/Sao_Paulo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo"
COPY --from=build /workspace/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# 3) Training — Record (grava o perfil de treinamento usando fat-jar)
FROM eclipse-temurin:25-jdk AS record
WORKDIR /opt/app
ENV TZ=America/Sao_Paulo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo"
COPY --from=build /workspace/target/*.jar app.jar
RUN java -XX:AOTMode=record -XX:AOTConfiguration=app.aotconf \
  -Dspring.context.exit=onRefresh -jar app.jar

# 4) Training — Create (gera o AOT cache a partir do perfil)
FROM eclipse-temurin:25-jdk AS train
WORKDIR /opt/app
ENV TZ=America/Sao_Paulo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo"
COPY --from=build /workspace/target/*.jar app.jar
COPY --from=record /opt/app/app.aotconf app.aotconf
RUN java -XX:AOTMode=create -XX:AOTConfiguration=app.aotconf \
  -XX:AOTCache=app.aot -jar app.jar

# 5) Runtime (usa o AOT cache com layers cacheáveis)
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /opt/app

# Segurança: usuário não-root
RUN groupadd --system appuser && \
  useradd  --system --gid appuser --home-dir /opt/app appuser

ENV TZ=America/Sao_Paulo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo"

COPY --from=extract --chown=appuser:appuser /opt/app/extracted/dependencies/ ./
COPY --from=extract --chown=appuser:appuser /opt/app/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=appuser:appuser /opt/app/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=appuser:appuser /opt/app/extracted/application/ ./
COPY --from=train   --chown=appuser:appuser /opt/app/app.aot app.aot

USER appuser

EXPOSE 8080

ENTRYPOINT ["java",                              \
  "-XX:AOTCache=/opt/app/app.aot",              \
  "-XX:+UseZGC",                                \
  "-XX:MaxRAMPercentage=75.0",                  \
  "-XX:InitialRAMPercentage=75.0",              \
  "-XX:+ExitOnOutOfMemoryError",                \
  "org.springframework.boot.loader.launch.JarLauncher"]
