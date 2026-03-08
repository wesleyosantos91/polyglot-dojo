# 🥋 polyglot-dojo

> **29 desafios práticos** · **4 stacks** · **116 implementações** · **40+ tecnologias**
>
> O mesmo domínio (**Person API**) implementado em **Spring Boot 4**, **Micronaut 4**, **Quarkus 3** e **Go + Gin** — do CRUD ao Event Sourcing, do JWT ao Keycloak SSO, do REST ao gRPC, do ORM ao SQL puro, do teste unitário ao Contract Testing com Pact.

---

## 💡 O que é

Um **dojo de engenharia backend** onde você pratica os mesmos 29 desafios em 4 stacks diferentes, construindo fluência real em cada uma. Não é tutorial superficial — cada desafio tem especificação completa, critérios de aceite, Docker Compose, dicas de implementação e referências.

**Ao completar, você terá:**
- 116 projetos funcionais no portfólio
- Domínio de 40+ tecnologias de mercado
- Capacidade de transitar entre stacks com fluência
- Conhecimento production-grade (observabilidade, segurança, resiliência, testes)

---

## 🥊 Por que "Dojo"?

> **Dojo** (道場) — lugar de prática. No karatê, você repete os mesmos katas até dominar. Aqui, você repete o mesmo domínio em 4 stacks até que **arquitetura, patterns e trade-offs** fiquem naturais — independente da linguagem.

---

## 📦 Stacks

| Projeto | Stack | Versão | Runtime | Dockerfile | README |
|---|---|---|---|---|---|
| [`01-desafio/api-person-spring`](01-desafio/api-person-spring/) | Spring Boot | 4.0.3 | JDK 25 (JEP 483 AOT) | 4 stages | [README](01-desafio/api-person-spring/README.md) |
| [`01-desafio/api-person-micronaut`](01-desafio/api-person-micronaut/) | Micronaut | 4.10.9 | JDK 25 (JEP 483 AOT) | 4 stages | [README](01-desafio/api-person-micronaut/README.md) |
| [`01-desafio/api-person-quarkus`](01-desafio/api-person-quarkus/) | Quarkus | 3.32.1 | JDK 25 (JEP 483 AOT) | 2 stages | [README](01-desafio/api-person-quarkus/README.md) |
| [`01-desafio/api-person-go-gin`](01-desafio/api-person-go-gin/) | Go + Gin + GORM | 1.26 | Go 1.26 | 2 stages (scratch) | [README](01-desafio/api-person-go-gin/README.md) |

---

## 🗺️ Challenges (29 desafios)

Todos os desafios estão documentados em [`challenges/`](challenges/README.md) com especificação completa.

### Fase 1 — Fundamentos
| # | Desafio | Nível | Horas/stack |
|---|---------|-------|:-----------:|
| 01 | [CRUD REST API](challenges/01-crud-rest-api.md) | ⭐ | 6–8h |

### Fase 2 — Mensageria & Eventos
| # | Desafio | Nível | Horas/stack |
|---|---------|-------|:-----------:|
| 03 | [Kafka Producer & Consumer](challenges/03-kafka-producer-consumer.md) | ⭐⭐ | 8–10h |
| 04 | [AWS SQS](challenges/04-aws-sqs.md) | ⭐⭐ | 6–8h |
| 05 | [RabbitMQ](challenges/05-rabbitmq.md) | ⭐⭐ | 6–8h |

### Fase 3 — Background & Integração
| # | Desafio | Nível | Horas/stack |
|---|---------|-------|:-----------:|
| 06 | [Batch Processing](challenges/06-batch-processing.md) | ⭐⭐ | 6–8h |
| 07 | [Scheduled Jobs](challenges/07-scheduled-jobs.md) | ⭐⭐ | 4–6h |
| 08 | [HTTP Client Integration](challenges/08-http-client-integration.md) | ⭐⭐ | 6–8h |

### Fase 4 — Protocolos Alternativos
| # | Desafio | Nível | Horas/stack |
|---|---------|-------|:-----------:|
| 09 | [gRPC](challenges/09-grpc.md) | ⭐⭐⭐ | 8–10h |
| 10 | [GraphQL](challenges/10-graphql.md) | ⭐⭐⭐ | 6–8h |
| 11 | [WebSocket](challenges/11-websocket.md) | ⭐⭐⭐ | 6–8h |

### Fase 5 — Cross-Cutting Concerns
| # | Desafio | Nível | Horas/stack |
|---|---------|-------|:-----------:|
| 02 | [AWS Lambda](challenges/02-aws-lambda.md) | ⭐⭐ | 6–8h |
| 12 | [File Upload S3](challenges/12-file-upload-s3.md) | ⭐⭐ | 6–8h |
| 13 | [Auth JWT / OAuth2 / SSO (Keycloak)](challenges/13-auth-jwt-oauth2.md) | ⭐⭐⭐⭐ | 12–16h |
| 15 | [Cache Redis](challenges/15-cache-redis.md) | ⭐⭐ | 6–8h |

