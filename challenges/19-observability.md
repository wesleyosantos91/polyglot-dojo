# 🏆 Desafio 19 — Full Observability Stack (Metrics, Traces, Logs & Profiling)

> **Nível:** ⭐⭐⭐⭐ Expert
> **Tipo:** Observability · OpenTelemetry · Prometheus · Grafana · Loki · Tempo · Jaeger · Pyroscope
> **Estimativa:** 12–16 horas por stack

---

## 📋 Descrição

Implementar **observabilidade completa e production-grade** (4 pilares: Metrics, Traces, Logs, Profiling) na API Person usando **OpenTelemetry** como padrão unificado. Integrar com **Grafana stack** (Prometheus, Tempo, Loki, Pyroscope, Alloy), configurar correlação total entre sinais, criar dashboards, alertas, SLOs e **exemplars** para navegação instantânea de métricas → traces → logs.

---

## 🎯 Objetivos de Aprendizado

- [ ] 4 pilares de observabilidade: Metrics, Traces, Logs, Profiling
- [ ] **OpenTelemetry SDK** — auto-instrumentation + instrumentation manual
- [ ] **OTel Collector** — pipelines (receivers, processors, exporters)
- [ ] Métricas customizadas (Counters, Gauges, Histograms, Summaries)
- [ ] **Exemplars** — vincular métricas a traces específicos
- [ ] Tracing distribuído (context propagation W3C TraceContext)
- [ ] **Structured logging** (JSON, ECS format, log correlation)
- [ ] **Continuous Profiling** com Pyroscope
- [ ] Correlação total: Metrics ↔ Traces ↔ Logs ↔ Profiles
- [ ] **Grafana dashboards** com drill-down entre sinais
- [ ] Alerting + Recording Rules (Prometheus)
- [ ] **SLIs / SLOs / Error Budgets**
- [ ] **Grafana Alloy** (collector unificado)
- [ ] **Jaeger** como alternativa de tracing UI

---

## 📐 Especificação

### Arquitetura de Observabilidade

```
┌─────────────┐     ┌──────────────────────┐     ┌──────────────┐
│  api-person  │────▶│   OTel Collector     │────▶│  Prometheus  │  ← Métricas
│  (OTel SDK)  │     │  (gRPC :4317)        │────▶│  Tempo       │  ← Traces
│              │     │  (HTTP :4318)        │────▶│  Loki        │  ← Logs
└─────────────┘     └──────────────────────┘     └──────┬───────┘
       │                                                 │
       │            ┌──────────────────────┐     ┌───────▼───────┐
       └───────────▶│  Grafana Alloy       │     │   Grafana     │  ← Dashboards
                    │  (log collector)     │────▶│   (3000)      │  ← Alerting
                    └──────────────────────┘     └───────────────┘
       │                                                 ▲
       └──────────────────▶ Pyroscope ───────────────────┘  ← Profiling
                           Jaeger UI (:16686)  ← Tracing alternativo
```

### Stack de Observabilidade Completa

| Componente | Tool | Porta | Propósito |
|---|---|---|---|
| **Métricas** | Prometheus | 9090 | Coleta, armazenamento e consulta (PromQL) |
| **Traces** | Grafana Tempo | 3200 | Backend de tracing (armazenamento) |
| **Traces UI** | Jaeger | 16686 | UI alternativa para explorar traces |
| **Logs** | Grafana Loki | 3100 | Agregação de logs (LogQL) |
| **Profiling** | Grafana Pyroscope | 4040 | Continuous profiling (CPU, memory) |
| **Dashboards** | Grafana | 3000 | Visualização unificada de todos os sinais |
| **Collector** | OTel Collector | 4317/4318 | Pipeline de telemetria (recebe, processa, exporta) |
| **Log Agent** | Grafana Alloy | 12345 | Coleta logs de containers e envia para Loki |
| **Alerting** | Grafana Alerting | — | Alertas unificados de métricas e logs |

---

### OpenTelemetry Collector — Pipeline

