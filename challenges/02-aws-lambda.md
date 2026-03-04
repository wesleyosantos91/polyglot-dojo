# 🏆 Desafio 02 — AWS Lambda Function

> **Nível:** ⭐⭐ Intermediário
> **Tipo:** Serverless · AWS Lambda · API Gateway · DynamoDB
> **Estimativa:** 6–8 horas por stack

---

## 📋 Descrição

Criar uma **função Lambda** que processa eventos de criação de Person e armazena em DynamoDB. A função é invocada via API Gateway (HTTP) e também por eventos diretos (invoke).

---

## 🎯 Objetivos de Aprendizado

- [ ] Modelo serverless (cold start vs warm start)
- [ ] Handler de eventos Lambda
- [ ] Integração com API Gateway (REST)
- [ ] DynamoDB como NoSQL database
- [ ] Deploy com SAM / Serverless Framework
- [ ] SnapStart (Java) ou custom runtime (Go)
- [ ] Monitoramento com CloudWatch + X-Ray

---

## 📐 Especificação

### Função: PersonFunction

**Triggers:**
1. **API Gateway** → `POST /persons` (cria person)
2. **API Gateway** → `GET /persons/{id}` (busca por ID)
3. **Invoke direto** → Evento JSON (batch de criação)

### Evento de Entrada (invoke direto)

```json
{
  "action": "create_batch",
  "persons": [
    { "name": "Wesley", "email": "wesley@email.com", "birth_date": "1991-01-15" },
    { "name": "Maria", "email": "maria@email.com", "birth_date": "1995-06-20" }
  ]
}
```

### DynamoDB Table: Persons

| Atributo | Tipo | Chave |
|---|---|---|
| `PK` | String (UUID) | Partition Key |
| `name` | String | — |
| `email` | String | GSI (Global Secondary Index) |
| `birth_date` | String | — |
| `created_at` | String (ISO 8601) | — |

---

## ✅ Critérios de Aceite

- [ ] Lambda handler respondendo a API Gateway
- [ ] Lambda handler respondendo a invoke direto
- [ ] DynamoDB como persistência
- [ ] Cold start < 3s (Java com SnapStart) ou < 500ms (Go)
- [ ] Template SAM ou `serverless.yml` funcional
- [ ] Testes locais com SAM CLI (`sam local invoke`)
- [ ] Logs estruturados no CloudWatch

---

## 🛠️ Implementar em

| Stack | Framework Lambda | Observações |
|---|---|---|
| **Spring Boot** | Spring Cloud Function | `@Bean Function<Input, Output>` + SnapStart |
| **Micronaut** | Micronaut AWS Lambda | `@FunctionBean`, GraalVM native-image |
| **Quarkus** | Quarkus AWS Lambda | `@Named("person")`, native build |
| **Go** | `aws-lambda-go` | Handler nativo, cold start ~50ms |

---

## 💡 Dicas

### Java — Reduzir Cold Start
```
# SnapStart (Spring/Micronaut)
SnapStart:
  ApplyOn: PublishedVersions

# Native Image (Quarkus/Micronaut)
Runtime: provided.al2023
```

### Go — Handler simples
```go
func HandleRequest(ctx context.Context, event PersonEvent) (Response, error) {
    // processar evento
}

func main() {
    lambda.Start(HandleRequest)
}
```

### SAM Template (exemplo)
```yaml
Resources:
  PersonFunction:
    Type: AWS::Serverless::Function
    Properties:
      Handler: bootstrap  # ou com.example.Handler
      Runtime: provided.al2023  # ou java25
      MemorySize: 512
      Timeout: 30
      Events:
        CreatePerson:
          Type: Api
          Properties:
            Path: /persons
            Method: post
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `spring-cloud-function-adapter-aws` |
| Micronaut | `micronaut-function-aws-api-proxy` |
| Quarkus | `quarkus-amazon-lambda-rest` |
| Go | `github.com/aws/aws-lambda-go` |

---

## 🔗 Referências

- [AWS Lambda Developer Guide](https://docs.aws.amazon.com/lambda/latest/dg/)
- [SAM CLI](https://docs.aws.amazon.com/serverless-application-model/)
- [DynamoDB Developer Guide](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/)
- [Spring Cloud Function](https://spring.io/projects/spring-cloud-function)
- [Quarkus AWS Lambda](https://quarkus.io/guides/amazon-lambda)
