# 🏆 Desafio 22 — Integration Testing com Testcontainers & Docker

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** Testing · Testcontainers · Docker · Integration · E2E
> **Estimativa:** 8–10 horas por stack

---

## 📋 Descrição

Implementar uma **suíte completa de testes de integração** para a API Person usando **Testcontainers** (Java/Go) para subir containers reais de PostgreSQL, Kafka, Redis e LocalStack durante os testes. Cobrir desde testes de repositório até testes E2E completos.

---

## 🎯 Objetivos de Aprendizado

- [ ] Testcontainers — lifecycle (start/stop containers)
- [ ] Testcontainers — reuse containers entre testes (`reuse = true`)
- [ ] Testcontainers — custom containers (Dockerfile)
- [ ] Testcontainers — compose module (subir docker-compose inteiro)
- [ ] Pirâmide de testes: unidade → integração → E2E
- [ ] Testes de Repository com banco real (PostgreSQL)
- [ ] Testes de mensageria (Kafka container)
- [ ] Testes com AWS (LocalStack container)
- [ ] Testes de API completos (HTTP client → DB → response)
- [ ] Test Fixtures e Data Builders
- [ ] Database migration durante testes (Flyway/Liquibase)
- [ ] Paralelização de testes
- [ ] Code coverage com análise e metas

---

## 📐 Especificação

### Estrutura de Testes

```
src/test/
├── unit/                    # Sem I/O, sem container
│   ├── PersonValidatorTest
│   └── PersonMapperTest
├── integration/             # Testcontainers
│   ├── repository/
│   │   ├── PersonRepositoryTest      ← PostgreSQL container
│   │   └── PersonSearchRepositoryTest ← Elasticsearch container
│   ├── messaging/
│   │   ├── KafkaProducerTest         ← Kafka container
│   │   └── KafkaConsumerTest         ← Kafka + PostgreSQL
│   ├── cache/
│   │   └── PersonCacheTest           ← Redis container
│   └── aws/
│       └── S3StorageTest             ← LocalStack container
├── e2e/                     # Full API
│   ├── PersonApiTest                 ← App + PostgreSQL + Kafka
│   └── HealthCheckTest              ← App + todas dependências
└── support/
    ├── TestContainerConfig          ← Container singletons
    ├── PersonFixtures               ← Data builders
    └── DatabaseCleaner              ← Limpa DB entre testes
```

### Containers Necessários

| Container | Imagem | Porta | Usado em |
|---|---|---|---|
| PostgreSQL | `postgres:17-alpine` | 5432 | Repository, E2E |
| Kafka | `apache/kafka:3.9.0` | 9092 | Messaging |
| Redis | `redis:7-alpine` | 6379 | Cache |
| LocalStack | `localstack/localstack:4` | 4566 | S3, SQS |
| Elasticsearch | `elasticsearch:8.17.0` | 9200 | Search |

### Padrões de Teste

#### 1. Repository Test (com PostgreSQL real)
```
Given: Banco PostgreSQL com schema migrado
When: personRepository.save(new Person("Wesley", "wesley@test.com"))
Then: personRepository.findByEmail("wesley@test.com") retorna o person
  And: person.getId() não é null
  And: person.getCreatedAt() não é null
```

#### 2. Kafka Integration Test
```
Given: Kafka rodando, consumer escutando "person-events"
When: producer.publish(PersonCreated event)
Then: Consumer recebe o evento em até 5 segundos
  And: View materializada contém o person
```

#### 3. E2E API Test
```
Given: App rodando com PostgreSQL
When: POST /api/persons {"name": "Wesley", "email": "wesley@test.com"}
Then: Status 201
  And: Body contém id, name, email, created_at
  And: GET /api/persons/{id} retorna o person
  And: GET /api/persons retorna lista com 1 item
```

#### 4. Negative/Edge Cases
```
Given: App rodando
When: POST /api/persons {"name": "", "email": "invalid"}
Then: Status 400
  And: Body contém validation errors

When: GET /api/persons/99999
Then: Status 404

When: POST /api/persons (email duplicado)
Then: Status 409
```

---

## ✅ Critérios de Aceite

- [ ] PostgreSQL container para testes de repositório
- [ ] Kafka container para testes de producer/consumer
- [ ] Redis container para testes de cache
- [ ] LocalStack container para testes AWS
- [ ] Container reuse entre testes (startup rápido)
- [ ] Database limpo entre cada teste (`@Transactional` rollback ou truncate)
- [ ] Migrations executam automaticamente durante os testes
- [ ] Teste E2E cobrindo happy path completo (CRUD)
- [ ] Teste cobrindo cenários de erro (validação, 404, 409)
- [ ] Fixtures/Builders para criação de dados
- [ ] Code coverage ≥ 80% (linhas e branches)
- [ ] Testes rodam em CI (GitHub Actions com Docker-in-Docker)
- [ ] Tempo total < 2 minutos