```yaml
# otel-collector-config.yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 5s
    send_batch_size: 1024
  memory_limiter:
    check_interval: 1s
    limit_mib: 256
  resource:
    attributes:
      - key: environment
        value: development
        action: upsert
  attributes:
    actions:
      - key: http.request.header.authorization
        action: delete
  tail_sampling:
    decision_wait: 10s
    policies:
      - name: errors
        type: status_code
        status_code: { status_codes: [ERROR] }
      - name: slow-requests
        type: latency
        latency: { threshold_ms: 1000 }
      - name: probabilistic
        type: probabilistic
        probabilistic: { sampling_percentage: 10 }

exporters:
  otlphttp/tempo:
    endpoint: http://tempo:3200
  otlphttp/loki:
    endpoint: http://loki:3100/otlp
  prometheus:
    endpoint: 0.0.0.0:8889
    resource_to_telemetry_conversion:
      enabled: true
  otlphttp/jaeger:
    endpoint: http://jaeger:4318
  otlphttp/pyroscope:
    endpoint: http://pyroscope:4040

connectors:
  spanmetrics:
    dimensions:
      - name: http.method
      - name: http.route
      - name: http.status_code

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch, attributes]
      exporters: [otlphttp/tempo, otlphttp/jaeger, spanmetrics]
    metrics:
      receivers: [otlp, spanmetrics]
      processors: [memory_limiter, batch]
      exporters: [prometheus]
    logs:
      receivers: [otlp]
      processors: [memory_limiter, resource, batch]
      exporters: [otlphttp/loki]
```

---

### Métricas Customizadas

| Métrica | Tipo | Descrição |
|---|---|---|
| `person_api_requests_total` | Counter | Total de requests por endpoint, método e status |
| `person_api_request_duration_seconds` | Histogram | Latência por endpoint com **exemplars** |
| `person_api_active_connections` | Gauge | Conexões ativas no momento |
| `person_api_db_query_duration_seconds` | Histogram | Latência de queries com **exemplars** |
| `person_api_db_pool_active` | Gauge | Conexões ativas no pool de DB |
| `person_api_db_pool_idle` | Gauge | Conexões idle no pool de DB |
| `person_api_persons_total` | Gauge | Total de persons no banco |
| `person_api_errors_total` | Counter | Erros por tipo (validation, db, timeout) |
| `person_api_cache_hits_total` | Counter | Cache hits |
| `person_api_cache_misses_total` | Counter | Cache misses |
| `person_api_inflight_requests` | Gauge | Requests em processamento |

### Exemplars (Métricas → Traces)

```
# HELP person_api_request_duration_seconds Request duration
# TYPE person_api_request_duration_seconds histogram
person_api_request_duration_seconds_bucket{method="POST",route="/api/persons",le="0.5"} 42 # {trace_id="abc123"} 0.345 1709547000.000
```

> **Exemplars** permitem clicar direto de um ponto no gráfico de métricas e ir para o trace correspondente no Tempo/Jaeger.

---

### Traces — Distributed Tracing

#### Span Hierarchy

```
[api-person-spring] POST /api/persons                                     200ms
  ├── [middleware] SecurityFilter                                           2ms
  │     └── [http] GET keycloak/realms/person-api/protocol/openid-connect   15ms
  ├── [handler] PersonHandler.create                                      180ms
  │   ├── [validation] ValidatePersonRequest                                1ms
  │   ├── [db] SELECT FROM persons WHERE email = ?                         12ms
  │   ├── [db] INSERT INTO persons (name, email, ...)                      35ms
  │   ├── [cache] SET person:{id}                                           3ms
  │   ├── [http] POST notification-service/api/notify                     100ms  ← propagação W3C
  │   │     ├── [db] INSERT INTO notifications                              8ms
  │   │     └── [smtp] SEND email                                          85ms
  │   └── [kafka] PUBLISH person-events (partition=2)                      15ms
  └── [serialization] PersonResponse → JSON                                 3ms

Baggage: { "tenant_id": "acme", "request_source": "web" }
```

#### Context Propagation

```
# W3C TraceContext headers propagados automaticamente entre serviços:
traceparent: 00-abc123def456789-span789ghi-01
tracestate: vendor=value
baggage: tenant_id=acme,request_source=web
```

---

### Structured Logging — Padrão ECS (Elastic Common Schema)

