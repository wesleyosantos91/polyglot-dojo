# 🚀 API Person — Micronaut 4.10.9

> CRUD de **Person** com Micronaut 4.10.9 + JDK 25 + Micronaut AOT + JEP 483 AOT Cache + OpenTelemetry

---

## 📋 Visão Geral

Microsserviço REST para gerenciamento de pessoas, construído com as melhores práticas de Micronaut:

| Item | Detalhe |
|---|---|
| **Framework** | Micronaut 4.10.9 |
| **JDK** | 25 (LTS) |
| **HTTP** | Netty (non-blocking) |
| **ORM** | Micronaut Data + Hibernate JPA |
| **Banco** | PostgreSQL 15+ |
| **Connection Pool** | HikariCP |
| **Serialização** | Micronaut Serde Jackson |
| **Observabilidade** | OpenTelemetry Exporter OTLP |
| **Testes** | JUnit 5 + Micronaut Test Resources |
| **Build** | Maven 3.9+ |

---

## 🏛️ Arquitetura

```
src/main/java/io/github/wesleyosantos91/
├── Application.java                   ← Entry point (Micronaut.run)
├── controller/
│   └── PersonController.java          ← REST endpoints (@Controller)
├── model/
│   ├── entity/
│   │   └── Person.java                ← Entidade JPA (@Entity)
│   ├── request/
│   │   └── PersonRequest.java         ← DTO de entrada (@Serdeable)
│   └── response/
│       └── PersonResponse.java        ← DTO de saída (@Serdeable)
├── mapper/
│   └── PersonMapper.java             ← Mapeamento Entity ↔ DTO
├── repository/
│   └── PersonRepository.java         ← @Repository (Micronaut Data)
└── service/
    └── PersonService.java            ← Regras de negócio (@Singleton)
```

---

## 🔌 API Endpoints

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| `GET` | `/api/persons` | Lista todas as persons | `200 OK` |
| `GET` | `/api/persons/{id}` | Busca por ID | `200 OK` / `404 Not Found` |
| `POST` | `/api/persons` | Cria nova person | `201 Created` |
| `PUT` | `/api/persons/{id}` | Atualiza person | `200 OK` / `404 Not Found` |
| `DELETE` | `/api/persons/{id}` | Remove person | `204 No Content` |

### Request Body (POST / PUT)

```json
{
  "name": "Wesley Santos",
  "email": "wesley@example.com",
  "birth_date": "1991-01-15"
}
```

### Response Body

```json
{
  "id": 1,
  "name": "Wesley Santos",
  "email": "wesley@example.com",
  "birth_date": "1991-01-15",
  "created_at": "2026-03-04T10:30:00",
  "updated_at": "2026-03-04T10:30:00"
}
```

---

## ⚡ Duas Camadas de AOT

Este projeto combina **duas camadas complementares** de otimização AOT:

### 1. Micronaut AOT (build-time)

Pré-computa no Maven, antes do deploy:

| Otimização | Descrição |
|---|---|
| `cached.environment` | Cache do environment Micronaut |
| `precompute.environment.properties` | Pré-computa propriedades |
| `logback.xml.to.java` | Converte logback.xml → código Java |
| `property-source-loader.generate` | Gera property sources estáticos |
| `serviceloading.jit` | Pré-resolve ServiceLoader |
| `scan.reactive.types` | Scan antecipado de tipos reativos |
| `deduce.environment` | Dedução de ambiente em build-time |
| `sealed.property.source` | Property sources selados |
| `known.missing.types` | Elimina verificação de tipos ausentes |

Configurado em `aot-jar.properties`.

### 2. JEP 483 AOT Cache (JVM, JDK 25)

Pré-linka classes na JVM — efeito **cumulativo** com o Micronaut AOT:

```
  BUILD          RECORD          TRAIN          RUNTIME
┌─────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│ mvn pkg │   │ AOTMode= │   │ AOTMode= │   │ AOTCache │
│ + MAOT  │──▶│ record   │──▶│ create   │──▶│ = app.aot│
│ thin.jar│   │ .aotconf │   │ app.aot  │   │ ZGC      │
└─────────┘   └──────────┘   └──────────┘   └──────────┘
```

> **Importante**: JEP 483 exige que todos os elementos do classpath sejam **JARs**.
> Por isso usamos thin JAR (`app.jar`) + `lib/*.jar`, não diretório `classes/`.

---

## 🐳 Docker

### Build

```bash
docker build -t api-person-micronaut .
```

### Run

```bash
docker run -d --name api-person-micronaut \
  -p 8080:8080 \
  -e DATASOURCES_DEFAULT_URL=jdbc:postgresql://host.docker.internal:5432/person_db \
  -e DATASOURCES_DEFAULT_USERNAME=postgres \
  -e DATASOURCES_DEFAULT_PASSWORD=postgres \
  -e JPA_DEFAULT_PROPERTIES_HIBERNATE_HBM2DDL_AUTO=update \
  api-person-micronaut
```

### Detalhes do Dockerfile (4 stages)