### Fase 6 — Produção & Patterns
| # | Desafio | Nível | Horas/stack |
|---|---------|-------|:-----------:|
| 14 | [Event Sourcing & CQRS](challenges/14-event-sourcing-cqrs.md) | ⭐⭐⭐ | 10–12h |
| 16 | [Elasticsearch](challenges/16-elasticsearch.md) | ⭐⭐⭐ | 8–10h |
| 17 | [Notification Service](challenges/17-notification-service.md) | ⭐⭐ | 6–8h |
| 18 | [API Gateway & BFF](challenges/18-api-gateway-bff.md) | ⭐⭐⭐ | 8–10h |

### Fase 7 — Observabilidade & Resiliência
| # | Desafio | Nível | Horas/stack |
|---|---------|-------|:-----------:|
| 19 | [Full Observability Stack](challenges/19-observability.md) | ⭐⭐⭐⭐ | 12–16h |
| 26 | [Resilience & Fault Tolerance](challenges/26-resilience-fault-tolerance.md) | ⭐⭐⭐ | 8–10h |

### Fase 8 — AWS & NoSQL
| # | Desafio | Nível | Horas/stack |
|---|---------|-------|:-----------:|
| 20 | [DynamoDB & NoSQL](challenges/20-dynamodb-nosql.md) | ⭐⭐⭐ | 8–10h |
| 21 | [AWS Cloud Native](challenges/21-aws-cloud-native.md) | ⭐⭐⭐ | 10–12h |

### Fase 9 — Testing Avançado
| # | Desafio | Nível | Horas/stack |
|---|---------|-------|:-----------:|
| 22 | [Integration Testing (Testcontainers)](challenges/22-integration-testing.md) | ⭐⭐⭐ | 8–10h |
| 23 | [BDD Cucumber](challenges/23-bdd-cucumber.md) | ⭐⭐ | 6–8h |
| 29 | [Contract Testing (Pact)](challenges/29-contract-testing.md) | ⭐⭐⭐ | 8–10h |

### Fase 10 — Performance, Deep-Dive & Arquitetura
| # | Desafio | Nível | Horas/stack |
|---|---------|-------|:-----------:|
| 24 | [Virtual Threads & Goroutines](challenges/24-virtual-threads-goroutines.md) | ⭐⭐⭐ | 8–10h |
| 25 | [SQL Puro vs ORM](challenges/25-sql-vs-orm.md) | ⭐⭐⭐ | 8–10h |
| 27 | [Hexagonal & Clean Architecture](challenges/27-hexagonal-clean-architecture.md) | ⭐⭐⭐ | 8–10h |
| 28 | [Reactive & SSE Streaming](challenges/28-reactive-sse-streaming.md) | ⭐⭐⭐ | 8–10h |

> 📋 **Índice completo com checklist de progresso:** [`challenges/README.md`](challenges/README.md)

---

## 🏛️ Arquitetura Base

Todos os projetos seguem a mesma estrutura lógica — o CRUD de **Person** como domínio central:

```
┌──────────────────────────────────────────────────┐
│                   HTTP Client                    │
└──────────────┬───────────────────────────────────┘
               │  REST API (JSON)
┌──────────────▼───────────────────────────────────┐
│          Controller / Handler / Resource          │
│   Validação de request, serialização de response  │
├──────────────────────────────────────────────────┤
│                    Service                        │
│         Regras de negócio, mapeamento             │
├──────────────────────────────────────────────────┤
│                   Repository                      │
│          Abstração de acesso a dados              │
├──────────────────────────────────────────────────┤
│                  Model / Entity                   │
│           Representação do domínio                │
├──────────────────────────────────────────────────┤
│               PostgreSQL (JPA / GORM)             │
└──────────────────────────────────────────────────┘
```