```json
{
  "@timestamp": "2026-03-04T10:30:00.123Z",
  "log.level": "INFO",
  "log.logger": "io.github.person.handler.PersonHandler",
  "message": "Person created successfully",
  "service.name": "api-person-spring",
  "service.version": "1.0.0",
  "service.environment": "development",
  "trace.id": "abc123def456789012345678",
  "span.id": "789ghijkl",
  "transaction.id": "abc123def456",
  "http.request.method": "POST",
  "url.path": "/api/persons",
  "http.response.status_code": 201,
  "event.duration": 195000000,
  "person.id": 1,
  "person.email": "john@example.com",
  "host.name": "api-person-spring-7b8c9d",
  "process.thread.name": "virtual-thread-42"
}
```

#### Níveis de Log com Contexto

| Nível | Quando usar | Exemplo |
|---|---|---|
| `ERROR` | Falha que requer atenção | `"message": "Failed to insert person", "error.type": "PSQLException", "error.message": "unique constraint violated"` |
| `WARN` | Situação inesperada mas recuperável | `"message": "Cache miss, falling back to DB", "cache.key": "person:42"` |
| `INFO` | Evento de negócio relevante | `"message": "Person created", "person.id": 1` |
| `DEBUG` | Informação de desenvolvimento | `"message": "Executing query", "db.statement": "SELECT * FROM persons WHERE id = ?"` |

---

### Continuous Profiling (Pyroscope)

| Profile Type | Descrição |
|---|---|
| `cpu` | Tempo de CPU por função |
| `alloc_objects` | Alocações de objetos |
| `alloc_space` | Memória alocada |
| `inuse_objects` | Objetos em uso |
| `inuse_space` | Memória em uso |
| `goroutines` | Goroutines ativas (Go) |
| `mutex` | Contenção de mutex |
| `block` | Eventos de blocking |

> No Grafana, correlacione: uma **métrica** mostra latência alta → abra o **trace** daquele momento → veja os **logs** com mesmo `trace_id` → analise o **profile** para ver exatamente onde o CPU gastou tempo.

---

### Grafana Dashboards (6 dashboards obrigatórios)

#### 1. **Service Overview (RED Method)**
- Request Rate (req/s) por endpoint
- Error Rate (%) por endpoint
- Duration (P50, P90, P95, P99) com **exemplars** clicáveis
- Apdex score

#### 2. **JVM / Go Runtime**
- **Java:** Heap usage, non-heap, GC pauses, GC count, Thread count (virtual vs platform), class loading
- **Go:** Goroutine count, GC pause, heap alloc, stack alloc, live objects

#### 3. **Database Performance**
- Query duration (P50, P99)
- Connection pool utilization (active/idle/max)
- Slow queries (> 100ms)
- Queries per second

#### 4. **Distributed Tracing**
- Service map (auto-gerado pelo Tempo)
- Trace search por duração, status, service
- Span breakdown por operação
- Error traces highlight

#### 5. **Logs Explorer**
- Live tail (streaming de logs em tempo real)
- Filter por service, level, trace_id
- Log volume (histogram de logs por nível)
- Correlação: clique no log → abre trace no Tempo

#### 6. **SLO Dashboard**
- SLI: Availability = requisições bem-sucedidas / total
- SLI: Latency = P99 < 500ms
- Error budget remaining (%)
- Burn rate

---

### SLIs / SLOs / Error Budgets

| SLO | SLI (Métrica) | Target | Window |
|---|---|---|---|
| Availability | `1 - (rate(http_server_errors[5m]) / rate(http_server_requests[5m]))` | 99.9% | 30 dias |
| Latency (P99) | `histogram_quantile(0.99, rate(http_server_duration_bucket[5m]))` | < 500ms | 30 dias |
| Throughput | `rate(http_server_requests_total[5m])` | > 100 req/s | 30 dias |

```yaml
# Prometheus Recording Rules para SLOs
groups:
  - name: slo-person-api
    interval: 30s
    rules:
      - record: person_api:sli:availability
        expr: |
          1 - (
            sum(rate(http_server_request_duration_seconds_count{http_response_status_code=~"5.."}[5m]))
            /
            sum(rate(http_server_request_duration_seconds_count[5m]))
          )
      - record: person_api:sli:latency_p99
        expr: |
          histogram_quantile(0.99, 
            sum(rate(http_server_request_duration_seconds_bucket[5m])) by (le)
          )
      - record: person_api:error_budget:remaining
        expr: |
          1 - (
            (1 - person_api:sli:availability)
            /
            (1 - 0.999)
          )
```

