# 🏆 Desafio 26 — Resilience & Fault Tolerance Patterns

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** Resilience · Circuit Breaker · Retry · Bulkhead · Rate Limiter · Timeout
> **Estimativa:** 8–10 horas por stack

---

## 📋 Descrição

Implementar **patterns de resiliência** na API Person que integra com 3 serviços externos (Address, Notification, Score). Aplicar **Circuit Breaker**, **Retry**, **Bulkhead**, **Rate Limiter**, **Timeout** e **Fallback** para garantir que falhas em serviços externos não derrubem a API.

---

## 🎯 Objetivos de Aprendizado

- [ ] Circuit Breaker — estados (Closed, Open, Half-Open) e transições
- [ ] Retry — backoff exponencial, jitter, max attempts
- [ ] Bulkhead — isolamento de recursos (thread pool / semaphore)
- [ ] Rate Limiter — proteger contra sobrecarga
- [ ] Timeout — fail-fast quando serviço demora
- [ ] Fallback — resposta alternativa quando tudo falha
- [ ] Composição de patterns (Circuit Breaker + Retry + Timeout)
- [ ] Métricas de resiliência (Prometheus)
- [ ] Testes de falha com WireMock (simular erros e latência)

---

## 📐 Especificação

### Arquitetura

```
Client → API Person ─┬─→ Address Service   (Circuit Breaker + Retry + Timeout)
                      ├─→ Notification Svc  (Bulkhead + Timeout + Fallback)
                      └─→ Score Service     (Rate Limiter + Circuit Breaker)
                      
WireMock simula todos os serviços externos
```

### Circuit Breaker Configuration

| Parâmetro | Valor |
|---|---|
| Failure Rate Threshold | 50% |
| Slow Call Duration | 2 seconds |
| Slow Call Rate Threshold | 80% |
| Wait Duration in Open State | 30 seconds |
| Permitted Calls in Half-Open | 3 |
| Sliding Window Size | 10 calls |

### Retry Configuration

| Parâmetro | Valor |
|---|---|
| Max Attempts | 3 |
| Wait Duration | 500ms (initial) |
| Backoff Multiplier | 2.0 (500ms → 1s → 2s) |
| Jitter | ±200ms |
| Retry On | `IOException`, `TimeoutException`, HTTP 503 |
| Do Not Retry On | HTTP 400, 404 |

### Bulkhead Configuration

| Parâmetro | Valor |
|---|---|
| Max Concurrent Calls | 10 |
| Max Wait Duration | 500ms |
| Type | Semaphore (Java) |

### Rate Limiter Configuration

| Parâmetro | Valor |
|---|---|
| Limit for Period | 50 requests |
| Limit Refresh Period | 1 second |
| Timeout Duration | 0 (reject immediately) |

### Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/persons` | Cria person + chama Address + Notification |
| `GET` | `/api/persons/{id}/score` | Busca score (Rate Limited + Circuit Breaker) |
| `GET` | `/api/resilience/status` | Status de todos Circuit Breakers |
| `GET` | `/api/resilience/metrics` | Métricas de resiliência (calls, failures, state) |
| `POST` | `/api/resilience/test/circuit-breaker` | Trigger Circuit Breaker (test endpoint) |

### Cenários de Falha (WireMock)

| Cenário | Simulação | Comportamento Esperado |
|---|---|---|
| Serviço lento | Delay 5s | Timeout após 2s → Retry → Fallback |
| Serviço down | HTTP 503 | Retry 3x → Circuit Breaker Open → Fallback |
| Error rate alta | 60% HTTP 500 | Circuit Breaker abre após 10 calls |
| Serviço recovery | 503 → delay → 200 | Circuit Breaker: Open → Half-Open → Closed |
| Sobrecarga | 100 req/s | Rate Limiter rejeita excedente (429) |
| Recurso escasso | 20 concurrent | Bulkhead rejeita excedente (503) |

### Fallback Responses

```json
// Fallback do Address Service
{
  "address": null,
  "fallback": true,
  "message": "Address service temporarily unavailable",
  "cached_at": null
}

// Fallback do Score Service (cached value)
{
  "score": 500,
  "tier": "STANDARD",
  "fallback": true,
  "cached_at": "2026-03-04T09:00:00Z",
  "message": "Using cached score"
}
```

---

## ✅ Critérios de Aceite

- [ ] Circuit Breaker com 3 estados funcionando
- [ ] Retry com backoff exponencial e jitter
- [ ] Bulkhead limitando chamadas concorrentes
- [ ] Rate Limiter com rejeição (HTTP 429)
- [ ] Timeout configurável por serviço
- [ ] Fallback com resposta degradada (parcial ou cache)
- [ ] Composição: Circuit Breaker → Retry → Timeout
- [ ] Endpoint de status dos Circuit Breakers
- [ ] Métricas expostas para Prometheus
- [ ] WireMock simulando todos os cenários de falha
- [ ] Testes de integração para cada padrão
- [ ] Teste de transição de estado do Circuit Breaker

