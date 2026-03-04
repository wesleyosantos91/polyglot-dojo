# 🏆 Desafio 29 — Contract Testing com Pact & OpenAPI

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** Testing · Contract Testing · Pact · OpenAPI · Consumer-Driven
> **Estimativa:** 6–8 horas por stack

---

## 📋 Descrição

Implementar **Contract Testing** para garantir compatibilidade entre a API Person (provider) e seus consumidores (consumers). Usar **Pact** para testes consumer-driven e **OpenAPI/Swagger** para documentação e validação de contrato. Prevenir breaking changes antes do deploy.

---

## 🎯 Objetivos de Aprendizado

- [ ] Consumer-Driven Contract Testing (CDC)
- [ ] Pact — Consumer test (expectativas do consumer)
- [ ] Pact — Provider verification (valida contra os contratos)
- [ ] Pact Broker — repositório central de contratos
- [ ] OpenAPI 3.1 specification — documentação automática
- [ ] OpenAPI validation — requests/responses vs spec
- [ ] Schema evolution — versionamento de API
- [ ] Breaking change detection automatizada
- [ ] Contract testing no CI/CD pipeline

---

## 📐 Especificação

### Cenário

```
┌─────────────┐     Contract      ┌─────────────────┐
│ BFF Frontend│ ──────────────── │ API Person       │
│ (Consumer)  │     (Pact)        │ (Provider)       │
└─────────────┘                   └─────────────────┘
                                         │
┌─────────────┐     Contract      ┌──────┘
│ Score Service│ ──────────────── │
│ (Consumer)  │     (Pact)        │
└─────────────┘                   │

                     ┌────────────┘
                     ▼
              ┌─────────────┐
              │ Pact Broker  │
              │ (Docker)     │
              └─────────────┘
```

### Consumer 1: BFF Frontend

**Expectations:**

```json
// GET /api/persons/{id}
{
  "id": 1,
  "name": "Wesley Santos",      // ← Consumer espera estes campos
  "email": "wesley@example.com",
  "age": 35                      // ← Campo calculado que consumer precisa
}

// GET /api/persons (lista)
[
  {
    "id": 1,
    "name": "Wesley Santos",
    "email": "wesley@example.com"
  }
]

// POST /api/persons
// Request
{ "name": "Wesley", "email": "wesley@example.com", "birth_date": "1991-01-15" }
// Response: 201
{ "id": 1, "name": "Wesley", "email": "wesley@example.com" }
```

### Consumer 2: Score Service

**Expectations (subset menor):**

```json
// GET /api/persons/{id}
{
  "id": 1,
  "name": "Wesley Santos",
  "email": "wesley@example.com"
  // Score service NÃO usa "age" — não está no contrato
}
```

### OpenAPI Spec

```yaml
openapi: 3.1.0
info:
  title: Person API
  version: 1.0.0
paths:
  /api/persons:
    get:
      summary: List all persons
      parameters:
        - name: page
          in: query
          schema: { type: integer, default: 0 }
        - name: size
          in: query
          schema: { type: integer, default: 20 }
      responses:
        '200':
          content:
            application/json:
              schema:
                type: array
                items: { $ref: '#/components/schemas/Person' }
    post:
      summary: Create a person
      requestBody:
        content:
          application/json:
            schema: { $ref: '#/components/schemas/CreatePersonRequest' }
      responses:
        '201':
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Person' }
        '400':
          content:
            application/json:
              schema: { $ref: '#/components/schemas/ErrorResponse' }
  /api/persons/{id}:
    get:
      responses:
        '200':
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Person' }
        '404':
          content:
            application/json:
              schema: { $ref: '#/components/schemas/ErrorResponse' }

components:
  schemas:
    Person:
      type: object
      required: [id, name, email]
      properties:
        id: { type: integer, format: int64 }
        name: { type: string, minLength: 2, maxLength: 100 }
        email: { type: string, format: email }
        birth_date: { type: string, format: date }
        age: { type: integer }
        created_at: { type: string, format: date-time }
    CreatePersonRequest:
      type: object
      required: [name, email]
      properties:
        name: { type: string, minLength: 2 }
        email: { type: string, format: email }
        birth_date: { type: string, format: date }
    ErrorResponse:
      type: object
      properties:
        message: { type: string }
        errors: { type: array, items: { type: string } }
```

### Breaking Change Scenarios

| Mudança | Breaking? | Motivo |
|---|---|---|
| Adicionar campo opcional | ✅ Safe | Consumers ignoram campos extras |
| Remover campo `name` | ❌ Breaking | BFF e Score usam `name` |
| Renomear `email` → `emailAddress` | ❌ Breaking | Consumers esperam `email` |
| Mudar `id` de int → string | ❌ Breaking | Tipo incompatível |
| Adicionar campo obrigatório no request | ❌ Breaking | Consumers existentes não enviam |
| Deprecar endpoint (sem remover) | ✅ Safe | Manter backward compatibility |

---

## ✅ Critérios de Aceite

- [ ] Consumer test (BFF) gerando contrato Pact
- [ ] Consumer test (Score Service) gerando contrato Pact
- [ ] Provider verification contra ambos contratos
- [ ] Pact Broker rodando no Docker
- [ ] Contratos publicados no Broker
- [ ] OpenAPI spec gerada automaticamente
- [ ] Swagger UI acessível em `/swagger-ui`
- [ ] Request/Response validation contra OpenAPI spec
- [ ] Teste de breaking change detection
- [ ] Pipeline CI: consumer tests → publish → provider verify
- [ ] `can-i-deploy` check antes do deploy