---

### Alertas (Prometheus + Grafana Alerting)

| Alerta | Condição (PromQL) | Severidade | Ação |
|---|---|---|---|
| **HighErrorRate** | `rate(http_server_errors[5m]) / rate(http_server_requests[5m]) > 0.05` | 🔴 Critical | PagerDuty + Slack |
| **HighLatencyP99** | `histogram_quantile(0.99, rate(...[5m])) > 2` | 🟡 Warning | Slack |
| **ServiceDown** | `up{job="api-person"} == 0` por 1min | 🔴 Critical | PagerDuty |
| **HighDBLatency** | `histogram_quantile(0.95, rate(db_query_duration[5m])) > 0.5` | 🟡 Warning | Slack |
| **ErrorBudgetBurn** | `person_api:error_budget:remaining < 0.25` | 🔴 Critical | PagerDuty |
| **HighMemoryUsage** | `jvm_memory_used_bytes / jvm_memory_max_bytes > 0.85` | 🟡 Warning | Slack |
| **TooManyGoroutines** | `go_goroutines > 10000` | 🟡 Warning | Slack |
| **LogErrorSpike** | LogQL: `sum(rate({job="api-person"} \|= "ERROR" [5m])) > 10` | 🟡 Warning | Slack |

---

## ✅ Critérios de Aceite

- [ ] OpenTelemetry SDK configurado (auto + manual instrumentation)
- [ ] OTel Collector com pipeline completo (receivers, processors, exporters)
- [ ] **Tail sampling** configurado no Collector (erros + lentos + probabilístico)
- [ ] Métricas customizadas com **exemplars** (link métrica → trace)
- [ ] Tracing distribuído com **context propagation** (W3C TraceContext + Baggage)
- [ ] Structured logging **JSON** com `trace_id` e `span_id` em todos os logs
- [ ] Correlação completa no Grafana: **Metrics → Traces → Logs → Profiles**
- [ ] **Jaeger UI** acessível como alternativa para explorar traces
- [ ] **Pyroscope** com profiling de CPU e memória
- [ ] 6 dashboards no Grafana (RED, JVM/Go, DB, Tracing, Logs, SLO)
- [ ] Alertas configurados (Prometheus rules + Grafana Alerting)
- [ ] **SLOs com error budget** configurados e visualizados
- [ ] Grafana **Alloy** coletando logs dos containers
- [ ] Docker Compose com stack completa (12+ services)
- [ ] Documentação: runbook de observabilidade

---

## 🐳 Docker Compose — Full Observability Stack

