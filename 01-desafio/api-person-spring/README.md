# 🌱 API Person — Spring Boot 4.0.3

> CRUD de **Person** com Spring Boot 4.0.3 + JDK 25 + JEP 483 AOT Cache + OpenTelemetry

---

## 📋 Visão Geral

Microsserviço REST para gerenciamento de pessoas, construído com as melhores práticas de Spring Boot:

| Item | Detalhe |
|---|---|
| **Framework** | Spring Boot 4.0.3 |
| **JDK** | 25 (LTS) |
| **HTTP** | Spring MVC (Tomcat) |
| **ORM** | Spring Data JPA + Hibernate |
| **Banco** | PostgreSQL 15+ |
| **Serialização** | Jackson 3.x |
| **Observabilidade** | Spring Boot OpenTelemetry Starter |
| **Testes** | JUnit 5 + Testcontainers (PostgreSQL) |
| **Build** | Maven 3.9+ |

---

## 🏛️ Arquitetura

```
src/main/java/io/github/wesleyosantos91/
├── ApiPersonApplication.java          ← Entry point (@SpringBootApplication)
├── api/
│   ├── controller/
│   │   └── PersonController.java      ← REST endpoints (@RestController)
│   └── exception/
│       └── GlobalExceptionHandler.java ← Tratamento centralizado de erros
├── domain/
│   ├── model/
│   │   └── Person.java                ← Entidade JPA (@Entity)
│   ├── request/
│   │   └── PersonRequest.java         ← DTO de entrada
│   └── response/
│       └── PersonResponse.java        ← DTO de saída
├── mapper/
│   └── PersonMapper.java             ← Mapeamento Entity ↔ DTO
├── repository/
│   └── PersonRepository.java         ← Spring Data JPA Repository
└── service/
    └── PersonService.java            ← Regras de negócio
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

## ⚡ JEP 483 — AOT Cache (JDK 25)

O Dockerfile utiliza **5 stages** para máxima otimização de startup:

```
  BUILD          EXTRACT         RECORD          TRAIN          RUNTIME
┌─────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│ mvn     │   │ extract  │   │ AOTMode= │   │ AOTMode= │   │ AOTCache │
│ package │──▶│ layers   │──▶│ record   │──▶│ create   │──▶│ = app.aot│
│         │   │          │   │ .aotconf │   │ app.aot  │   │ ZGC      │
└─────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘
```

A flag `-Dspring.context.exit=onRefresh` permite que o **Record** finalize automaticamente após carregar todas as classes — sem necessidade de `kill`.

---

## 🐳 Docker

### Build

```bash
docker build -t api-person-spring .
```

### Run

```bash
docker run -d --name api-person-spring \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/person_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  api-person-spring
```

### Detalhes do Dockerfile

| Stage | Base | Propósito |
|---|---|---|
| `build` | `maven:3.9.12-eclipse-temurin-25` | Compila o JAR |
| `extract` | `eclipse-temurin:25-jdk` | Extrai layered JAR (CDS/AOT friendly) |
| `record` | `eclipse-temurin:25-jdk` | Grava perfil de classes (`app.aotconf`) |
| `train` | `eclipse-temurin:25-jdk` | Gera AOT cache binário (`app.aot`) |
| `runtime` | `eclipse-temurin:25-jre` | Imagem final mínima com ZGC |

**Boas práticas aplicadas:**
- ✅ Usuário non-root (`appuser`)
- ✅ Layered JAR para cache de Docker layers
- ✅ ZGC (low-latency GC)
- ✅ `MaxRAMPercentage=75%` (container-aware)
- ✅ `ExitOnOutOfMemoryError` (fail-fast)

---

## 🧪 Testes

```bash
# Unitários
mvn test

# Unitários + Integração (requer Docker para Testcontainers)
mvn verify

# Pular unitários
mvn verify -DskipUTs

# Pular integração
mvn verify -DskipITs
```

### Testcontainers

Os testes de integração usam **Testcontainers** para provisionar automaticamente:
- PostgreSQL container
- Grafana container (observabilidade)

---

## 🚀 Desenvolvimento Local

### Pré-requisitos

- JDK 25+
- Maven 3.9+
- PostgreSQL 15+ (ou Docker)

### Rodando localmente

```bash
# Com banco local
mvn spring-boot:run

# Com H2 em memória (profile dev)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Configuração

O `application.yml` define apenas o nome da aplicação. As demais configurações são injetadas via variáveis de ambiente:

| Variável | Padrão | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | — | JDBC URL do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | — | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | — | Senha do banco |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `none` | Estratégia DDL |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | — | Endpoint do coletor OTLP |
| `OTEL_SERVICE_NAME` | `api-person` | Nome do serviço no tracing |

---

## 📦 Dependências Principais

| Dependência | Propósito |
|---|---|
| `spring-boot-starter-webmvc` | REST API com Spring MVC |
| `spring-boot-starter-data-jpa` | JPA + Hibernate + Spring Data |
| `spring-boot-starter-opentelemetry` | Tracing distribuído (OTLP) |
| `postgresql` | Driver JDBC PostgreSQL |
| `h2` | Banco em memória para testes/dev |
| `spring-boot-testcontainers` | Integração JUnit 5 + Testcontainers |
| `testcontainers-postgresql` | Container PostgreSQL efêmero |
| `testcontainers-grafana` | Container Grafana para validação de traces |

---

## 🔗 Links

- [Spring Boot 4.0 Reference](https://docs.spring.io/spring-boot/reference/)
- [JEP 483 — AOT Class Loading & Linking](https://openjdk.org/jeps/483)
- [Testcontainers for Java](https://java.testcontainers.org/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/languages/java/)

---

> Parte do [Workshop — Person CRUD Microservices](../README.md)
