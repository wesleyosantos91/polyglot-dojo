# 🏆 Desafio 28 — Reactive Streams & SSE (Server-Sent Events)

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** Reactive · Non-Blocking · SSE · Streaming · Backpressure
> **Estimativa:** 8–10 horas por stack

---

## 📋 Descrição

Implementar uma API Person com **dois modos de operação**:

1. **Blocking/Imperative** (baseline) — Virtual Threads
2. **Reactive/Non-Blocking** — WebFlux (Spring), Mutiny (Quarkus), Reactor (Micronaut)

Incluir **Server-Sent Events (SSE)** para streaming em tempo real de atualizações de persons, e **backpressure** para processar grandes volumes de dados sem sobrecarregar o sistema.

---

## 🎯 Objetivos de Aprendizado

### Reactive
- [ ] Mono/Flux (Reactor), Uni/Multi (Mutiny), Publisher (Micronaut)
- [ ] Non-blocking I/O — event loop vs thread-per-request
- [ ] Backpressure — controle de fluxo entre producer e consumer
- [ ] Reactive Database access (R2DBC)
- [ ] Reactive HTTP Client
- [ ] Error handling em fluxos reativos
- [ ] Testing reactive streams (`StepVerifier`)

### SSE
- [ ] Server-Sent Events — `text/event-stream`
- [ ] Emitter de eventos em tempo real
- [ ] Reconexão automática do cliente (`Last-Event-ID`)
- [ ] Heartbeat para manter conexão

### Go
- [ ] Streaming com `http.Flusher`
- [ ] Channels para backpressure nativo
- [ ] `io.Pipe` para streaming
- [ ] SSE com goroutines

---

## 📐 Especificação

### Endpoints

| Método | Rota | Tipo | Descrição |
|---|---|---|---|
| `GET` | `/api/persons` | JSON | Listar (imperative) |
| `GET` | `/api/reactive/persons` | JSON (reactive) | Listar (non-blocking) |
| `GET` | `/api/reactive/persons/stream` | `text/event-stream` | SSE: stream de persons |
| `GET` | `/api/reactive/persons/changes` | `text/event-stream` | SSE: mudanças em tempo real |
| `POST` | `/api/reactive/persons` | JSON (reactive) | Criar (non-blocking) |
| `GET` | `/api/reactive/persons/export` | `application/ndjson` | Stream 10k+ records (backpressure) |
| `POST` | `/api/reactive/persons/import` | Streaming upload | Import CSV streaming |
| `GET` | `/api/benchmark/blocking-vs-reactive` | JSON | Comparação de performance |

### SSE: Mudanças em Tempo Real

```
GET /api/reactive/persons/changes

→ HTTP 200
Content-Type: text/event-stream

:heartbeat
data: {"type":"HEARTBEAT","timestamp":"2026-03-04T10:30:00Z"}

id: 1
event: PERSON_CREATED
data: {"id":1,"name":"Wesley","email":"wesley@test.com","timestamp":"2026-03-04T10:30:05Z"}

id: 2
event: PERSON_UPDATED
data: {"id":1,"name":"Wesley Santos","timestamp":"2026-03-04T10:30:10Z"}

id: 3
event: PERSON_DELETED
data: {"id":1,"timestamp":"2026-03-04T10:30:15Z"}

:heartbeat
data: {"type":"HEARTBEAT","timestamp":"2026-03-04T10:30:30Z"}
```

### Backpressure: Export 10k+ Records

```
GET /api/reactive/persons/export
Accept: application/x-ndjson

→ Streams records one-by-one (no loading all into memory)
→ Honors backpressure from slow clients
→ Memory usage should stay constant regardless of total records

{"id":1,"name":"Person 1","email":"p1@test.com"}
{"id":2,"name":"Person 2","email":"p2@test.com"}
...
{"id":10000,"name":"Person 10000","email":"p10000@test.com"}
```

---

## ✅ Critérios de Aceite

- [ ] API reactive com R2DBC (non-blocking database access)
- [ ] SSE endpoint emitindo events em tempo real
- [ ] SSE com reconexão (`Last-Event-ID` support)
- [ ] Heartbeat a cada 15 segundos
- [ ] Export streaming com backpressure (memória constante)
- [ ] Import CSV streaming (sem carregar tudo em memória)
- [ ] Benchmark: Blocking vs Reactive (latência, throughput, memória)
- [ ] Testes com `StepVerifier` (Java) / channel assertions (Go)
- [ ] SSE client de teste (HTML + JavaScript EventSource)

---

## 🛠️ Implementar em

| Stack | Reactive | SSE | DB |
|---|---|---|---|
| **Spring Boot** | WebFlux + Reactor (Mono/Flux) | `Flux<ServerSentEvent>` | R2DBC |
| **Micronaut** | Reactor ou RxJava3 | `@Produces(TEXT_EVENT_STREAM)` | R2DBC |
| **Quarkus** | Mutiny (Uni/Multi) + RESTEasy Reactive | `@RestStreamElementType` + Multi | Hibernate Reactive + Vert.x |
| **Go** | Goroutines + Channels (native) | `http.Flusher` | `pgx` (native async) |

---

## 💡 Dicas