```yaml
services:
  # ========================
  # API Person
  # ========================
  api-person:
    build: .
    ports:
      - "8080:8080"
    environment:
      OTEL_SERVICE_NAME: api-person
      OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4318
      OTEL_METRICS_EXPORTER: otlp
      OTEL_TRACES_EXPORTER: otlp
      OTEL_LOGS_EXPORTER: otlp
      OTEL_RESOURCE_ATTRIBUTES: "service.version=1.0.0,deployment.environment=dev"
    depends_on:
      - otel-collector
      - postgres

  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: persondb
      POSTGRES_USER: person
      POSTGRES_PASSWORD: person123

  # ========================
  # OpenTelemetry Collector
  # ========================
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.123.0
    ports:
      - "4317:4317"    # gRPC receiver
      - "4318:4318"    # HTTP receiver
      - "8889:8889"    # Prometheus exporter
    volumes:
      - ./observability/otel-collector-config.yaml:/etc/otelcol-contrib/config.yaml
    depends_on:
      - tempo
      - loki
      - jaeger
      - pyroscope

  # ========================
  # Métricas — Prometheus
  # ========================
  prometheus:
    image: prom/prometheus:v3.4.1
    ports:
      - "9090:9090"
    volumes:
      - ./observability/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./observability/alerts.yml:/etc/prometheus/alerts.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=15d'
      - '--web.enable-remote-write-receiver'
      - '--enable-feature=exemplar-storage'

  # ========================
  # Traces — Tempo
  # ========================
  tempo:
    image: grafana/tempo:2.7.20
    ports:
      - "3200:3200"    # Tempo HTTP API
      - "9095:9095"    # Tempo gRPC
    volumes:
      - ./observability/tempo-config.yaml:/etc/tempo/config.yaml
    command: ["-config.file=/etc/tempo/config.yaml"]

  # ========================
  # Traces UI — Jaeger
  # ========================
  jaeger:
    image: jaegertracing/jaeger:2.6.0
    ports:
      - "16686:16686"  # Jaeger UI
      - "4318"         # OTLP HTTP receiver (internal)
    environment:
      COLLECTOR_OTLP_ENABLED: "true"

  # ========================
  # Logs —  Loki
  # ========================
  loki:
    image: grafana/loki:3.5.0
    ports:
      - "3100:3100"
    volumes:
      - ./observability/loki-config.yaml:/etc/loki/config.yaml
    command: ["-config.file=/etc/loki/config.yaml"]

  # ========================
  # Log Collector — Grafana Alloy
  # ========================
  alloy:
    image: grafana/alloy:v1.9.1
    ports:
      - "12345:12345"
    volumes:
      - ./observability/alloy-config.alloy:/etc/alloy/config.alloy
      - /var/run/docker.sock:/var/run/docker.sock:ro
    command: ["run", "/etc/alloy/config.alloy"]

  # ========================
  # Profiling — Pyroscope
  # ========================
  pyroscope:
    image: grafana/pyroscope:1.13.3
    ports:
      - "4040:4040"

  # ========================
  # Dashboards & Alerting — Grafana
  # ========================
  grafana:
    image: grafana/grafana:11.6.0
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
      GF_INSTALL_PLUGINS: grafana-pyroscope-app
      GF_FEATURE_TOGGLES_ENABLE: traceqlEditor tempoServiceGraph correlations
    volumes:
      - ./observability/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./observability/grafana/datasources:/etc/grafana/provisioning/datasources
      - ./observability/grafana/alerting:/etc/grafana/provisioning/alerting
    depends_on:
      - prometheus
      - tempo
      - loki
      - pyroscope
      - jaeger
```

---

### Grafana Datasources (Provisioning)

```yaml
# observability/grafana/datasources/datasources.yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
    isDefault: true
    jsonData:
      exemplarTraceIdDestinations:
        - name: trace_id
          datasourceUid: tempo
      httpMethod: POST

  - name: Tempo
    type: tempo
    uid: tempo
    url: http://tempo:3200
    jsonData:
      tracesToLogsV2:
        datasourceUid: loki
        filterByTraceID: true
        filterBySpanID: true
      tracesToMetrics:
        datasourceUid: Prometheus
      tracesToProfiles:
        datasourceUid: pyroscope
        profileTypeId: "process_cpu:cpu:nanoseconds:cpu:nanoseconds"
      serviceMap:
        datasourceUid: Prometheus
      nodeGraph:
        enabled: true

  - name: Loki
    type: loki
    uid: loki
    url: http://loki:3100
    jsonData:
      derivedFields:
        - name: TraceID
          matcherRegex: '"trace_id":"(\w+)"'
          url: "$${__value.raw}"
          datasourceUid: tempo
          matcherType: regex

  - name: Pyroscope
    type: grafana-pyroscope-datasource
    uid: pyroscope
    url: http://pyroscope:4040

  - name: Jaeger
    type: jaeger
    url: http://jaeger:16686
```

> **Correlação total:** Prometheus (exemplars) → Tempo → Loki (derived fields) → Pyroscope. Clique em qualquer sinal e navegue para os outros.

---

### Grafana Alloy — Log Collection

