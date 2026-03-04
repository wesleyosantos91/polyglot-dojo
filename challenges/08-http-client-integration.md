# 🏆 Desafio 09 — HTTP Client Integration

> **Nível:** ⭐⭐ Intermediário
> **Tipo:** Integração · REST Client · Resilience · Circuit Breaker
> **Estimativa:** 6–8 horas por stack

---

## 📋 Descrição

Criar um serviço que **consome APIs externas** (ViaCEP, JSONPlaceholder, etc.) para enriquecer dados de Person com endereço e informações adicionais. Implementar padrões de resiliência: Circuit Breaker, Retry, Timeout, Fallback e Rate Limiting.

---

## 🎯 Objetivos de Aprendizado

- [ ] HTTP Client declarativo e programático
- [ ] Serialização/Desserialização de responses externos
- [ ] Circuit Breaker pattern (Open → Half-Open → Closed)
- [ ] Retry com backoff exponencial
- [ ] Timeout configuration
- [ ] Fallback (valor padrão quando serviço está fora)
- [ ] Rate Limiting (respeitar limites da API externa)
- [ ] Logging de requisições HTTP (interceptors)

---

## 📐 Especificação

### APIs Externas

| API | URL | Propósito |
|---|---|---|
| ViaCEP | `https://viacep.com.br/ws/{cep}/json/` | Busca endereço por CEP |
| JSONPlaceholder | `https://jsonplaceholder.typicode.com/users/{id}` | Dados mock de usuário |
| OpenWeather (opcional) | `https://api.openweathermap.org/data/2.5/weather` | Clima da cidade |

### Fluxo de Enriquecimento

```
POST /api/persons (com CEP)
    │
    ├── 1. Validar dados básicos
    ├── 2. Chamar ViaCEP → obter endereço
    │       ├── Sucesso → salvar endereço
    │       └── Falha → Circuit Breaker → Fallback (endereço parcial)
    ├── 3. Persistir Person + Address
    └── 4. Retornar 201 Created
```

### Request Enriquecido

```json
{
  "name": "Wesley Santos",
  "email": "wesley@example.com",
  "birth_date": "1991-01-15",
  "zip_code": "01001-000"
}
```

### Response Enriquecido

```json
{
  "id": 1,
  "name": "Wesley Santos",
  "email": "wesley@example.com",
  "birth_date": "1991-01-15",
  "address": {
    "zip_code": "01001-000",
    "street": "Praça da Sé",
    "complement": "lado ímpar",
    "neighborhood": "Sé",
    "city": "São Paulo",
    "state": "SP",
    "source": "VIACEP"
  }
}
```

### Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/persons` | Cria person com enriquecimento |
| `GET` | `/api/persons/{id}/address` | Busca endereço da person |
| `POST` | `/api/address/lookup/{cep}` | Consulta direta ao ViaCEP |
| `GET` | `/api/circuit-breaker/status` | Status do Circuit Breaker |

### Configuração do Circuit Breaker

| Parâmetro | Valor |
|---|---|
| Failure threshold | 5 falhas |
| Wait in open state | 30 segundos |
| Permitted calls in half-open | 3 |
| Timeout per request | 5 segundos |
| Retry attempts | 3 |
| Retry backoff | 1s → 2s → 4s |

---

## ✅ Critérios de Aceite

- [ ] Integração com ViaCEP funcionando
- [ ] Circuit Breaker abrindo após 5 falhas
- [ ] Fallback retornando endereço parcial (só CEP)
- [ ] Retry com backoff exponencial (3 tentativas)
- [ ] Timeout de 5s por requisição
- [ ] Status do Circuit Breaker via endpoint
- [ ] Request/Response logging (interceptor)
- [ ] Teste com WireMock/MockServer simulando falhas

---

## 🛠️ Implementar em

| Stack | HTTP Client | Resilience |
|---|---|---|
| **Spring Boot** | `RestClient` / `@HttpExchange` | Resilience4j |
| **Micronaut** | `@Client` (declarativo) | Micronaut Retry/CircuitBreaker |
| **Quarkus** | `@RegisterRestClient` | MicroProfile Fault Tolerance |
| **Go** | `net/http` / `resty` | `sony/gobreaker` |

---

## 💡 Dicas

### Spring Boot (RestClient + Resilience4j)
```java
@HttpExchange("https://viacep.com.br/ws")
public interface ViaCepClient {
    @GetExchange("/{cep}/json/")
    AddressResponse findByCep(@PathVariable String cep);
}

@CircuitBreaker(name = "viacep", fallbackMethod = "fallback")
@Retry(name = "viacep")
@TimeLimiter(name = "viacep")
public AddressResponse findAddress(String cep) {
    return viaCepClient.findByCep(cep);
}

private AddressResponse fallback(String cep, Throwable t) {
    return AddressResponse.partial(cep);
}
```

### Micronaut
```java
@Client("https://viacep.com.br/ws")
public interface ViaCepClient {
    @Get("/{cep}/json/")
    @Retryable(attempts = "3", delay = "1s", multiplier = "2")
    AddressResponse findByCep(String cep);
}
```

### Go
```go
cb := gobreaker.NewCircuitBreaker(gobreaker.Settings{
    Name:        "viacep",
    MaxRequests: 3,
    Interval:    30 * time.Second,
    Timeout:     30 * time.Second,
})

result, err := cb.Execute(func() (interface{}, error) {
    resp, err := http.Get(fmt.Sprintf("https://viacep.com.br/ws/%s/json/", cep))
    // ...
})
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `resilience4j-spring-boot3` |
| Micronaut | `micronaut-http-client` (built-in retry) |
| Quarkus | `quarkus-smallrye-fault-tolerance` |
| Go | `github.com/sony/gobreaker` + `github.com/go-resty/resty/v2` |

---

## 🔗 Referências

- [ViaCEP API](https://viacep.com.br/)
- [Resilience4j](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [MicroProfile Fault Tolerance](https://microprofile.io/project/eclipse/microprofile-fault-tolerance)
