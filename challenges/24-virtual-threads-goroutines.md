# 🏆 Desafio 24 — Virtual Threads, Goroutines & Channels

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** Concurrency · Virtual Threads · Goroutines · Channels · Structured Concurrency
> **Estimativa:** 8–12 horas por stack

---

## 📋 Descrição

Criar uma **API de agregação de dados** que faz múltiplas chamadas paralelas a serviços externos e agrega os resultados. O foco é dominar **Virtual Threads** (Java 25 — JEP 444) e **Goroutines + Channels** (Go), comparando modelos de concorrência, performance e legibilidade.

---

## 🎯 Objetivos de Aprendizado

### Java (Spring/Micronaut/Quarkus)
- [ ] Virtual Threads vs Platform Threads — diferenças fundamentais
- [ ] `Thread.ofVirtual().start()` — criação direta
- [ ] `Executors.newVirtualThreadPerTaskExecutor()` — executor
- [ ] Structured Concurrency (`StructuredTaskScope` — JEP 480)
- [ ] `StructuredTaskScope.ShutdownOnFailure` — fail-fast
- [ ] `StructuredTaskScope.ShutdownOnSuccess` — race pattern
- [ ] Scoped Values (`ScopedValue` — JEP 481) vs ThreadLocal
- [ ] Pinning — quando Virtual Threads bloqueiam carrier threads
- [ ] Configuração no Spring Boot: `spring.threads.virtual.enabled=true`

### Go
- [ ] Goroutines — lightweight green threads
- [ ] Channels — comunicação entre goroutines (unbuffered vs buffered)
- [ ] `select` — multiplexação de channels
- [ ] `sync.WaitGroup` — esperar goroutines terminarem
- [ ] `errgroup.Group` — goroutines com propagação de erro
- [ ] `context.Context` — cancelamento e timeout
- [ ] Channel patterns: fan-out, fan-in, pipeline, worker pool
- [ ] Data races — `go test -race`

---

## 📐 Especificação

### Cenário: Person Profile Aggregator

A API recebe um `person_id` e agrega dados de **5 serviços** em paralelo:

```
GET /api/persons/{id}/profile

  ┌─────────────────────────────────────────────┐
  │            Person Profile Aggregator         │
  │                                              │
  │  ┌─── Service 1: Person (PostgreSQL)     ──┐ │
  │  ├─── Service 2: Address (ViaCEP API)    ──┤ │
  │  ├─── Service 3: Orders (HTTP service)   ──┤ │  Paralelo
  │  ├─── Service 4: Score (cálculo lento)   ──┤ │
  │  └─── Service 5: Avatar (S3/external)    ──┘ │
  │                                              │
  │  Timeout global: 3 segundos                  │
  │  Se 1 falha: retorna parcial (com flag)      │
  └─────────────────────────────────────────────┘
```

### Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/persons/{id}/profile` | Agrega dados de 5 fontes em paralelo |
| `GET` | `/api/persons/{id}/profile?timeout=2s` | Com timeout customizado |
| `GET` | `/api/benchmark/sequential` | Mesma agregação sequencial (para comparação) |
| `GET` | `/api/benchmark/parallel` | Agregação paralela com métricas |
| `GET` | `/api/benchmark/race?services=svc1,svc2` | Retorna o primeiro resultado |
| `POST` | `/api/batch/persons` | Processa N persons em paralelo (worker pool) |

### Response (Profile Aggregado)

```json
{
  "person": { "id": 1, "name": "Wesley", "email": "..." },
  "address": { "cep": "01001-000", "street": "Praça da Sé", "city": "São Paulo" },
  "orders": { "total": 15, "last_order": "2026-03-01" },
  "score": { "value": 850, "tier": "GOLD" },
  "avatar_url": "https://s3.amazonaws.com/avatars/1.jpg",
  "metadata": {
    "total_time_ms": 450,
    "services": {
      "person": { "status": "OK", "time_ms": 12 },
      "address": { "status": "OK", "time_ms": 230 },
      "orders": { "status": "OK", "time_ms": 180 },
      "score": { "status": "OK", "time_ms": 450 },
      "avatar": { "status": "TIMEOUT", "time_ms": 3000, "error": "deadline exceeded" }
    },
    "thread_type": "virtual",
    "partial": true
  }
}
```

### Padrões de Concorrência a Implementar

| Padrão | Descrição | Java | Go |
|---|---|---|---|
| **Fan-Out** | 1 request → N chamadas paralelas | Structured Concurrency | N goroutines |
| **Fan-In** | N resultados → 1 response | `StructuredTaskScope` | `select` + channel |
| **Worker Pool** | N tasks → M workers | Virtual Thread pool | Buffered channel + goroutines |
| **Race** | N fontes → primeiro resultado | `ShutdownOnSuccess` | `select` first channel |
| **Pipeline** | Stage 1 → Stage 2 → Stage 3 | CompletableFuture chain | Channel pipeline |
| **Timeout** | Cancelar se exceder limite | `context.withTimeout()` | `context.WithTimeout()` |

---

## ✅ Critérios de Aceite

### Java
- [ ] Virtual Threads habilitados para request handling
- [ ] Structured Concurrency para agregação paralela (JEP 480)
- [ ] `ShutdownOnFailure` para fan-out com fail-fast
- [ ] `ShutdownOnSuccess` para race pattern
- [ ] ScopedValue para propagar context (correlation-id)
- [ ] Benchmark: Virtual vs Platform threads (latência e throughput)
- [ ] Teste de pinning: detectar e resolver synchronized → ReentrantLock
- [ ] Worker pool para batch processing

