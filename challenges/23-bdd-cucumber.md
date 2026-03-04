# 🏆 Desafio 23 — BDD com Cucumber / Gherkin / Godog

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** Testing · BDD · Cucumber · Gherkin · Acceptance Tests
> **Estimativa:** 6–8 horas por stack

---

## 📋 Descrição

Implementar **Behavior-Driven Development (BDD)** na API Person usando **Gherkin** para especificação em linguagem natural e **Cucumber** (Java) / **Godog** (Go) para execução. Os cenários servem como documentação viva e testes de aceitação.

---

## 🎯 Objetivos de Aprendizado

- [ ] Gherkin syntax (Given/When/Then, Scenario Outline, Data Tables)
- [ ] Step Definitions com binding automático
- [ ] Background (setup comum entre cenários)
- [ ] Scenario Outline com Examples (data-driven tests)
- [ ] Tags para categorização (`@smoke`, `@regression`, `@api`)
- [ ] Hooks (`@Before`, `@After`) para setup/teardown
- [ ] Integração com Testcontainers (BDD + containers reais)
- [ ] Reports HTML (Cucumber Reports)
- [ ] BDD como documentação viva do sistema

---

## 📐 Especificação

### Feature Files

#### `features/person-crud.feature`
```gherkin
@api @crud
Feature: Person CRUD Operations
  Como um usuário da API
  Eu quero gerenciar pessoas
  Para manter o cadastro atualizado

  Background:
    Given o banco de dados está limpo
    And o serviço está rodando

  @smoke @happy-path
  Scenario: Criar uma nova pessoa com sucesso
    Given eu tenho os seguintes dados de pessoa:
      | name          | email              | birth_date |
      | Wesley Santos | wesley@example.com | 1991-01-15 |
    When eu envio uma requisição POST para "/api/persons"
    Then o status da resposta deve ser 201
    And a resposta deve conter o campo "id"
    And a resposta deve conter "name" igual a "Wesley Santos"
    And a resposta deve conter "email" igual a "wesley@example.com"

  @happy-path
  Scenario: Buscar pessoa por ID
    Given existe uma pessoa cadastrada:
      | name          | email              |
      | Wesley Santos | wesley@example.com |
    When eu envio uma requisição GET para "/api/persons/{id}"
    Then o status da resposta deve ser 200
    And a resposta deve conter "name" igual a "Wesley Santos"

  @happy-path
  Scenario: Listar todas as pessoas
    Given existem as seguintes pessoas cadastradas:
      | name          | email              |
      | Wesley Santos | wesley@example.com |
      | Maria Silva   | maria@example.com  |
      | João Souza    | joao@example.com   |
    When eu envio uma requisição GET para "/api/persons"
    Then o status da resposta deve ser 200
    And a resposta deve conter 3 itens

  @happy-path
  Scenario: Atualizar pessoa existente
    Given existe uma pessoa cadastrada:
      | name          | email              |
      | Wesley Santos | wesley@example.com |
    When eu envio uma requisição PUT para "/api/persons/{id}" com:
      | name           | email                  |
      | Wesley Updated | wesley.new@example.com |
    Then o status da resposta deve ser 200
    And a resposta deve conter "name" igual a "Wesley Updated"

  @happy-path
  Scenario: Deletar pessoa existente
    Given existe uma pessoa cadastrada:
      | name          | email              |
      | Wesley Santos | wesley@example.com |
    When eu envio uma requisição DELETE para "/api/persons/{id}"
    Then o status da resposta deve ser 204
    When eu envio uma requisição GET para "/api/persons/{id}"
    Then o status da resposta deve ser 404
```

#### `features/person-validation.feature`
```gherkin
@api @validation
Feature: Person Validation
  Validações de entrada da API Person

  @negative
  Scenario Outline: Rejeitar pessoa com dados inválidos
    When eu envio uma requisição POST para "/api/persons" com:
      | name   | email   | birth_date   |
      | <name> | <email> | <birth_date> |
    Then o status da resposta deve ser 400
    And a resposta deve conter erro no campo "<field>"

    Examples:
      | name   | email          | birth_date | field      |
      |        | valid@test.com | 1991-01-15 | name       |
      | Wesley |                | 1991-01-15 | email      |
      | Wesley | invalid-email  | 1991-01-15 | email      |
      | Wesley | valid@test.com | 2030-01-01 | birth_date |
      | We     | valid@test.com | 1991-01-15 | name       |

  @negative
  Scenario: Rejeitar email duplicado
    Given existe uma pessoa cadastrada:
      | name          | email              |
      | Wesley Santos | wesley@example.com |
    When eu envio uma requisição POST para "/api/persons" com:
      | name        | email              |
      | Outro Nome  | wesley@example.com |
    Then o status da resposta deve ser 409
    And a resposta deve conter mensagem "Email já cadastrado"
```

#### `features/person-search.feature`
```gherkin
@api @search
Feature: Person Search and Pagination
  Busca e paginação da API Person

  Background:
    Given existem 25 pessoas cadastradas

  Scenario: Paginar resultados
    When eu envio uma requisição GET para "/api/persons?page=0&size=10"
    Then o status da resposta deve ser 200
    And a resposta deve conter 10 itens
    And a resposta deve conter campo "totalElements" igual a 25

  Scenario: Buscar por nome
    Given existe uma pessoa cadastrada:
      | name     | email            |
      | Específo | unico@test.com   |
    When eu envio uma requisição GET para "/api/persons?name=Específo"
    Then o status da resposta deve ser 200
    And a resposta deve conter 1 item
```