| Stage | Base | Propósito |
|---|---|---|
| `build` | `maven:3.9.12-eclipse-temurin-25` | Compila com Micronaut AOT + extrai thin JAR |
| `record` | `eclipse-temurin:25-jdk` | Grava perfil de classes (`app.aotconf`) |
| `train` | `eclipse-temurin:25-jdk` | Gera AOT cache binário (`app.aot`) |
| `runtime` | `eclipse-temurin:25-jre` | Imagem final mínima com ZGC |

**Detalhes do build stage:**
1. `mvn package` com `-Dmicronaut.aot.enabled=true` → aplica otimizações do `aot-jar.properties`
2. `mvn dependency:copy-dependencies` → extrai libs para `target/dependency/`
3. `mv original-api-person-*.jar app.jar` → renomeia thin JAR (pré-shade)

**Boas práticas aplicadas:**
- ✅ Usuário non-root (`appuser`)
- ✅ Cache de dependências Maven (`dependency:go-offline`)
- ✅ Thin JAR + libs separadas (Docker layer cache)
- ✅ ZGC (low-latency GC)
- ✅ `MaxRAMPercentage=75%` (container-aware)
- ✅ `ExitOnOutOfMemoryError` (fail-fast)
- ✅ Shutdown graceful no record (`kill` + `wait`)

---

## 🧪 Testes

```bash
# Unitários
mvn test

# Unitários + Integração (requer Docker para Test Resources)
mvn verify

# Pular unitários
mvn verify -DskipUTs

# Pular integração
mvn verify -DskipITs
```

### Micronaut Test Resources

Os testes utilizam **Micronaut Test Resources** que provisionam automaticamente:
- PostgreSQL container via Testcontainers
- Configuração automática de datasource (zero config)

---

## 🚀 Desenvolvimento Local

### Pré-requisitos

- JDK 25+
- Maven 3.9+
- Docker (para Test Resources / Testcontainers)

### Rodando localmente

```bash
# Com Micronaut Test Resources (auto-provisiona PostgreSQL)
mvn mn:run

# Com banco externo
export DATASOURCES_DEFAULT_URL=jdbc:postgresql://localhost:5432/person_db
export DATASOURCES_DEFAULT_USERNAME=postgres
export DATASOURCES_DEFAULT_PASSWORD=postgres
mvn mn:run
```

### Configuração

Configurações são injetadas via variáveis de ambiente (Micronaut property binding):

| Variável | Padrão | Descrição |
|---|---|---|
| `DATASOURCES_DEFAULT_URL` | — | JDBC URL do PostgreSQL |
| `DATASOURCES_DEFAULT_USERNAME` | — | Usuário do banco |
| `DATASOURCES_DEFAULT_PASSWORD` | — | Senha do banco |
| `DATASOURCES_DEFAULT_DRIVER_CLASS_NAME` | `org.postgresql.Driver` | Driver JDBC |
| `JPA_DEFAULT_PROPERTIES_HIBERNATE_HBM2DDL_AUTO` | `none` | Estratégia DDL |
| `OTEL_TRACES_EXPORTER` | `otlp` | Exportador de traces |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | — | Endpoint do coletor OTLP |
| `MICRONAUT_APPLICATION_NAME` | `api-person` | Nome do serviço |

---

## 📦 Dependências Principais

| Dependência | Propósito |
|---|---|
| `micronaut-http-server-netty` | HTTP server non-blocking (Netty) |
| `micronaut-data-tx-hibernate` | Transações + Hibernate |
| `micronaut-hibernate-jpa` | JPA com Hibernate |
| `micronaut-jdbc-hikari` | Connection pool HikariCP |
| `micronaut-serde-jackson` | Serialização compilada (sem reflection) |
| `jakarta.data-api` | Jakarta Data API |
| `opentelemetry-exporter-otlp` | Tracing distribuído (OTLP) |
| `postgresql` | Driver JDBC PostgreSQL |
| `micronaut-test-junit5` | Framework de teste Micronaut |
| `micronaut-test-resources-jdbc-postgresql` | Auto-provisioning de PostgreSQL |

---

## 🔗 Links

- [Micronaut User Guide](https://docs.micronaut.io/4.10.9/guide/index.html)
- [Micronaut API Reference](https://docs.micronaut.io/4.10.9/api/index.html)
- [Micronaut AOT](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)
- [Micronaut Maven Plugin — AOT](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/examples/aot.html)
- [Micronaut Data](https://micronaut-projects.github.io/micronaut-data/latest/guide/)
- [Micronaut Serde Jackson](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)
- [Micronaut Test Resources](https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/)
- [Micronaut Hikari JDBC](https://micronaut-projects.github.io/micronaut-sql/latest/guide/index.html#jdbc)
- [Micronaut Hibernate JPA](https://micronaut-projects.github.io/micronaut-sql/latest/guide/index.html#hibernate)
- [Jakarta Data API](https://jakarta.ee/specifications/data/1.0/jakarta-data-1.0)
- [JEP 483 — AOT Class Loading & Linking](https://openjdk.org/jeps/483)
- [OpenTelemetry](https://opentelemetry.io)

---

> Parte do [Workshop — Person CRUD Microservices](../README.md)
