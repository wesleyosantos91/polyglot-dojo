# Workshop Memory

## Projeto: api-person-spring

**Stack:** Spring Boot 4.0.3 + Java 25 LTS + Maven

### Estrutura de pacotes
- `domain/entity`, `domain/service`, `domain/repository`, `domain/exception`
- `api/controller`, `api/request`, `api/response`, `api/exception`
- `core/mapper` (MapStruct)
- `infrastructure/async`, `infrastructure/datastore`, `infrastructure/logging`, `infrastructure/resilience`, `infrastructure/web`

### Padrões confirmados
- **Datasource routing writer/reader**: `AbstractRoutingDataSource` + `@Transactional(readOnly=true)` → READER, escrita → WRITER
- **`@ConditionalOnProperty(prefix = "app.datasource.writer", name = "enabled")`** desabilita custom datasource nos testes (usa H2 via `application-test.yml`)
- **IT tests**: `AbstractIT` com `@Container` estático PostgreSQL 18.3 + `@DynamicPropertySource` (não usa `@ServiceConnection`)
- **Profile IT**: `@ActiveProfiles("it")` em `AbstractIT`, arquivo `application-it.yml`
- **Profile test (unit)**: `application-test.yml` com H2 + `app.datasource.*.enabled: false`
- **MockMvcTester** (AssertJ) — Spring Framework 7 API — nos ITs
- **Failsafe**: `**/*IT.java` | **Surefire**: `**/*Test.java`

### Correções já aplicadas
- `application-it.yml`: dois blocos `management:` mesclados em um (era bug crítico YAML)
- `ITContainersConfig.java`: removido (dead code)
- `TestcontainersConfiguration.java`: imagem alinhada para `postgres:18.3`
- `PersonEntity.toString()`: email mascarado (`***`) — PII
- `PersonEntity.prePersist()`: duplo `;;` corrigido
- `PersonService`: removidos `setCreatedAt`/`setUpdatedAt` redundantes (deixado para `@PrePersist`/`@PreUpdate`)
- `PersonMapper.updateEntity`: método dead removido
- `AsyncConfig.cpuTaskExecutor`: trocado para `ThreadPoolTaskExecutor` com lifecycle Spring
- `WebConfig`: criado com `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`
- `GlobalExceptionHandler`: logs 404/business sem `setCause` (sem stack trace para erros esperados)
- `AdaptiveHttpLoggingFilter`: refatorado com constantes + `http_method`/`http_path` em todos os eventos; `Throwable` → `Exception`
- `CorrelationMdcFilter`: removidas chaves duplicadas `trace_id`/`span_id` (mantidos `traceId`/`spanId`)
- `application.yml`: adicionado histogramas SLO (50ms,100ms,200ms,500ms,1s,2s) + percentis p50/p95/p99
- `otel-collector-config.yaml`: adicionado `otlphttp/loki` para exportar logs ao Loki
- `docker-compose.yml`: healthchecks completos, `depends_on: condition: service_healthy`, resource limits, `GF_FEATURE_TOGGLES_ENABLE`
- `Dockerfile`: `HEALTHCHECK` adicionado, `InitialRAMPercentage` corrigido de 75% para 50%

### Testes criados
- `PersonServiceTest.java` — unit tests completos (Mockito, sem Spring context)
- `PersonValidationPropertyTest.java` — jqwik PBT (4 properties)
- `ContractBase.java` + contratos YAML em `src/test/resources/contracts/persons/`
- `persons.feature` + `CucumberSuite` + `CucumberSpringConfig` + `PersonStepDefinitions`
- BDD reutiliza `AbstractIT` (Testcontainers + MockMvcTester)

### Dependências adicionadas ao pom.xml
- `spring-cloud-starter-contract-verifier`
- `cucumber-spring` 7.22.0
- `cucumber-junit-platform-engine` 7.22.0
- `junit-platform-suite`
- Plugin `spring-cloud-contract-maven-plugin` com `ContractBase` como base class

### Versões de imagens Docker (docker-compose)
- `postgres:18.3`, `otel/opentelemetry-collector-contrib:0.123.0`
- `jaegertracing/jaeger:2.5.0`, `prom/prometheus:v3.3.0`
- `grafana/loki:3.5.4`, `grafana/promtail:3.5.4`, `grafana/grafana:12.0.0`