# 🏆 Desafio 21 — AWS Cloud Native (SNS + EventBridge + Step Functions)

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** AWS · Serverless · Event-Driven · Orquestração · SNS · EventBridge
> **Estimativa:** 10–14 horas por stack

---

## 📋 Descrição

Criar um **workflow de onboarding de Person** usando serviços AWS nativos:

1. **SNS** — Notificação fan-out para múltiplos subscribers
2. **EventBridge** — Event bus para roteamento baseado em regras
3. **Step Functions** — Orquestração do workflow de onboarding
4. **Secrets Manager** — Gerenciamento seguro de credenciais
5. **Parameter Store (SSM)** — Configurações dinâmicas

Ambiente local com **LocalStack**.

---

## 🎯 Objetivos de Aprendizado

- [ ] SNS Topics com múltiplos subscribers (SQS, Lambda, HTTP)
- [ ] SNS Message Filtering (subscriber recebe apenas eventos relevantes)
- [ ] EventBridge Rules com patterns de matching
- [ ] EventBridge Schema Registry (descoberta de schemas)
- [ ] Step Functions — state machine (Choice, Parallel, Wait, Map)
- [ ] Step Functions — error handling (Retry, Catch)
- [ ] Secrets Manager — rotação de credenciais
- [ ] SSM Parameter Store — configurações por ambiente
- [ ] LocalStack para emular todos os serviços

---

## 📐 Especificação

### Workflow: Person Onboarding

```
API POST /api/persons
  │
  ▼
EventBridge (person-bus)
  │
  ├── Rule: person.created → Start Step Function
  ├── Rule: person.created → SNS notification
  └── Rule: person.* → CloudWatch Logs (audit)

Step Function (person-onboarding-workflow):
  ┌──────────────────────────────────────┐
  │  1. ValidatePerson (Task)            │
  │     ├── success → 2                  │
  │     └── fail → NotifyError           │
  │                                      │
  │  2. Parallel                         │
  │     ├── 2a. CreateWelcomeEmail       │
  │     ├── 2b. AssignDefaultRole        │
  │     └── 2c. InitializePreferences    │
  │                                      │
  │  3. Wait (30 seconds)               │
  │                                      │
  │  4. VerifyEmailSent (Choice)         │
  │     ├── verified → 5                 │
  │     └── not verified → ResendEmail   │
  │                                      │
  │  5. CompleteOnboarding               │
  └──────────────────────────────────────┘
```

### SNS Topics

| Topic | Subscribers | Filter |
|---|---|---|
| `person-notifications` | SQS (email-queue) | `event_type = PERSON_CREATED` |
| `person-notifications` | SQS (audit-queue) | Sem filtro (todos) |
| `person-notifications` | HTTP endpoint | `priority = HIGH` |

### EventBridge Events

```json
{
  "source": "api-person",
  "detail-type": "PersonCreated",
  "detail": {
    "id": 1,
    "name": "Wesley Santos",
    "email": "wesley@example.com",
    "timestamp": "2026-03-04T10:30:00Z"
  }
}
```

### EventBridge Rules

| Rule | Pattern | Target |
|---|---|---|
| `onboarding-start` | `detail-type: PersonCreated` | Step Functions |
| `notify-fan-out` | `detail-type: Person*` | SNS Topic |
| `audit-log` | `source: api-person` | CloudWatch Logs |

### Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/persons` | Cria person → EventBridge event |
| `GET` | `/api/persons/{id}/onboarding` | Status do workflow (Step Functions) |
| `GET` | `/api/config/{key}` | Ler configuração do Parameter Store |
| `GET` | `/api/health/aws` | Health check dos serviços AWS |

---

## ✅ Critérios de Aceite

- [ ] Evento publicado no EventBridge a cada Person criada
- [ ] EventBridge Rule roteando para Step Functions
- [ ] Step Function com Parallel, Choice, Wait e error handling
- [ ] SNS fan-out para 3 subscribers (SQS + SQS + HTTP)
- [ ] SNS Message Filtering funcionando
- [ ] Secrets Manager armazenando e rotacionando API keys
- [ ] Parameter Store com configurações por ambiente (dev/staging/prod)
- [ ] Endpoint retornando status do workflow
- [ ] LocalStack rodando todos os serviços
- [ ] Teste de integração end-to-end