---

## 🛠️ Implementar em

| Stack | Pact | OpenAPI |
|---|---|---|
| **Spring Boot** | `au.com.dius.pact:consumer/provider` | `springdoc-openapi-starter-webmvc-ui` |
| **Micronaut** | `au.com.dius.pact:consumer/provider` | `micronaut-openapi` + Swagger UI |
| **Quarkus** | `au.com.dius.pact:consumer/provider` | `quarkus-smallrye-openapi` + Swagger UI |
| **Go** | `github.com/pact-foundation/pact-go` | `github.com/swaggo/gin-swagger` |

---

## 💡 Dicas

### Consumer Test (qualquer stack Java)
```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "person-api")
class BffConsumerPactTest {

    @Pact(consumer = "bff-frontend")
    V4Pact personByIdPact(PactDslWithProvider builder) {
        return builder
            .given("person with id 1 exists")
            .uponReceiving("get person by id")
            .path("/api/persons/1")
            .method("GET")
            .willRespondWith()
            .status(200)
            .body(newJsonBody(body -> {
                body.integerType("id", 1);
                body.stringType("name", "Wesley Santos");
                body.stringType("email", "wesley@example.com");
                body.integerType("age", 35);
            }).build())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "personByIdPact")
    void shouldGetPersonById(MockServer mockServer) {
        var client = new PersonApiClient(mockServer.getUrl());
        var person = client.getPersonById(1L);

        assertThat(person.name()).isEqualTo("Wesley Santos");
        assertThat(person.age()).isEqualTo(35);
    }
}
```

### Provider Verification (Spring Boot)
```java
@Provider("person-api")
@PactBroker(url = "http://localhost:9292")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class PersonProviderPactTest {

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("person with id 1 exists")
    void personExists() {
        personRepository.save(new Person(1L, "Wesley Santos", "wesley@example.com",
            LocalDate.of(1991, 1, 15)));
    }
}
```

### Go — Pact Consumer
```go
func TestBFFConsumer(t *testing.T) {
    mockProvider, err := consumer.NewV4Pact(consumer.MockHTTPProviderConfig{
        Consumer: "bff-frontend",
        Provider: "person-api",
    })
    require.NoError(t, err)

    err = mockProvider.
        AddInteraction().
        Given("person with id 1 exists").
        UponReceiving("get person by id").
        WithCompleteRequest(consumer.Request{
            Method: "GET",
            Path:   matchers.String("/api/persons/1"),
        }).
        WithCompleteResponse(consumer.Response{
            Status: 200,
            Body: matchers.Map{
                "id":    matchers.Integer(1),
                "name":  matchers.String("Wesley Santos"),
                "email": matchers.String("wesley@example.com"),
            },
        }).
        ExecuteTest(t, func(config consumer.MockServerConfig) error {
            client := NewPersonClient(config.URL)
            person, err := client.GetByID(context.Background(), 1)
            assert.NoError(t, err)
            assert.Equal(t, "Wesley Santos", person.Name)
            return nil
        })
    assert.NoError(t, err)
}
```

### Go — OpenAPI with Swagger
```go
// @title Person API
// @version 1.0
// @description API for managing persons
// @BasePath /api

// @Summary Get person by ID
// @Tags persons
// @Accept json
// @Produce json
// @Param id path int true "Person ID"
// @Success 200 {object} PersonResponse
// @Failure 404 {object} ErrorResponse
// @Router /persons/{id} [get]
func (h *PersonHandler) GetByID(c *gin.Context) { ... }
```

---

## 🐳 Docker Compose

```yaml
services:
  pact-broker:
    image: pactfoundation/pact-broker:latest
    ports:
      - "9292:9292"
    environment:
      PACT_BROKER_DATABASE_URL: postgres://postgres:postgres@pact-db/pact_broker
      PACT_BROKER_BASIC_AUTH_USERNAME: pact
      PACT_BROKER_BASIC_AUTH_PASSWORD: pact
    depends_on:
      - pact-db

  pact-db:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: pact_broker
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres

  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: persondb
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin
    ports:
      - "5432:5432"
```

---

## 📦 Dependências

| Stack | Dependência |
|---|---|
| Spring | `au.com.dius.pact.consumer:junit5`, `au.com.dius.pact.provider:spring`, `springdoc-openapi` |
| Micronaut | `au.com.dius.pact.consumer:junit5`, `au.com.dius.pact.provider:junit5`, `micronaut-openapi` |
| Quarkus | `au.com.dius.pact.consumer:junit5`, `au.com.dius.pact.provider:junit5`, `quarkus-smallrye-openapi` |
| Go | `github.com/pact-foundation/pact-go/v2`, `github.com/swaggo/gin-swagger` |

---

## 🔗 Referências

- [Pact Documentation](https://docs.pact.io/)
- [Consumer-Driven Contract Testing](https://martinfowler.com/articles/consumerDrivenContracts.html)
- [OpenAPI 3.1 Specification](https://spec.openapis.org/oas/latest.html)
- [Pact Go](https://github.com/pact-foundation/pact-go)
- [Springdoc OpenAPI](https://springdoc.org/)