### Go
- [ ] Fan-out/fan-in com goroutines + channels
- [ ] `errgroup.Group` com context para cancelamento
- [ ] Worker pool com buffered channel
- [ ] `select` para race pattern e timeout
- [ ] Pipeline pattern (channel chaining)
- [ ] `go test -race` sem data races
- [ ] Benchmark: `testing.B` comparando sequential vs concurrent

### Ambos
- [ ] Timeout global de 3s
- [ ] Resposta parcial quando 1+ serviço falha
- [ ] Métricas: tempo por serviço, tempo total, thread type
- [ ] Endpoint de benchmark para comparação

---

## 💡 Dicas

### Spring Boot — Virtual Threads + Structured Concurrency
```java
// application.yml
spring:
  threads:
    virtual:
      enabled: true  # Todas as requests usam Virtual Threads

// Structured Concurrency (JEP 480)
@Service
public class ProfileAggregator {

    public PersonProfile aggregate(Long personId) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Subtask<Person> personTask = scope.fork(() -> personService.findById(personId));
            Subtask<Address> addressTask = scope.fork(() -> addressClient.findByPersonId(personId));
            Subtask<OrderSummary> ordersTask = scope.fork(() -> orderClient.getSummary(personId));
            Subtask<Score> scoreTask = scope.fork(() -> scoreCalculator.calculate(personId));
            Subtask<String> avatarTask = scope.fork(() -> avatarService.getUrl(personId));

            scope.joinUntil(Instant.now().plusSeconds(3)); // timeout 3s
            scope.throwIfFailed();

            return new PersonProfile(
                personTask.get(),
                addressTask.get(),
                ordersTask.get(),
                scoreTask.get(),
                avatarTask.get()
            );
        }
    }
}
```

### Spring Boot — Scoped Values
```java
private static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

public Response handleRequest(String correlationId) {
    return ScopedValue.where(CORRELATION_ID, correlationId)
        .call(() -> {
            // Todos os Virtual Threads filhos enxergam CORRELATION_ID
            return profileAggregator.aggregate(personId);
        });
}
```

### Go — Fan-Out/Fan-In com errgroup
```go
func (s *ProfileService) Aggregate(ctx context.Context, personID int64) (*Profile, error) {
    ctx, cancel := context.WithTimeout(ctx, 3*time.Second)
    defer cancel()

    g, ctx := errgroup.WithContext(ctx)
    profile := &Profile{}

    g.Go(func() error {
        person, err := s.personRepo.FindByID(ctx, personID)
        if err != nil { return err }
        profile.Person = person
        return nil
    })

    g.Go(func() error {
        address, err := s.addressClient.Find(ctx, personID)
        if err != nil { return err }
        profile.Address = address
        return nil
    })

    g.Go(func() error {
        orders, err := s.orderClient.GetSummary(ctx, personID)
        if err != nil { return err }
        profile.Orders = orders
        return nil
    })

    if err := g.Wait(); err != nil {
        return profile, err // retorna parcial
    }
    return profile, nil
}
```

### Go — Worker Pool
```go
func ProcessBatch(ctx context.Context, persons []Person, workers int) []Result {
    jobs := make(chan Person, len(persons))
    results := make(chan Result, len(persons))

    // Start workers
    for w := 0; w < workers; w++ {
        go func() {
            for person := range jobs {
                result := processOne(ctx, person)
                results <- result
            }
        }()
    }

    // Send jobs
    for _, p := range persons {
        jobs <- p
    }
    close(jobs)

    // Collect results
    var output []Result
    for range persons {
        output = append(output, <-results)
    }
    return output
}
```

### Go — Select + Timeout
```go
func RaceServices(ctx context.Context, services ...func(context.Context) (any, error)) (any, error) {
    ctx, cancel := context.WithTimeout(ctx, 2*time.Second)
    defer cancel()

    ch := make(chan any, len(services))
    for _, svc := range services {
        go func(fn func(context.Context) (any, error)) {
            result, err := fn(ctx)
            if err == nil {
                ch <- result
            }
        }(svc)
    }

    select {
    case result := <-ch:
        return result, nil // primeiro que responder
    case <-ctx.Done():
        return nil, ctx.Err()
    }
}
```

---

## 📊 Benchmark Esperado

| Cenário | Sequential | Virtual Threads / Goroutines |
|---|---|---|
| 5 serviços (200ms cada) | ~1000ms | ~200ms |
| 100 requests simultâneos | ~100s | ~2-3s |
| Batch 1000 persons | ~50s | ~5s (10 workers) |

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

  # Mock dos serviços externos
  mock-services:
    image: wiremock/wiremock:3.12.1
    ports:
      - "8888:8080"
    volumes:
      - ./wiremock:/home/wiremock
```

---

## 📦 Dependências

| Stack | Dependência |
|---|---|
| Spring | JDK 25+ (built-in Virtual Threads, Structured Concurrency) |
| Micronaut | JDK 25+ (configurar Virtual Thread executor) |
| Quarkus | JDK 25+, `quarkus-virtual-threads` |
| Go | `golang.org/x/sync/errgroup` (demais é stdlib) |

---

## 🔗 Referências

- [JEP 444 — Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 480 — Structured Concurrency](https://openjdk.org/jeps/480)
- [JEP 481 — Scoped Values](https://openjdk.org/jeps/481)
- [Go Concurrency Patterns](https://go.dev/blog/pipelines)
- [errgroup Package](https://pkg.go.dev/golang.org/x/sync/errgroup)
- [Go Data Race Detector](https://go.dev/doc/articles/race_detector)
