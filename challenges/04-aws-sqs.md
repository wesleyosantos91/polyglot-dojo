# 🏆 Desafio 05 — AWS SQS Producer & Consumer

> **Nível:** ⭐⭐ Intermediário
> **Tipo:** Mensageria · AWS SQS · Queue · Async Processing
> **Estimativa:** 5–7 horas por stack

---

## 📋 Descrição

Criar dois serviços: um **producer** que envia mensagens para uma fila SQS e um **consumer** que processa as mensagens. O cenário é um sistema de **processamento de cadastro assíncrono** — a API recebe o pedido, coloca na fila e retorna `202 Accepted`. O consumer processa, valida e persiste.

---

## 🎯 Objetivos de Aprendizado

- [ ] AWS SQS Standard vs FIFO queues
- [ ] Envio de mensagens com atributos
- [ ] Long polling vs short polling
- [ ] Visibility timeout e retry automático
- [ ] Dead Letter Queue (DLQ) nativa do SQS
- [ ] Mensagens com deduplication (FIFO)
- [ ] LocalStack para desenvolvimento local

---

## 📐 Especificação

### Filas SQS

| Fila | Tipo | Propósito |
|---|---|---|
| `person-registration-queue` | Standard | Fila principal de cadastro |
| `person-registration-dlq` | Standard | Mensagens com falha (max 3 retries) |

### Producer (API)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/persons/register` | Enfileira cadastro, retorna `202 Accepted` |
| `GET` | `/api/persons/register/{requestId}` | Consulta status do processamento |

#### Request
```json
{
  "name": "Wesley Santos",
  "email": "wesley@example.com",
  "birth_date": "1991-01-15"
}
```

#### Response (202)
```json
{
  "request_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "QUEUED",
  "message": "Registration is being processed"
}
```

### Consumer (Worker)

Processa mensagens da fila:
1. Valida dados
2. Verifica email duplicado
3. Persiste no banco
4. Atualiza status do request

### Tabela: registration_requests

| Campo | Tipo | Descrição |
|---|---|---|
| `request_id` | UUID | PK |
| `status` | Enum | `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `payload` | JSON | Dados originais |
| `result` | JSON | Person criada ou erro |
| `created_at` | Timestamp | Quando enfileirou |
| `processed_at` | Timestamp | Quando processou |

---

## ✅ Critérios de Aceite

- [ ] Producer envia mensagem e retorna `202 Accepted`
- [ ] Consumer processa mensagens automaticamente
- [ ] Status consultável via `GET /register/{requestId}`
- [ ] DLQ configurada (maxReceiveCount = 3)
- [ ] Visibility timeout adequado (30s)
- [ ] Long polling habilitado (waitTimeSeconds = 20)
- [ ] LocalStack rodando via Docker Compose
- [ ] Teste de integração end-to-end

---

## 🛠️ Implementar em

| Stack | Abordagem |
|---|---|
| **Spring Boot** | Spring Cloud AWS SQS / `@SqsListener` |
| **Micronaut** | Micronaut AWS SDK v2 |
| **Quarkus** | Quarkus Amazon SQS |
| **Go** | AWS SDK for Go v2 (`github.com/aws/aws-sdk-go-v2`) |

---

## 💡 Dicas

### Spring Boot
```java
// Producer
sqsTemplate.send("person-registration-queue", message);

// Consumer
@SqsListener("person-registration-queue")
public void process(@Payload RegistrationMessage msg) {
    // processar
}
```

### Go
```go
// Producer
client.SendMessage(ctx, &sqs.SendMessageInput{
    QueueUrl:    &queueURL,
    MessageBody: &messageJSON,
})

// Consumer (polling loop)
for {
    output, _ := client.ReceiveMessage(ctx, &sqs.ReceiveMessageInput{
        QueueUrl:            &queueURL,
        WaitTimeSeconds:     20,
        MaxNumberOfMessages: 10,
    })
    for _, msg := range output.Messages {
        process(msg)
        client.DeleteMessage(ctx, &sqs.DeleteMessageInput{...})
    }
}
```

---

## 🐳 Docker Compose (LocalStack)

```yaml
services:
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"
    environment:
      SERVICES: sqs
      DEFAULT_REGION: us-east-1

  init-aws:
    image: amazon/aws-cli:latest
    depends_on:
      - localstack
    entrypoint: /bin/sh -c
    command: >
      "
      aws --endpoint-url=http://localstack:4566 sqs create-queue --queue-name person-registration-dlq &&
      aws --endpoint-url=http://localstack:4566 sqs create-queue --queue-name person-registration-queue \
        --attributes '{\"RedrivePolicy\": \"{\\\"deadLetterTargetArn\\\":\\\"arn:aws:sqs:us-east-1:000000000000:person-registration-dlq\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}'
      "
    environment:
      AWS_ACCESS_KEY_ID: test
      AWS_SECRET_ACCESS_KEY: test
      AWS_DEFAULT_REGION: us-east-1
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `spring-cloud-aws-starter-sqs` |
| Micronaut | `micronaut-aws-sdk-sqs` |
| Quarkus | `quarkus-amazon-sqs` |
| Go | `github.com/aws/aws-sdk-go-v2/service/sqs` |

---

## 🔗 Referências

- [AWS SQS Developer Guide](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/)
- [LocalStack](https://docs.localstack.cloud/)
- [Spring Cloud AWS](https://awspring.io/)
- [Quarkus Amazon SQS](https://docs.quarkiverse.io/quarkus-amazon-services/dev/amazon-sqs.html)