### Spring Boot — WebFlux + SSE
```java
@RestController
@RequestMapping("/api/reactive/persons")
public class PersonReactiveController {
    private final PersonReactiveRepository repository;
    private final Sinks.Many<PersonEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    // SSE: stream de mudanças em tempo real
    @GetMapping(value = "/changes", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PersonEvent>> streamChanges(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        Flux<ServerSentEvent<PersonEvent>> events = sink.asFlux()
            .map(event -> ServerSentEvent.<PersonEvent>builder()
                .id(String.valueOf(event.id()))
                .event(event.type())
                .data(event)
                .build());

        Flux<ServerSentEvent<PersonEvent>> heartbeat = Flux.interval(Duration.ofSeconds(15))
            .map(i -> ServerSentEvent.<PersonEvent>builder()
                .comment("heartbeat")
                .build());

        return Flux.merge(events, heartbeat);
    }

    // Export com backpressure
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Person> exportAll() {
        return repository.findAll(); // R2DBC streams from DB
    }

    // Notificar mudança
    public void notifyChange(PersonEvent event) {
        sink.tryEmitNext(event);
    }
}
```

### Quarkus — Mutiny + SSE
```java
@Path("/api/reactive/persons")
public class PersonReactiveResource {

    @GET
    @Path("/changes")
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<PersonEvent> streamChanges() {
        Multi<PersonEvent> events = personEventBus.toMulti();
        Multi<PersonEvent> heartbeat = Multi.createFrom()
            .ticks().every(Duration.ofSeconds(15))
            .map(tick -> PersonEvent.heartbeat());

        return Multi.createBy().merging().streams(events, heartbeat);
    }

    @GET
    @Path("/export")
    @Produces("application/x-ndjson")
    public Multi<Person> export() {
        return personRepository.streamAll(); // Hibernate Reactive
    }
}
```

### Go — SSE + Goroutines
```go
func (h *Handler) StreamChanges(c *gin.Context) {
    c.Header("Content-Type", "text/event-stream")
    c.Header("Cache-Control", "no-cache")
    c.Header("Connection", "keep-alive")

    flusher, ok := c.Writer.(http.Flusher)
    if !ok {
        c.AbortWithStatus(500)
        return
    }

    // Subscribe to events
    eventCh := h.eventBus.Subscribe()
    defer h.eventBus.Unsubscribe(eventCh)

    heartbeat := time.NewTicker(15 * time.Second)
    defer heartbeat.Stop()

    for {
        select {
        case event := <-eventCh:
            data, _ := json.Marshal(event)
            fmt.Fprintf(c.Writer, "id: %d\nevent: %s\ndata: %s\n\n",
                event.ID, event.Type, data)
            flusher.Flush()

        case <-heartbeat.C:
            fmt.Fprintf(c.Writer, ":heartbeat\ndata: {\"type\":\"HEARTBEAT\"}\n\n")
            flusher.Flush()

        case <-c.Request.Context().Done():
            return
        }
    }
}

// Event Bus com channels
type EventBus struct {
    mu          sync.RWMutex
    subscribers map[chan PersonEvent]struct{}
}

func (eb *EventBus) Publish(event PersonEvent) {
    eb.mu.RLock()
    defer eb.mu.RUnlock()
    for ch := range eb.subscribers {
        select {
        case ch <- event:
        default: // drop if subscriber is slow (backpressure)
        }
    }
}
```

---

## 📊 Benchmark Esperado

| Cenário | Blocking | Reactive/Channels |
|---|---|---|
| 1000 concurrent requests | ~150 threads | ~4 event loop threads |
| Memory (10k connections) | ~2GB (thread stacks) | ~200MB |
| Export 100k records | Loads all in memory | Streams (constant memory) |
| SSE 500 clients | 500 threads | 1 goroutine/subscriber |

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

  # Seed data for export tests
  db-seed:
    image: postgres:17-alpine
    depends_on:
      - postgres
    entrypoint: >
      sh -c "sleep 5 && PGPASSWORD=admin psql -h postgres -U admin -d persondb -c \"
        INSERT INTO persons (name, email, birth_date)
        SELECT 'Person ' || i, 'person' || i || '@test.com', '1990-01-01'::date + (i || ' days')::interval
        FROM generate_series(1, 10000) AS i
        ON CONFLICT DO NOTHING;\""
```

---

## 📦 Dependências

| Stack | Dependência |
|---|---|
| Spring | `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `r2dbc-postgresql` |
| Micronaut | `micronaut-reactor`, `micronaut-data-r2dbc`, `r2dbc-postgresql` |
| Quarkus | `quarkus-resteasy-reactive`, `quarkus-hibernate-reactive-panache`, `quarkus-reactive-pg-client` |
| Go | Standard library (`net/http`, `encoding/json`), `github.com/jackc/pgx/v5` |

---

## 🔗 Referências

- [Project Reactor Reference](https://projectreactor.io/docs/core/release/reference/)
- [SmallRye Mutiny](https://smallrye.io/smallrye-mutiny/)
- [R2DBC](https://r2dbc.io/)
- [SSE Specification (W3C)](https://html.spec.whatwg.org/multipage/server-sent-events.html)
- [Go SSE Example](https://github.com/r3labs/sse)