---

## ✅ Critérios de Aceite

- [ ] Mínimo 15 cenários em Gherkin
- [ ] Features organizadas por domínio (CRUD, validation, search)
- [ ] Scenario Outline com Examples (data-driven)
- [ ] Background para setup comum
- [ ] Tags para filtrar execução (`@smoke`, `@regression`)
- [ ] Step Definitions reutilizáveis entre features
- [ ] Hooks para setup/teardown (database cleanup)
- [ ] Testcontainers para PostgreSQL real nos testes
- [ ] Relatório HTML de execução
- [ ] Cenários negativos (validação, 404, 409)
- [ ] `mvn verify -Dcucumber.filter.tags="@smoke"` executa apenas smoke tests
- [ ] Features legíveis por não-desenvolvedores

---

## 🛠️ Implementar em

| Stack | Framework BDD | Runner |
|---|---|---|
| **Spring Boot** | Cucumber-Java + JUnit Platform | `@Suite` + `@SelectPackages` |
| **Micronaut** | Cucumber-Java + `@MicronautTest` | `@Suite` |
| **Quarkus** | Cucumber-Java + `@QuarkusTest` | `@Suite` |
| **Go** | Godog (`github.com/cucumber/godog`) | `TestFeatures(t *testing.T)` |

---

## 💡 Dicas

### Spring Boot — Step Definitions
```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class PersonSteps {
    @LocalServerPort int port;
    @Autowired PersonRepository repository;

    private Response response;
    private Long createdPersonId;

    @Given("o banco de dados está limpo")
    public void cleanDatabase() {
        repository.deleteAll();
    }

    @When("eu envio uma requisição POST para {string}")
    public void postRequest(String path) {
        response = given()
            .port(port)
            .contentType(JSON)
            .body(currentRequestBody)
            .post(path);
    }

    @Then("o status da resposta deve ser {int}")
    public void verifyStatus(int expectedStatus) {
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
    }

    @Then("a resposta deve conter {string} igual a {string}")
    public void verifyField(String field, String value) {
        assertThat(response.jsonPath().getString(field)).isEqualTo(value);
    }
}
```

### Go — Godog
```go
func TestFeatures(t *testing.T) {
    suite := godog.TestSuite{
        ScenarioInitializer: InitializeScenario,
        Options: &godog.Options{
            Format:   "pretty",
            Paths:    []string{"features"},
            Tags:     "@smoke",
            TestingT: t,
        },
    }
    if suite.Run() != 0 {
        t.Fatal("non-zero exit code from BDD tests")
    }
}

func InitializeScenario(ctx *godog.ScenarioContext) {
    api := &apiContext{}

    ctx.Before(func(ctx context.Context, sc *godog.Scenario) (context.Context, error) {
        api.cleanup()
        return ctx, nil
    })

    ctx.Step(`^eu envio uma requisição GET para "([^"]*)"$`, api.sendGetRequest)
    ctx.Step(`^o status da resposta deve ser (\d+)$`, api.verifyStatusCode)
    ctx.Step(`^a resposta deve conter "([^"]*)" igual a "([^"]*)"$`, api.verifyField)
}

func (a *apiContext) sendGetRequest(path string) error {
    resp, err := http.Get(a.baseURL + path)
    a.response = resp
    return err
}

func (a *apiContext) verifyStatusCode(expected int) error {
    if a.response.StatusCode != expected {
        return fmt.Errorf("expected %d, got %d", expected, a.response.StatusCode)
    }
    return nil
}
```

---

## 📁 Estrutura de Diretórios

```
project/
├── src/test/
│   ├── resources/features/       # ← Feature files (Gherkin)
│   │   ├── person-crud.feature
│   │   ├── person-validation.feature
│   │   └── person-search.feature
│   └── java/.../bdd/
│       ├── CucumberConfig.java   # ← Suite configuration
│       ├── PersonSteps.java      # ← Step definitions
│       └── CommonSteps.java      # ← Reusable steps
```

```
go-project/
├── features/                     # ← Feature files
│   ├── person-crud.feature
│   └── person-validation.feature
├── bdd_test.go                   # ← Godog setup + steps
```

---

## 📦 Dependências

| Stack | Dependência |
|---|---|
| Spring | `io.cucumber:cucumber-java`, `io.cucumber:cucumber-spring`, `io.cucumber:cucumber-junit-platform-engine` |
| Micronaut | `io.cucumber:cucumber-java`, `io.cucumber:cucumber-junit-platform-engine` |
| Quarkus | `io.cucumber:cucumber-java`, `io.cucumber:cucumber-junit-platform-engine` |
| Go | `github.com/cucumber/godog` |

---

## 🔗 Referências

- [Cucumber Documentation](https://cucumber.io/docs/)
- [Gherkin Reference](https://cucumber.io/docs/gherkin/reference/)
- [Godog - Go BDD Framework](https://github.com/cucumber/godog)
- [Cucumber-Spring Integration](https://cucumber.io/docs/installation/java/#spring)