---

## 🛠️ Implementar em

| Stack | Framework | Containers |
|---|---|---|
| **Spring Boot** | JUnit 5 + `@SpringBootTest` + `@Testcontainers` | `PostgreSQLContainer`, `KafkaContainer` |
| **Micronaut** | JUnit 5 + `@MicronautTest` + Testcontainers | `PostgreSQLContainer` |
| **Quarkus** | JUnit 5 + `@QuarkusTest` + DevServices (auto-containers!) | DevServices (automático) |
| **Go** | `testing` + `testcontainers-go` + `httptest` | `postgres`, `kafka`, `redis` modules |

---

## 💡 Dicas

### Spring Boot — Singleton Container Pattern
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withReuse(true);

    static final KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("apache/kafka:3.9.0"))
        .withReuse(true);

    @BeforeAll
    static void startContainers() {
        Startables.deepStart(postgres, kafka).join();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

### Spring Boot — Test
```java
@Nested
class CreatePerson extends IntegrationTestBase {
    @Test
    void shouldCreateAndRetrievePerson() {
        var request = new CreatePersonRequest("Wesley", "wesley@test.com", "1991-01-15");

        var response = restTemplate.postForEntity("/api/persons", request, PersonResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().name()).isEqualTo("Wesley");

        var found = restTemplate.getForEntity("/api/persons/" + response.getBody().id(), PersonResponse.class);
        assertThat(found.getBody().email()).isEqualTo("wesley@test.com");
    }
}
```

### Quarkus — DevServices (zero config!)
```java
// Quarkus starta PostgreSQL automaticamente para testes!
// Apenas configure application.properties:
// %test.quarkus.datasource.devservices.enabled=true

@QuarkusTest
class PersonResourceTest {
    @Test
    void shouldCreatePerson() {
        given()
            .contentType(JSON)
            .body("""
                {"name": "Wesley", "email": "wesley@test.com"}
                """)
            .when().post("/api/persons")
            .then()
                .statusCode(201)
                .body("name", equalTo("Wesley"));
    }
}
```

### Go — testcontainers-go
```go
func TestPersonRepository_Integration(t *testing.T) {
    ctx := context.Background()

    pgContainer, err := postgres.Run(ctx, "postgres:17-alpine",
        postgres.WithDatabase("testdb"),
        postgres.WithUsername("test"),
        postgres.WithPassword("test"),
        testcontainers.WithWaitStrategy(
            wait.ForLog("database system is ready to accept connections").
                WithOccurrence(2).WithStartupTimeout(30*time.Second)),
    )
    require.NoError(t, err)
    defer pgContainer.Terminate(ctx)

    connStr, _ := pgContainer.ConnectionString(ctx, "sslmode=disable")
    db, _ := gorm.Open(gormPostgres.Open(connStr), &gorm.Config{})
    db.AutoMigrate(&model.Person{})

    repo := repository.NewPersonRepository(db)

    t.Run("should save and find person", func(t *testing.T) {
        person := &model.Person{Name: "Wesley", Email: "wesley@test.com"}
        err := repo.Create(person)
        assert.NoError(t, err)
        assert.NotZero(t, person.ID)

        found, err := repo.FindByID(person.ID)
        assert.NoError(t, err)
        assert.Equal(t, "Wesley", found.Name)
    })
}
```

---

## 📊 Meta de Coverage

| Camada | Meta | Tipo de Teste |
|---|---|---|
| Model / Validation | 95% | Unit |
| Repository | 90% | Integration (Testcontainers) |
| Service | 85% | Unit + Integration |
| Handler / Controller | 80% | E2E |
| **Total** | **≥ 80%** | Misto |

---

## 📦 Dependências

| Stack | Dependência |
|---|---|
| Spring | `spring-boot-testcontainers`, `org.testcontainers:postgresql`, `org.testcontainers:kafka` |
| Micronaut | `micronaut-test-junit5`, `org.testcontainers:postgresql` |
| Quarkus | `quarkus-junit5`, DevServices (built-in), `rest-assured` |
| Go | `github.com/testcontainers/testcontainers-go`, `github.com/stretchr/testify` |

---

## 🔗 Referências

- [Testcontainers Java](https://java.testcontainers.org/)
- [Testcontainers Go](https://golang.testcontainers.org/)
- [Quarkus Dev Services](https://quarkus.io/guides/dev-services)
- [Spring Boot Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
