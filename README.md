# 🏗️ Workshop — Person CRUD Microservices

> **POC comparativa**: o mesmo CRUD de **Person** implementado em **4 stacks diferentes**, seguindo as melhores práticas de mercado para microsserviços em produção.

---

## 🎯 Objetivo

Demonstrar como construir um microsserviço de CRUD completo aplicando:

- **Clean Architecture** — separação em camadas (handler/controller → service → repository → model)
- **Container Best Practices** — multi-stage Dockerfile, non-root user, health checks, layers cacheáveis
- **JEP 483 AOT Cache (JDK 25)** — pré-link de classes para startup ultra-rápido nos projetos Java
- **Observabilidade** — OpenTelemetry integrado (traces via OTLP)
- **Banco de dados** — PostgreSQL em produção, Testcontainers em testes
- **Testes automatizados** — unitários + integração com containers efêmeros
- **Imagens mínimas** — JRE slim para Java, `scratch` para Go

---

## 📦 Projetos

| Projeto | Stack | Versão | JDK / Go | Dockerfile Stages | README |
|---|---|---|---|---|---|
| [`api-person-spring`](api-person-spring/) | Spring Boot | 4.0.3 | JDK 25 | 5 (build → extract → record → train → runtime) | [README](api-person-spring/README.md) |
| [`api-person-micronaut`](api-person-micronaut/) | Micronaut | 4.10.9 | JDK 25 | 4 (build → record → train → runtime) | [README](api-person-micronaut/README.md) |
| [`api-person-quarkus`](api-person-quarkus/) | Quarkus | 3.32.1 | JDK 25 | 2 (build → runtime) | [README](api-person-quarkus/README.md) |
| [`api-person-go-gin`](api-person-go-gin/) | Go + Gin + GORM | 1.26 | Go 1.26 | 2 (build → scratch) | [README](api-person-go-gin/README.md) |

---

## 🏛️ Arquitetura Comum

Todos os projetos seguem a mesma estrutura lógica — o CRUD de **Person** com as camadas:

```
┌──────────────────────────────────────────────────┐
│                   HTTP Client                    │
└──────────────┬───────────────────────────────────┘
               │  REST API (JSON)
┌──────────────▼───────────────────────────────────┐
│          Controller / Handler / Resource          │
│   Validação de request, serialização de response  │
├──────────────────────────────────────────────────┤
│                    Service *                      │
│         Regras de negócio, mapeamento             │
├──────────────────────────────────────────────────┤
│                   Repository                      │
│          Abstração de acesso a dados              │
├──────────────────────────────────────────────────┤
│                  Model / Entity                   │
│           Representação do domínio                │
├──────────────────────────────────────────────────┤
│               PostgreSQL (GORM/JPA)               │
└──────────────────────────────────────────────────┘

* Service layer é opcional dependendo da complexidade
```

---

## 🔌 API — Endpoints

Todos os projetos expõem a mesma API REST:

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/persons` | Lista todas as persons |
| `GET` | `/api/persons/{id}` | Busca person por ID |
| `POST` | `/api/persons` | Cria uma nova person |
| `PUT` | `/api/persons/{id}` | Atualiza person existente |
| `DELETE` | `/api/persons/{id}` | Remove person por ID |

### Payload de exemplo

```json
{
  "name": "Wesley Santos",
  "email": "wesley@example.com",
  "birth_date": "1991-01-15"
}
```

---

## ⚡ JEP 483 — AOT Cache (JDK 25)

Os três projetos Java utilizam o **JEP 483 (Ahead-of-Time Class Loading & Linking)** para reduzir drasticamente o tempo de startup:

```
┌─────────────────────────────────────────────────────┐
│  Stage: RECORD                                      │
│  java -XX:AOTMode=record → grava app.aotconf        │
│  (perfil de classes carregadas/linkadas no startup)  │
├─────────────────────────────────────────────────────┤
│  Stage: CREATE                                      │
│  java -XX:AOTMode=create → gera app.aot             │
│  (cache binário pré-linkado)                        │
├─────────────────────────────────────────────────────┤
│  Stage: RUNTIME                                     │
│  java -XX:AOTCache=app.aot → startup otimizado      │
│  (pula classloading e verificação de bytecode)       │
└─────────────────────────────────────────────────────┘
```

> **Requisito**: todos os elementos do classpath devem ser **JARs** (não diretórios de classes).

---

## 🐳 Docker — Build & Run

### Build de qualquer projeto

```bash
# Spring Boot
docker build -t api-person-spring ./api-person-spring

# Micronaut
docker build -t api-person-micronaut ./api-person-micronaut

# Quarkus
docker build -t api-person-quarkus ./api-person-quarkus

# Go + Gin
docker build -t api-person-go-gin ./api-person-go-gin
```

### Run

```bash
docker run -d --name api-person \
  -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  -e DB_NAME=person_db \
  api-person-spring  # ou qualquer outra imagem
```

---

## 🔭 Observabilidade

Todos os projetos possuem **OpenTelemetry** integrado para tracing distribuído:

| Stack | Dependência |
|---|---|
| Spring Boot | `spring-boot-starter-opentelemetry` |
| Micronaut | `opentelemetry-exporter-otlp` |
| Quarkus | `quarkus-opentelemetry` |
| Go + Gin | *(a ser integrado)* |

Variáveis de ambiente para configuração do exportador OTLP:

```bash
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
OTEL_SERVICE_NAME=api-person
```

---

## 🧪 Testes

| Stack | Framework de Teste | DB em Teste |
|---|---|---|
| Spring Boot | JUnit 5 + Testcontainers | PostgreSQL container |
| Micronaut | JUnit 5 + Micronaut Test Resources | PostgreSQL container |
| Quarkus | JUnit 5 + Quarkus Test | PostgreSQL (DevServices) |
| Go + Gin | Testing (stdlib) | *(a ser integrado)* |

```bash
# Java (Maven)
mvn test          # unitários
mvn verify        # unitários + integração

# Go
go test ./...
```

---

## 📋 Pré-requisitos

| Ferramenta | Versão mínima |
|---|---|
| **JDK** | 25 (LTS) |
| **Maven** | 3.9+ |
| **Go** | 1.26+ |
| **Docker** | 24+ |
| **PostgreSQL** | 15+ |

---

## 📂 Estrutura do Workspace

```
workshop/
├── README.md                          ← este arquivo
├── api-person-spring/                 ← Spring Boot 4.0.3
│   ├── Dockerfile                     (5 stages — JEP 483 + layered JAR)
│   ├── pom.xml
│   └── src/
├── api-person-micronaut/              ← Micronaut 4.10.9
│   ├── Dockerfile                     (4 stages — Micronaut AOT + JEP 483)
│   ├── pom.xml
│   ├── aot-jar.properties
│   └── src/
├── api-person-quarkus/                ← Quarkus 3.32.1
│   ├── Dockerfile                     (2 stages — Quarkus AOT + JEP 483)
│   ├── pom.xml
│   └── src/
└── api-person-go-gin/                 ← Go 1.26 + Gin + GORM
    ├── Dockerfile                     (2 stages — static binary + scratch)
    ├── go.mod
    ├── cmd/api/main.go
    └── internal/
```

---

## 📊 Comparativo de Características

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
| **Observability** | OpenTelemetry | OpenTelemetry | OpenTelemetry | *(pendente)* |

---

## 📄 Licença

Esta POC é para fins educacionais e de workshop.

---

> **Autor**: [Wesley Santos](https://github.com/wesleyosantos91)
