# 🏆 Desafio 16 — Cache com Redis

> **Nível:** ⭐⭐ Intermediário
> **Tipo:** Cache · Redis · Performance · TTL · Invalidation
> **Estimativa:** 5–7 horas por stack

---

## 📋 Descrição

Adicionar **cache distribuído com Redis** à API Person para melhorar performance. Implementar cache-aside pattern, TTL, invalidação e métricas de hit/miss.

---

## 🎯 Objetivos de Aprendizado

- [ ] Cache-aside pattern (lazy loading)
- [ ] Write-through vs Write-behind
- [ ] TTL (Time to Live) e eviction
- [ ] Cache invalidation strategies
- [ ] Cache key design
- [ ] Redis data structures (String, Hash, Sorted Set)
- [ ] Cache metrics (hit rate, miss rate, latency)
- [ ] Serialização para cache (JSON vs binário)

---

## 📐 Especificação

### Cache Strategy

| Operação | Cache Action |
|---|---|
| `GET /persons/{id}` | Cache-aside: busca no cache → se miss, busca no DB → popula cache |
| `GET /persons` | Cache com TTL curto (30s) — lista muda frequentemente |
| `POST /persons` | Invalida cache da lista |
| `PUT /persons/{id}` | Atualiza cache do item + invalida lista |
| `DELETE /persons/{id}` | Remove cache do item + invalida lista |

### Cache Keys

| Key Pattern | TTL | Dados |
|---|---|---|
| `person:{id}` | 5 minutos | JSON da person |
| `persons:list:page:{page}:size:{size}` | 30 segundos | Lista paginada |
| `persons:count` | 1 minuto | Total de registros |

### Endpoints de Monitoramento

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/cache/stats` | Hit/miss ratio, keys count |
| `DELETE` | `/api/cache/flush` | Limpa todo o cache |
| `DELETE` | `/api/cache/persons/{id}` | Invalida cache específico |

### Response de Stats

```json
{
  "total_keys": 150,
  "hit_count": 8500,
  "miss_count": 1200,
  "hit_rate": 0.876,
  "avg_latency_ms": 0.8,
  "memory_usage_bytes": 524288,
  "evictions": 45
}
```

---

## ✅ Critérios de Aceite

- [ ] Cache-aside funcionando para GET by ID
- [ ] Cache da lista com TTL de 30s
- [ ] Invalidação correta no PUT/DELETE
- [ ] Métricas de hit/miss expostas
- [ ] Redis rodando via Docker Compose
- [ ] Performance: GET by ID < 5ms (cache hit)
- [ ] Teste de integração com Redis (Testcontainers)
- [ ] Graceful degradation (app funciona sem Redis)

---

## 🐳 Docker Compose

```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --maxmemory 100mb --maxmemory-policy allkeys-lru
```

---

## 🛠️ Implementar em

| Stack | Abordagem |
|---|---|
| **Spring Boot** | `@Cacheable`, `@CacheEvict`, `@CachePut` + Spring Data Redis |
| **Micronaut** | `@Cacheable`, `@CacheInvalidate` + Micronaut Redis |
| **Quarkus** | `@CacheResult`, `@CacheInvalidate` + Quarkus Redis |
| **Go** | `go-redis/redis` + custom middleware |

---

## 💡 Dicas

### Spring Boot
```java
@Cacheable(value = "persons", key = "#id")
public Person findById(Long id) {
    return personRepository.findById(id).orElseThrow();
}

@CacheEvict(value = "persons", key = "#id")
@CachePut(value = "persons", key = "#id")
public Person update(Long id, PersonRequest request) {
    // atualizar no banco e retornar
}
```

### Go
```go
func (h *PersonHandler) FindByID(c *gin.Context) {
    id := c.Param("id")
    
    // Try cache
    cached, err := h.redis.Get(ctx, "person:"+id).Result()
    if err == nil {
        c.Data(200, "application/json", []byte(cached))
        return
    }
    
    // Cache miss → DB
    person, _ := h.repo.FindByID(id)
    data, _ := json.Marshal(person)
    h.redis.Set(ctx, "person:"+id, data, 5*time.Minute)
    c.JSON(200, person)
}
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `spring-boot-starter-data-redis` + `spring-boot-starter-cache` |
| Micronaut | `micronaut-redis-lettuce` + `micronaut-cache-core` |
| Quarkus | `quarkus-redis-client` + `quarkus-cache` |
| Go | `github.com/redis/go-redis/v9` |

---

## 🔗 Referências

- [Redis Documentation](https://redis.io/docs/)
- [Cache-Aside Pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/cache-aside)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
