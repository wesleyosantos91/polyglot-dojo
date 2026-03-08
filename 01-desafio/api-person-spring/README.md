# 🌱 API Person — Spring Boot 4.0.3

> CRUD de **Person** com Spring Boot 4.0.3 + JDK 25 + JEP 483 AOT Cache + OpenTelemetry

---

## 📦 Containerização Oficial

Este projeto usa **apenas um** Dockerfile suportado:

- `Dockerfile`

Esse Dockerfile aplica:

- build com `-Pnative` (Spring AOT)
- extração de camadas (`jarmode=tools`)
- treinamento AOT (`TRAINING_MODE=full`) com `docker/training-collection.sh`
- geração de `app.aot` para startup/warmup melhores

> Os Dockerfiles antigos foram removidos para reduzir manutenção e evitar divergência.

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

O Dockerfile utiliza **4 stages** para máxima otimização de startup com **training real**:

```
  BUILD          EXTRACT              TRAIN                    RUNTIME
┌─────────┐   ┌──────────┐   ┌──────────────────┐   ┌──────────────────┐
│ mvn     │   │ extract  │   │ 1. Start app     │   │ java             │
│ package │──▶│ layers   │──▶│ 2. Run collection│──▶│ -XX:AOTCache=    │
│         │   │          │   │ 3. Shutdown      │   │   app.aot        │
│         │   │          │   │ 4. Generate .aot │   │ + ZGC            │
└─────────┘   └──────────┘   └──────────────────┘   └──────────────────┘
                                     ↓
                              app.aot (cache)
```

### Training com Cenários Reais

Durante o **stage train** do build:

1. **Inicia** a aplicação com `-XX:AOTCacheOutput=app.aot` + profile `aot-training` (H2)
2. **Aguarda** health check (até 60s)
3. **Executa** 15 cenários reais da collection:
   - ✅ CRUD completo (Create, Read, Update, Patch, Delete)
   - ✅ Validações e erros (400, 404, 409/422)
   - ✅ Buscas e filtros (nome, email)
   - ✅ Paginação e ordenação
   - ✅ Atualizações parciais
4. **Finaliza** gracefully (SIGTERM)
5. **Gera** `app.aot` durante o shutdown da JVM
6. **Copia** para a imagem final

**Benefícios:**
- ⚡ Startup 30-50% mais rápido
- 🚀 Warm-up instantâneo dos paths mais usados
- 💾 Menor uso de CPU no início
- 🎯 Performance otimizada para cenários reais

**Scripts de Training:**
- `docker/run-training.sh` — Orquestra o processo completo (start → health check → collection → shutdown)
- `docker/training-collection.sh` — Executa os 15 cenários da API com curl

---

## 🐳 Docker

### Build

```bash
# Build oficial (full training + app.aot)
docker build --no-cache \
  --build-arg TRAINING_MODE=full \
  -t api-person-spring:prod-aot .

# Ou use o script helper
chmod +x build-docker.sh
./build-docker.sh
```

### Run

```bash
docker run -d --name api-person-spring \
  -p 8080:8080 \
  -e APP_DATASOURCE_WRITER_URL=jdbc:postgresql://host.docker.internal:5432/dev \
  -e APP_DATASOURCE_WRITER_USERNAME=postgres \
  -e APP_DATASOURCE_WRITER_PASSWORD=postgres \
  -e APP_DATASOURCE_READER_URL=jdbc:postgresql://host.docker.internal:5432/dev \
  -e APP_DATASOURCE_READER_USERNAME=postgres \
  -e APP_DATASOURCE_READER_PASSWORD=postgres \
  api-person-spring:prod-aot
```

### Detalhes do Dockerfile

Arquivo: `Dockerfile`

| Stage | Base | Propósito |
|---|---|---|
| `build` | `maven:3.9.12-eclipse-temurin-25` | Compila o JAR com cache de dependências |
| `extract` | `eclipse-temurin:25-jre` | Extrai layered JAR (CDS/AOT friendly) |
| `train` | `eclipse-temurin:25-jre` | Inicia app + executa collection + gera AOT cache (`app.aot`) |
| `runtime` | `eclipse-temurin:25-jre` | Imagem final mínima com ZGC + AOT cache |

**Boas práticas aplicadas:**
- ✅ Usuário non-root (`appuser`)
- ✅ Layered JAR para cache de Docker layers
- ✅ Training com cenários reais (15 requests da collection)
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
- PostgreSQL container (`postgres:18.3`)

> **Pré-requisito para reuso de containers entre runs:**
> Adicione ao arquivo `~/.testcontainers.properties`:
> ```properties
> testcontainers.reuse.enable=true
> ```
> Sem essa configuração, `withReuse(true)` é ignorado e um novo container é criado a cada `mvn verify`.

## 🔥 Testes de Carga (k6)

Os cenários de carga ficam em `../infra/performance/k6/persons-workload.js` com 3 perfis:

- `smoke`: validação rápida (1 VU por 30s)
- `load`: carga sustentada para baseline de capacidade
- `stress`: rampa de pressão para identificar limite

### Pré-requisitos

- API e banco em execução (recomendado: `../infra/docker/docker-compose.yml`)
- Endpoint `BASE_URL` acessível

### Rodando com k6 local

```bash
k6 run ../infra/performance/k6/persons-workload.js -e BASE_URL=http://localhost:8080 -e TEST_TYPE=smoke
k6 run ../infra/performance/k6/persons-workload.js -e BASE_URL=http://localhost:8080 -e TEST_TYPE=load
k6 run ../infra/performance/k6/persons-workload.js -e BASE_URL=http://localhost:8080 -e TEST_TYPE=stress
```

### Rodando com Docker (sem instalar k6)

```bash
docker run --rm -i \
  -v "${PWD}/..:/work" \
  -w /work \
  grafana/k6 run infra/performance/k6/persons-workload.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e TEST_TYPE=load
```

### Exportando resultados

```bash
k6 run ../infra/performance/k6/persons-workload.js \
  -e BASE_URL=http://localhost:8080 \
  -e TEST_TYPE=load \
  --summary-export=../infra/performance/k6/results/load-summary.json
```

### Mix de tráfego usado

- 60% `GET /api/persons` (lista paginada)
- 30% `GET /api/persons/{id}`
- 10% fluxo de escrita (`POST` + `PATCH` + `DELETE`)

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
| `APP_DATASOURCE_WRITER_URL` | `jdbc:postgresql://localhost:5432/dev?...` | JDBC URL de escrita |
| `APP_DATASOURCE_WRITER_USERNAME` | `postgres` | Usuário de escrita |
| `APP_DATASOURCE_WRITER_PASSWORD` | `postgres` | Senha de escrita |
| `APP_DATASOURCE_READER_URL` | `jdbc:postgresql://localhost:5432/dev?...` | JDBC URL de leitura |
| `APP_DATASOURCE_READER_USERNAME` | `postgres` | Usuário de leitura |
| `APP_DATASOURCE_READER_PASSWORD` | `postgres` | Senha de leitura |
| `MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | Export de traces |
| `MANAGEMENT_OPENTELEMETRY_LOGGING_EXPORT_OTLP_ENDPOINT` | `http://localhost:4318/v1/logs` | Export de logs |
| `MANAGEMENT_OTLP_METRICS_EXPORT_URL` | `http://localhost:4318/v1/metrics` | Export de métricas |

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