```alloy
// observability/alloy-config.alloy

// Descoberta de containers Docker
discovery.docker "containers" {
  host = "unix:///var/run/docker.sock"
}

// Coleta de logs dos containers
loki.source.docker "logs" {
  host          = "unix:///var/run/docker.sock"
  targets       = discovery.docker.containers.targets
  forward_to    = [loki.process.pipeline.receiver]
  relabel_rules = discovery.relabel.containers.rules
}

discovery.relabel "containers" {
  targets = discovery.docker.containers.targets
  rule {
    source_labels = ["__meta_docker_container_name"]
    target_label  = "container"
  }
  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
    target_label  = "service"
  }
}

// Pipeline de processamento
loki.process "pipeline" {
  stage.json {
    expressions = {
      level     = "log.level",
      trace_id  = "trace.id",
      span_id   = "span.id",
      service   = "service.name",
    }
  }
  stage.labels {
    values = {
      level    = "",
      service  = "",
    }
  }
  stage.structured_metadata {
    values = {
      trace_id = "",
      span_id  = "",
    }
  }
  forward_to = [loki.write.loki.receiver]
}

loki.write "loki" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

---

## 🛠️ Implementar em

| Stack | OTel SDK | Metrics | Logging | Profiling |
|---|---|---|---|---|
| **Spring Boot** | `micrometer-tracing-bridge-otel` + OTel Java Agent | `micrometer-registry-prometheus` | Logback + `logstash-logback-encoder` (JSON/ECS) | Pyroscope Java agent |
| **Micronaut** | `micronaut-tracing-opentelemetry-http` | `micronaut-micrometer-registry-prometheus` | Logback + `logstash-logback-encoder` | Pyroscope Java agent |
| **Quarkus** | `quarkus-opentelemetry` | `quarkus-micrometer-registry-prometheus` | `quarkus-logging-json` | Pyroscope Java agent |
| **Go** | `go.opentelemetry.io/otel` | `github.com/prometheus/client_golang` | `log/slog` (JSON handler) | `github.com/grafana/pyroscope-go` |

---

## 💡 Dicas

### OpenTelemetry Auto-Instrumentation + Manual (Spring)

```java
// application.yml
management:
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
    metrics:
      export:
        endpoint: http://otel-collector:4318/v1/metrics
  tracing:
    sampling:
      probability: 1.0
  metrics:
    tags:
      application: api-person-spring
    distribution:
      percentiles-histogram:
        http.server.requests: true

// Instrumentation manual — Span customizado
@Service
public class PersonService {
    private final Tracer tracer;
    
    public Person create(PersonRequest request) {
        Span span = tracer.nextSpan().name("PersonService.create").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("person.email", request.getEmail());
            
            // business logic...
            Person saved = repository.save(person);
            
            span.event("person.persisted");
            span.tag("person.id", String.valueOf(saved.getId()));
            return saved;
        } finally {
            span.end();
        }
    }
}
```

### Métricas Customizadas com Exemplars (Spring)

```java
@Component
public class PersonMetrics {
    private final MeterRegistry registry;

    public PersonMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Gauge — total de persons no banco
        Gauge.builder("person_api_persons_total", personRepository, CrudRepository::count)
            .description("Total persons in database")
            .register(registry);
    }

    public void recordPersonCreated() {
        registry.counter("person_api_persons_created_total").increment();
    }
    
    public void recordRequestDuration(String method, String path, int status, long durationMs) {
        Timer.builder("person_api_request_duration_seconds")
            .tag("method", method)
            .tag("path", path)
            .tag("status", String.valueOf(status))
            .publishPercentileHistogram()  // habilita exemplars
            .register(registry)
            .record(Duration.ofMillis(durationMs));
    }
}
```

### Structured Logging com Correlation (Logback)

```xml
<!-- logback-spring.xml -->
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeMdcKeyName>trace_id</includeMdcKeyName>
      <includeMdcKeyName>span_id</includeMdcKeyName>
      <customFields>{"service.name":"api-person-spring"}</customFields>
    </encoder>
  </appender>
  
  <root level="INFO">
    <appender-ref ref="JSON"/>
  </root>
</configuration>
```

### Go — OTel + slog + Pyroscope

```go
package main

import (
    "context"
    "log/slog"
    "os"
    
    "go.opentelemetry.io/otel"
    "go.opentelemetry.io/otel/attribute"
    "go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
    "go.opentelemetry.io/otel/sdk/trace"

    "github.com/grafana/pyroscope-go"
)

