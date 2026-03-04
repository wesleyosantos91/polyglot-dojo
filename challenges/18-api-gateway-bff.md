# 🏆 Desafio 19 — API Gateway & BFF (Backend for Frontend)

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** API Gateway · Rate Limiting · Aggregation · BFF Pattern
> **Estimativa:** 8–10 horas por stack

---

## 📋 Descrição

Criar um **API Gateway** que fica na frente dos microserviços Person (Spring, Micronaut, Quarkus, Go) e implementa cross-cutting concerns: rate limiting, autenticação, logging, request routing e agregação de dados de múltiplos serviços.

---

## 🎯 Objetivos de Aprendizado

- [ ] API Gateway pattern
- [ ] BFF — Backend for Frontend
- [ ] Rate limiting (token bucket / sliding window)
- [ ] Request routing (path-based / header-based)
- [ ] Response aggregation (chamada paralela a múltiplos serviços)
- [ ] Circuit breaker no gateway
- [ ] Request/Response transformation
- [ ] API versioning (v1, v2)
- [ ] Centralized logging e correlation ID

---

## 📐 Especificação

### Arquitetura

```
             ┌──────────────────────┐
   Client ──▶│    API Gateway       │
             │  (rate limit, auth,  │
             │   logging, routing)  │
             └──────────┬───────────┘
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Person API   │ │ Address API  │ │ Notification │
│ (any stack)  │ │ (enrichment) │ │   Service    │
└──────────────┘ └──────────────┘ └──────────────┘
```

### Gateway Routes

| Route Pattern | Backend | Descrição |
|---|---|---|
| `/api/v1/persons/**` | `person-service:8080` | CRUD de Person |
| `/api/v1/addresses/**` | `address-service:8081` | Endereços |
| `/api/v1/notifications/**` | `notification-service:8082` | Notificações |
| `/api/v1/aggregate/person-full/{id}` | Múltiplos serviços | Agregação |

### Rate Limiting

| Tier | Requests/min | Burst |
|---|---|---|
| Anonymous | 30 | 10 |
| Authenticated | 100 | 30 |
| Premium | 500 | 100 |

### Agregação (BFF endpoint)

`GET /api/v1/aggregate/person-full/{id}`

Chama em paralelo:
1. `GET /api/persons/{id}` → Person
2. `GET /api/persons/{id}/address` → Address
3. `GET /api/notifications/{id}` → Notifications

Response agregado:
```json
{
  "person": { "id": 1, "name": "Wesley", "email": "..." },
  "address": { "street": "...", "city": "São Paulo" },
  "notifications": { "unread": 3, "total": 15 },
  "response_time_ms": {
    "person_service": 12,
    "address_service": 8,
    "notification_service": 15,
    "total": 18
  }
}
```

### Headers adicionados pelo Gateway

| Header | Descrição |
|---|---|
| `X-Request-Id` | UUID único por request |
| `X-Correlation-Id` | Propagado para backend services |
| `X-Rate-Limit-Remaining` | Requests restantes |
| `X-Rate-Limit-Reset` | Timestamp do reset |
| `X-Response-Time` | Tempo total em ms |

### Endpoints do Gateway

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/gateway/health` | Health de todos os backends |
| `GET` | `/api/gateway/routes` | Lista de rotas configuradas |
| `GET` | `/api/gateway/metrics` | Métricas (req/s, latency, errors) |

---

## ✅ Critérios de Aceite

- [ ] Roteamento para múltiplos backends
- [ ] Rate limiting por IP/user
- [ ] Headers de rate limit na response
- [ ] Aggregation endpoint chamando 3 serviços em paralelo
- [ ] Correlation ID propagado end-to-end
- [ ] Circuit breaker para cada backend
- [ ] Health check agregado de todos os backends
- [ ] Request/response logging centralizado
- [ ] API versioning (v1/v2)

---

## 🛠️ Implementar em

| Stack | Abordagem |
|---|---|
| **Spring Boot** | Spring Cloud Gateway (reactive) |
| **Micronaut** | Custom Reverse Proxy (HttpClient) |
| **Quarkus** | Custom gateway + Vert.x HTTP Proxy |
| **Go** | `net/http/httputil.ReverseProxy` + middleware chain |

---

## 💡 Dicas

### Spring Cloud Gateway
```java
@Bean
public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("persons", r -> r
            .path("/api/v1/persons/**")
            .filters(f -> f
                .addRequestHeader("X-Correlation-Id", UUID.randomUUID().toString())
                .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter())))
            .uri("http://person-service:8080"))
        .build();
}
```

### Go
```go
func main() {
    proxy := httputil.NewSingleHostReverseProxy(targetURL)
    
    handler := rateLimiter(
        correlationId(
            circuitBreaker(
                logging(proxy))))
    
    http.ListenAndServe(":8000", handler)
}
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `spring-cloud-starter-gateway` + `spring-boot-starter-data-redis-reactive` |
| Micronaut | `micronaut-http-client` (custom proxy) |
| Quarkus | `io.vertx:vertx-web-proxy` |
| Go | `net/http/httputil` (stdlib) + `golang.org/x/time/rate` |

---

## 🔗 Referências

- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)
- [BFF Pattern](https://samnewman.io/patterns/architectural/bff/)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Rate Limiting Algorithms](https://blog.bytebytego.com/p/rate-limiting-fundamentals)