---

## 🛠️ Implementar em

| Stack | Abordagem |
|---|---|
| **Spring Boot** | `spring-cloud-aws-starter-sns`, `spring-cloud-aws-starter-ssm`, AWS SDK v2 para EventBridge/StepFunctions |
| **Micronaut** | `micronaut-aws-sdk-v2` para todos serviços AWS |
| **Quarkus** | `quarkus-amazon-sns`, `quarkus-amazon-ssm`, `quarkus-amazon-eventbridge` |
| **Go** | `aws-sdk-go-v2` para todos (sns, eventbridge, sfn, ssm, secretsmanager) |

---

## 💡 Dicas

### Spring Boot — EventBridge
```java
@Service
public class PersonEventPublisher {
    private final EventBridgeClient eventBridge;

    public void publishPersonCreated(Person person) {
        eventBridge.putEvents(PutEventsRequest.builder()
            .entries(PutEventsRequestEntry.builder()
                .source("api-person")
                .detailType("PersonCreated")
                .eventBusName("person-bus")
                .detail(objectMapper.writeValueAsString(person))
                .build())
            .build());
    }
}
```

### Spring Boot — Parameter Store
```yaml
# application.yml
spring:
  cloud:
    aws:
      parameterstore:
        enabled: true
        prefix: /api-person
        profile-separator: _

# SSM Parameter: /api-person/dev/database.url → sa value
```

### Go — Step Functions
```go
func (s *Service) StartOnboarding(ctx context.Context, personID string) (string, error) {
    input, _ := json.Marshal(map[string]string{"person_id": personID})
    result, err := s.sfnClient.StartExecution(ctx, &sfn.StartExecutionInput{
        StateMachineArn: aws.String(s.stateMachineArn),
        Input:           aws.String(string(input)),
        Name:            aws.String(fmt.Sprintf("onboarding-%s-%d", personID, time.Now().Unix())),
    })
    return *result.ExecutionArn, err
}
```

---

## 🐳 Docker Compose

```yaml
services:
  localstack:
    image: localstack/localstack:4
    ports:
      - "4566:4566"
    environment:
      SERVICES: sns,sqs,events,stepfunctions,ssm,secretsmanager,logs
      DEFAULT_REGION: us-east-1
    volumes:
      - ./init-aws.sh:/etc/localstack/init/ready.d/init.sh

  app:
    build: .
    environment:
      AWS_ENDPOINT: http://localstack:4566
      AWS_REGION: us-east-1
      AWS_ACCESS_KEY_ID: test
      AWS_SECRET_ACCESS_KEY: test
    depends_on:
      - localstack
```

---

## 📦 Dependências

| Stack | Dependências |
|---|---|
| Spring | `spring-cloud-aws-starter-sns`, `spring-cloud-aws-starter-ssm`, `eventbridge`, `sfn` |
| Micronaut | `micronaut-aws-sdk-v2` |
| Quarkus | `quarkus-amazon-sns`, `quarkus-amazon-ssm`, `quarkus-amazon-eventbridge` |
| Go | `aws-sdk-go-v2/{sns,eventbridge,sfn,ssm,secretsmanager}` |

---

## 🔗 Referências

- [AWS EventBridge User Guide](https://docs.aws.amazon.com/eventbridge/latest/userguide/)
- [AWS Step Functions Developer Guide](https://docs.aws.amazon.com/step-functions/latest/dg/)
- [SNS Message Filtering](https://docs.aws.amazon.com/sns/latest/dg/sns-subscription-filter-policies.html)
- [LocalStack Documentation](https://docs.localstack.cloud/)
- [Spring Cloud AWS](https://docs.awspring.io/spring-cloud-aws/docs/current/reference/html/)