---

## 🛠️ Implementar em

| Stack | Library |
|---|---|
| **Spring Boot** | Resilience4j + `spring-boot-starter-aop` + annotations |
| **Micronaut** | `micronaut-resilience4j` ou Micronaut Retry built-in |
| **Quarkus** | MicroProfile Fault Tolerance (`@CircuitBreaker`, `@Retry`, etc.) |
| **Go** | `sony/gobreaker` (CB) + `avast/retry-go` + custom middleware |

---

## 💡 Dicas

### Spring Boot — Resilience4j
```java
@Service
public class AddressClient {

    @CircuitBreaker(name = "addressService", fallbackMethod = "addressFallback")
    @Retry(name = "addressService")
    @Timeout(name = "addressService")
    public Address findByPersonId(Long personId) {
        return restClient.get()
            .uri("/api/addresses?personId={id}", personId)
            .retrieve()
            .body(Address.class);
    }

    private Address addressFallback(Long personId, Throwable t) {
        log.warn("Address fallback triggered: {}", t.getMessage());
        return Address.unknown(); // resposta degradada
    }
}
```

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      addressService:
        failureRateThreshold: 50
        slowCallDurationThreshold: 2s
        slowCallRateThreshold: 80
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        slidingWindowSize: 10
  retry:
    instances:
      addressService:
        maxAttempts: 3
        waitDuration: 500ms
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
  bulkhead:
    instances:
      notificationService:
        maxConcurrentCalls: 10
        maxWaitDuration: 500ms
```

### Quarkus — MicroProfile Fault Tolerance
```java
@ApplicationScoped
public class ScoreClient {

    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 30, delayUnit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 500, jitter = 200)
    @Timeout(2000)
    @Fallback(fallbackMethod = "scoreFallback")
    @RateLimiter(value = 50, window = 1, windowUnit = ChronoUnit.SECONDS)
    public Score getScore(Long personId) {
        return restClient.getScore(personId);
    }

    public Score scoreFallback(Long personId) {
        return Score.defaultScore(); // cached/default
    }
}
```

### Go — gobreaker + retry-go
```go
type AddressClient struct {
    cb     *gobreaker.CircuitBreaker
    client *http.Client
}

func NewAddressClient() *AddressClient {
    settings := gobreaker.Settings{
        Name:          "address-service",
        MaxRequests:   3,                  // half-open
        Interval:      10 * time.Second,   // sliding window
        Timeout:       30 * time.Second,   // open → half-open
        ReadyToTrip: func(counts gobreaker.Counts) bool {
            return counts.ConsecutiveFailures > 5
        },
        OnStateChange: func(name string, from, to gobreaker.State) {
            log.Printf("Circuit Breaker %s: %s → %s", name, from, to)
        },
    }
    return &AddressClient{
        cb:     gobreaker.NewCircuitBreaker(settings),
        client: &http.Client{Timeout: 2 * time.Second},
    }
}

func (c *AddressClient) FindByPersonID(ctx context.Context, id int64) (*Address, error) {
    result, err := c.cb.Execute(func() (any, error) {
        return retry.DoWithData(
            func() (*Address, error) {
                return c.doRequest(ctx, id)
            },
            retry.Attempts(3),
            retry.Delay(500*time.Millisecond),
            retry.DelayType(retry.BackOffDelay),
            retry.Context(ctx),
        )
    })
    if err != nil {
        return c.fallback(id) // fallback
    }
    return result.(*Address), nil
}
```

---

## 🐳 Docker Compose

```yaml
services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: persondb
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin
    ports:
      - "5432:5432"

  wiremock:
    image: wiremock/wiremock:3.12.1
    ports:
      - "8888:8080"
    volumes:
      - ./wiremock:/home/wiremock
    command: --verbose --global-response-templating

  prometheus:
    image: prom/prometheus:v3.2.1
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
```

---

## 📦 Dependências

| Stack | Dependência |
|---|---|
| Spring | `resilience4j-spring-boot3`, `resilience4j-micrometer` |
| Micronaut | `micronaut-resilience4j` |
| Quarkus | `quarkus-smallrye-fault-tolerance` |
| Go | `github.com/sony/gobreaker`, `github.com/avast/retry-go/v4` |

---

## 🔗 Referências

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [MicroProfile Fault Tolerance](https://microprofile.io/microprofile-fault-tolerance/)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [gobreaker — Go Circuit Breaker](https://github.com/sony/gobreaker)
