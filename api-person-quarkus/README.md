# ⚡ API Person — Quarkus 3.32.1

> CRUD de **Person** com Quarkus 3.32.1 + JDK 25 + Quarkus AOT + JEP 483 AOT Cache + OpenTelemetry

---

## 📋 Visão Geral

Microsserviço REST para gerenciamento de pessoas, construído com as melhores práticas de Quarkus:

| Item | Detalhe |
|---|---|
| **Framework** | Quarkus 3.32.1 |
| **JDK** | 25 (LTS) |
| **HTTP** | Quarkus REST (Vert.x) |
| **ORM** | Hibernate ORM with Panache |
| **Banco** | PostgreSQL 15+ |
| **Serialização** | REST Jackson |
| **Observabilidade** | Quarkus OpenTelemetry |
| **Testes** | JUnit 5 + Quarkus DevServices |
| **Build** | Maven 3.9+ |

---

## 🏛️ Arquitetura

```
src/main/java/io/github/wesleyosantos91/
├── resource/
│   └── PersonResource.java           ← REST endpoints (@Path)
├── model/
│   ├── entity/
│   │   └── Person.java               ← Entidade JPA (PanacheEntity)
│   ├── request/
│   │   └── PersonRequest.java        ← DTO de entrada
│   └── response/
│       └── PersonResponse.java       ← DTO de saída
└── mapper/
    └── PersonMapper.java             ← Mapeamento Entity ↔ DTO
```

> **Nota**: Quarkus com Panache simplifica a camada de repository — o `PanacheEntity` ou `PanacheRepository` embutido elimina a necessidade de interface separada.

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

## ⚡ Quarkus AOT + JEP 483

O Quarkus gerencia o **JEP 483 nativamente** via configuração Maven — sem stages extras no Dockerfile:

```
  BUILD (mvn package)                         RUNTIME
┌────────────────────────────────────────┐   ┌──────────┐
│ 1. Compilação Quarkus (build-time)     │   │ AOTCache │
│    - Indexação Jandex                  │   │ = app.aot│
│    - Geração de bytecode               │   │ ZGC      │
│ 2. JEP 483 AOT Cache integrado        │──▶│          │
│    - quarkus.package.jar.aot.enabled   │   │ quarkus  │
│    - Gera app.aot automaticamente      │   │ -run.jar │
└────────────────────────────────────────┘   └──────────┘
```

Configurações AOT no build:

```bash
-Dquarkus.package.jar.aot.enabled=true   # Habilita JEP 483
-Dquarkus.package.jar.aot.type=aot       # Tipo de cache
-Dquarkus.package.jar.aot.phase=build    # Gera durante o build (sem IT)
```

> O Quarkus cuida do record/create internamente — **2 stages** no Dockerfile são suficientes.

---

## 🐳 Docker

### Build

```bash
docker build -t api-person-quarkus .
```

### Run

```bash
docker run -d --name api-person-quarkus \
  -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/person_db \
  -e QUARKUS_DATASOURCE_USERNAME=postgres \
  -e QUARKUS_DATASOURCE_PASSWORD=postgres \
  -e QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=update \
  api-person-quarkus
```

### Detalhes do Dockerfile (2 stages)

| Stage | Base | Propósito |
|---|---|---|
| `build` | `maven:3.9.12-eclipse-temurin-25` | Compila + gera AOT cache (`app.aot`) |
| `runtime` | `eclipse-temurin:25-jre` | Imagem final mínima com ZGC + AOT |

**Boas práticas aplicadas:**
- ✅ Usuário non-root (`quarkus`)
- ✅ Cache de dependências Maven (`dependency:go-offline`)
- ✅ Quarkus fast-jar layout (lib/quarkus/app separados)
- ✅ ZGC (low-latency GC)
- ✅ `MaxRAMPercentage=75%` (container-aware)
- ✅ `ExitOnOutOfMemoryError` (fail-fast)
- ✅ AOT cache integrado (sem stages record/train manuais)

---

## 🧪 Testes

```bash
# Unitários
mvn test

# Unitários + Integração
mvn verify

# Pular unitários
mvn verify -DskipUTs

# Pular integração
mvn verify -DskipITs
```

### Quarkus DevServices

Os testes utilizam **Quarkus DevServices** que provisionam automaticamente:
- PostgreSQL container (zero config — Quarkus detecta o driver JDBC)

---

## 🚀 Desenvolvimento Local

### Pré-requisitos

- JDK 25+
- Maven 3.9+
- Docker (para DevServices)

### Rodando em modo dev

```bash
# Dev mode com live reload + DevServices (auto-provisiona PostgreSQL)
mvn quarkus:dev
```

> **Dev UI** disponível em http://localhost:8080/q/dev/ durante dev mode.

### Rodando o JAR

```bash
mvn package
java -jar target/quarkus-app/quarkus-run.jar
```

### Build nativo (GraalVM)

```bash
# Com GraalVM instalado
mvn package -Dnative

# Sem GraalVM (build em container)
mvn package -Dnative -Dquarkus.native.container-build=true

# Executar
./target/api-person-1.0.0-SNAPSHOT-runner
```

### Configuração

| Variável | Padrão | Descrição |
|---|---|---|
| `QUARKUS_DATASOURCE_JDBC_URL` | — | JDBC URL do PostgreSQL |
| `QUARKUS_DATASOURCE_USERNAME` | — | Usuário do banco |
| `QUARKUS_DATASOURCE_PASSWORD` | — | Senha do banco |
| `QUARKUS_DATASOURCE_DB_KIND` | `postgresql` | Tipo de banco |
| `QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION` | `none` | Estratégia DDL |
| `QUARKUS_OPENTELEMETRY_TRACER_EXPORTER_OTLP_ENDPOINT` | — | Endpoint OTLP |
| `QUARKUS_APPLICATION_NAME` | `api-person` | Nome do serviço |

---

## 📦 Dependências Principais

| Dependência | Propósito |
|---|---|
| `quarkus-rest-jackson` | REST API + serialização Jackson |
| `quarkus-hibernate-orm-panache` | ORM simplificado (Active Record / Repository) |
| `quarkus-jdbc-postgresql` | Driver JDBC PostgreSQL |
| `quarkus-opentelemetry` | Tracing distribuído (OTLP) |
| `quarkus-arc` | CDI (injeção de dependência) |
| `quarkus-junit5` | Framework de teste Quarkus |

---

## 🔗 Links

- [Quarkus Website](https://quarkus.io/)
- [Quarkus Guides](https://quarkus.io/guides/)
- [REST Jackson Guide](https://quarkus.io/guides/rest#json-serialisation)
- [Hibernate ORM with Panache Guide](https://quarkus.io/guides/hibernate-orm-panache)
- [OpenTelemetry Guide](https://quarkus.io/guides/opentelemetry)
- [JDBC PostgreSQL Guide](https://quarkus.io/guides/datasource)
- [JEP 483 — AOT Class Loading & Linking](https://openjdk.org/jeps/483)
- [Quarkus Native Build Guide](https://quarkus.io/guides/maven-tooling)

---

> Parte do [Workshop — Person CRUD Microservices](../README.md)
