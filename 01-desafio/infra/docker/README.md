# 🐳 Infra Docker

Compose principal da infraestrutura do desafio 01.

Arquivo: [`docker-compose.yml`](./docker-compose.yml)

## Serviços

- `postgres`: banco principal (`dev`)
- `api-person-spring`: API Spring Boot
- `otel-collector`: coletor OpenTelemetry
- `prometheus`: métricas
- `grafana`: dashboards
- `jaeger`: traces
- `loki`: logs
- `promtail`: coleta de logs do Docker

## Subir ambiente

```bash
docker compose up -d --build api-person-spring
```

Subir tudo:

```bash
docker compose up -d --build
```

## Acompanhar

```bash
docker compose ps
docker compose logs -f api-person-spring
docker compose logs -f postgres
```

## Parar

```bash
docker compose down
```

Removendo volumes:

```bash
docker compose down -v
```

> Se você já tinha subido o Postgres com setup antigo, execute `docker compose down -v` antes de subir novamente para garantir um volume limpo.

## Endpoints

- API: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Liveness: `http://localhost:8080/livez`
- Readiness: `http://localhost:8080/readyz`
- Startup: `http://localhost:8080/startupz`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Jaeger UI: `http://localhost:16686`
- OTLP HTTP: `http://localhost:4318`

## Observações

- O serviço `api-person-spring` depende de `postgres` saudável.
- O schema do banco é gerenciado pela API via Flyway (não há mais `init.sql` no Postgres).
- A configuração de observabilidade está em `./observability/`.
- Dashboards e datasources do Grafana estão em `./grafana/`.
- Regras de alerta do Prometheus estão em `./observability/alert-rules.yml`.

## Dashboards Grafana

Dashboards disponíveis em `./grafana/dashboards/` (provisionados automaticamente):

| Dashboard | Arquivo | Descrição |
|---|---|---|
| **Golden Signals** | `api-person-spring-golden-signals.json` | SLI/SLO/Apdex, tráfego, latência (heatmap + percentis), erros, saturação, GC, resiliência |
| **Troubleshooting & RCA** | `api-person-spring-troubleshooting-rca.json` | Investigação de incidentes: logs Loki, retry por exceção, latência por endpoint, HikariCP acquire/usage time |
| **JVM & Infrastructure** | `api-person-spring-jvm-infra.json` | Deep-dive JVM: heap/non-heap por pool, GC overhead, threads por estado, class loading, file descriptors, buffer pools |

Todos os dashboards possuem **links de navegação** entre si e variáveis compartilhadas (`service`, `DS_PROMETHEUS`, `DS_LOKI`).

## Alertas ativos (Prometheus)

Arquivo: `./observability/alert-rules.yml`

- `ApiPersonServerErrorRateHigh`: taxa de `http_access` com `outcome=SERVER_ERROR` acima de 5%.
- `ApiPersonLatencyP95High`: p95 HTTP acima de 500ms.
- `ApiPersonLatencyP99High`: p99 HTTP acima de 1s.
- `ApiPersonDbRetryExhausted`: pelo menos 1 retry de banco esgotado nos últimos 5 minutos.

Para validar no Prometheus:

```bash
curl -s http://localhost:9090/api/v1/rules
curl -s http://localhost:9090/api/v1/alerts
```

## Dockerfile da API Spring

A API `api-person-spring` usa um único Dockerfile oficial:

- `Dockerfile`

O `docker-compose.yml` já está configurado para esse Dockerfile com:

- `TRAINING_MODE=full` no build
- geração de `app.aot` durante o stage de treino
- foco em cold start e warmup com cenário real