### API — Endpoints Base

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/persons` | Lista todas as persons |
| `GET` | `/api/persons/{id}` | Busca person por ID |
| `POST` | `/api/persons` | Cria uma nova person |
| `PUT` | `/api/persons/{id}` | Atualiza person existente |
| `DELETE` | `/api/persons/{id}` | Remove person por ID |

```json
{
  "name": "Wesley Santos",
  "email": "wesley@example.com",
  "birth_date": "1991-01-15"
}
```

---

## ⚡ JEP 483 — AOT Cache (JDK 25)

Os três projetos Java utilizam o **JEP 483 (Ahead-of-Time Class Loading & Linking)** para startup ultra-rápido:

```
Record  → java -XX:AOTMode=record  → grava app.aotconf (perfil de classes)
Create  → java -XX:AOTMode=create  → gera app.aot (cache binário pré-linkado)
Runtime → java -XX:AOTCache=app.aot → startup otimizado (pula classloading)
```

> **Requisito**: todos os elementos do classpath devem ser **JARs** (não diretórios).

---

## 🧰 Tecnologias

| Categoria | Tecnologias |
|---|---|
| **Linguagens** | Java 25, Go 1.26 |
| **Frameworks** | Spring Boot 4, Micronaut 4, Quarkus 3, Gin |
| **Mensageria** | Kafka (KRaft), AWS SQS, RabbitMQ |
| **Banco de Dados** | PostgreSQL, DynamoDB, Elasticsearch |
| **Cache** | Redis |
| **Cloud AWS** | Lambda, S3, SQS, SNS, EventBridge, Step Functions, DynamoDB, Secrets Manager |
| **Auth & SSO** | Keycloak, OAuth2, OIDC, JWT, PKCE, MFA |
| **Observabilidade** | OpenTelemetry, Prometheus, Grafana, Tempo, Loki, Jaeger, Pyroscope |
| **Protocolos** | REST, gRPC, GraphQL, WebSocket, SSE |
| **Testing** | JUnit 5, Testcontainers, Cucumber, Pact, WireMock, ArchUnit, Godog |
| **Resiliência** | Resilience4j, MicroProfile Fault Tolerance, gobreaker |
| **Arquitetura** | Hexagonal, Clean Architecture, Event Sourcing, CQRS, BFF |
| **Concorrência** | Virtual Threads (JEP 444), Structured Concurrency (JEP 480), Goroutines, Channels |
| **Containers** | Docker, Docker Compose, Testcontainers |
| **Infra Local** | LocalStack, Keycloak, MinIO |

---

## 📊 Comparativo de Stacks

| Característica | Spring Boot | Micronaut | Quarkus | Go + Gin |
|---|---|---|---|---|
| **Linguagem** | Java 25 | Java 25 | Java 25 | Go 1.26 |
| **ORM** | Spring Data JPA | Hibernate JPA | Hibernate ORM Panache | GORM |
| **HTTP Server** | Tomcat/Netty | Netty | Vert.x | Gin |
| **Serialização** | Jackson 3.x | Serde Jackson | REST Jackson | encoding/json |
| **AOT (build-time)** | Spring AOT | Micronaut AOT | Quarkus Build-Time | N/A (compilado) |
| **AOT (JVM)** | JEP 483 | JEP 483 | JEP 483 | N/A |
| **GC** | ZGC | ZGC | ZGC | Go GC |
| **Imagem base** | eclipse-temurin:25-jre | eclipse-temurin:25-jre | eclipse-temurin:25-jre | scratch |
| **Non-root** | ✅ | ✅ | ✅ | ✅ (UID 65534) |

---

## 🐳 Quick Start

```bash
# Clone
git clone https://github.com/wesleyosantos91/polyglot-dojo.git
cd polyglot-dojo

# Build qualquer stack
docker build -t api-person-spring ./01-desafio/api-person-spring
docker build -t api-person-micronaut ./01-desafio/api-person-micronaut
docker build -t api-person-quarkus ./01-desafio/api-person-quarkus
docker build -t api-person-go-gin ./01-desafio/api-person-go-gin

# Subir stack local (Postgres + Observabilidade + API Spring)
cd 01-desafio/infra/docker
docker compose up -d --build api-person-spring

# Test
curl http://localhost:8080/api/persons
```

---

## 📋 Pré-requisitos

| Ferramenta | Versão |
|---|---|
| **JDK** | 25 (LTS) |
| **Maven** | 3.9+ |
| **Go** | 1.26+ |
| **Docker** | 24+ |
| **PostgreSQL** | 17+ |

---

## 📂 Estrutura

```
polyglot-dojo/
├── README.md                          ← este arquivo
├── challenges/                        ← 29 desafios com specs completas
│   ├── README.md                      ← índice + checklist de progresso
│   ├── 01-crud-rest-api.md
│   ├── ...
│   └── 29-contract-testing.md
└── 01-desafio/
    ├── README.md                      ← visão consolidada do desafio 01
    ├── api-person-spring/             ← Spring Boot 4.0.3
    ├── api-person-micronaut/          ← Micronaut 4.10.9
    ├── api-person-quarkus/            ← Quarkus 3.32.1
    ├── api-person-go-gin/             ← Go 1.26 + Gin + GORM
    └── infra/
        ├── README.md                  ← documentação de infra local
        ├── docker/                    ← docker-compose e configs de observabilidade
        └── performance/               ← testes de carga (k6)
```

---

## 📄 Licença

Este projeto é para fins educacionais e de prática pessoal.

---

> **Autor**: [Wesley Santos](https://github.com/wesleyosantos91)