// Structured logger com trace correlation
func LogWithTrace(ctx context.Context, level slog.Level, msg string, attrs ...slog.Attr) {
    span := otel.SpanFromContext(ctx).SpanContext()
    allAttrs := append(attrs,
        slog.String("trace_id", span.TraceID().String()),
        slog.String("span_id", span.SpanID().String()),
        slog.String("service.name", "api-person-go"),
    )
    slog.LogAttrs(ctx, level, msg, allAttrs...)
}

// Handler com tracing manual
func (h *PersonHandler) Create(c *gin.Context) {
    ctx, span := h.tracer.Start(c.Request.Context(), "PersonHandler.Create")
    defer span.End()
    
    span.SetAttributes(attribute.String("person.email", req.Email))
    
    person, err := h.service.Create(ctx, req)
    if err != nil {
        span.RecordError(err)
        span.SetStatus(codes.Error, err.Error())
        LogWithTrace(ctx, slog.LevelError, "Failed to create person",
            slog.String("error", err.Error()),
        )
        return
    }
    
    span.AddEvent("person.created", trace.WithAttributes(
        attribute.Int64("person.id", int64(person.ID)),
    ))
    LogWithTrace(ctx, slog.LevelInfo, "Person created",
        slog.Int64("person.id", int64(person.ID)),
    )
}

// Pyroscope profiling
func initPyroscope() {
    pyroscope.Start(pyroscope.Config{
        ApplicationName: "api-person-go",
        ServerAddress:   "http://pyroscope:4040",
        ProfileTypes: []pyroscope.ProfileType{
            pyroscope.ProfileCPU,
            pyroscope.ProfileAllocObjects,
            pyroscope.ProfileAllocSpace,
            pyroscope.ProfileInuseObjects,
            pyroscope.ProfileInuseSpace,
            pyroscope.ProfileGoroutines,
            pyroscope.ProfileMutexCount,
            pyroscope.ProfileBlockCount,
        },
    })
}
```

### Quarkus — Config Mínima

```properties
# application.properties
quarkus.otel.exporter.otlp.endpoint=http://otel-collector:4318
quarkus.otel.service.name=api-person-quarkus
quarkus.otel.resource.attributes=service.version=1.0.0

# Structured JSON logging
quarkus.log.console.json=true
quarkus.log.console.json.additional-field."service.name".value=api-person-quarkus
```

---

## 📦 Dependências Extras

| Stack | Dependências |
|---|---|
| **Spring** | `micrometer-registry-prometheus` + `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` + `logstash-logback-encoder` |
| **Micronaut** | `micronaut-micrometer-registry-prometheus` + `micronaut-tracing-opentelemetry-http` + `logstash-logback-encoder` |
| **Quarkus** | `quarkus-micrometer-registry-prometheus` + `quarkus-opentelemetry` + `quarkus-logging-json` |
| **Go** | `go.opentelemetry.io/otel` + `go.opentelemetry.io/otel/exporters/otlp/*` + `github.com/prometheus/client_golang` + `github.com/grafana/pyroscope-go` |

---

## 🔗 Referências

- [OpenTelemetry — Docs](https://opentelemetry.io/docs/)
- [OTel Collector — Configuration](https://opentelemetry.io/docs/collector/configuration/)
- [Prometheus — PromQL](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana — Dashboards](https://grafana.com/docs/grafana/latest/dashboards/)
- [Grafana Tempo — TraceQL](https://grafana.com/docs/tempo/latest/traceql/)
- [Grafana Loki — LogQL](https://grafana.com/docs/loki/latest/query/)
- [Grafana Pyroscope](https://grafana.com/docs/pyroscope/latest/)
- [Grafana Alloy](https://grafana.com/docs/alloy/latest/)
- [Jaeger](https://www.jaegertracing.io/docs/)
- [Exemplars in Prometheus](https://prometheus.io/docs/prometheus/latest/feature_flags/#exemplars-storage)
- [SRE Book — SLOs](https://sre.google/sre-book/service-level-objectives/)
- [RED Method](https://grafana.com/blog/2018/08/02/the-red-method-how-to-instrument-your-services/)
- [ECS Logging](https://www.elastic.co/guide/en/ecs-logging/overview/current/intro.html)
