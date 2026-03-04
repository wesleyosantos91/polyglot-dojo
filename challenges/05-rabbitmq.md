# 🏆 Desafio 06 — RabbitMQ Producer & Consumer

> **Nível:** ⭐⭐ Intermediário
> **Tipo:** Mensageria · RabbitMQ · AMQP · Exchange/Queue
> **Estimativa:** 5–7 horas por stack

---

## 📋 Descrição

Criar um sistema de **notificações assíncronas** usando RabbitMQ. Quando uma Person é criada/atualizada/deletada, o serviço publica mensagens em um **Topic Exchange**. Consumers separados assinam filas com routing keys diferentes para processar tipos específicos de eventos.

---

## 🎯 Objetivos de Aprendizado

- [ ] Conceitos AMQP: Exchange, Queue, Binding, Routing Key
- [ ] Tipos de Exchange (Direct, Topic, Fanout, Headers)
- [ ] Publisher Confirms (garantia de entrega)
- [ ] Consumer Acknowledgement (manual ack/nack)
- [ ] Dead Letter Exchange (DLX)
- [ ] Prefetch count (controle de concorrência)
- [ ] RabbitMQ Management UI

---

## 📐 Especificação

### Topologia RabbitMQ

```
                          ┌───────────────────────────┐
                          │  person.topic (Exchange)   │
                          │      type: topic           │
                          └─────────┬─────────────────┘
                  ┌─────────────────┼──────────────────┐
                  │                 │                   │
          person.created.*  person.updated.*   person.deleted.*
                  │                 │                   │
                  ▼                 ▼                   ▼
         ┌──────────────┐ ┌──────────────┐   ┌──────────────┐
         │ notification │ │   audit-log  │   │   cleanup    │
         │    queue     │ │    queue     │   │    queue     │
         └──────────────┘ └──────────────┘   └──────────────┘
                  │                 │                   │
         Email/Push           Log no DB         Limpar cache
```

### Exchanges

| Exchange | Tipo | Propósito |
|---|---|---|
| `person.topic` | Topic | Roteamento por evento |
| `person.dlx` | Fanout | Dead Letter Exchange |

### Queues

| Queue | Binding Key | Consumer |
|---|---|---|
| `person.notification` | `person.created.*` | Envia email de boas-vindas |
| `person.audit` | `person.#` | Registra todos os eventos no audit log |
| `person.cleanup` | `person.deleted.*` | Limpa dados relacionados |
| `person.dlq` | Bind em `person.dlx` | Mensagens com falha |

### Routing Keys

| Evento | Routing Key |
|---|---|
| Person criada | `person.created.v1` |
| Person atualizada | `person.updated.v1` |
| Person deletada | `person.deleted.v1` |

### Mensagem

```json
{
  "routing_key": "person.created.v1",
  "message_id": "uuid",
  "timestamp": "2026-03-04T10:30:00Z",
  "payload": {
    "id": 1,
    "name": "Wesley Santos",
    "email": "wesley@example.com"
  }
}
```

---

## ✅ Critérios de Aceite

- [ ] Topic Exchange com 3 routing keys
- [ ] 3 consumers independentes (notification, audit, cleanup)
- [ ] Publisher Confirms habilitado
- [ ] Manual acknowledgement nos consumers
- [ ] Dead Letter Exchange para mensagens falhadas
- [ ] Prefetch = 10 (controle de throughput)
- [ ] Docker Compose com RabbitMQ + Management UI
- [ ] Teste de integração (Testcontainers)

---

## 🛠️ Implementar em

| Stack | Abordagem |
|---|---|
| **Spring Boot** | Spring AMQP / `@RabbitListener` |
| **Micronaut** | Micronaut RabbitMQ / `@RabbitListener` |
| **Quarkus** | Quarkus AMQP / Reactive Messaging |
| **Go** | `github.com/rabbitmq/amqp091-go` |

---

## 💡 Dicas

### Spring Boot
```java
// Producer
rabbitTemplate.convertAndSend("person.topic", "person.created.v1", event);

// Consumer
@RabbitListener(bindings = @QueueBinding(
    value = @Queue("person.notification"),
    exchange = @Exchange(value = "person.topic", type = "topic"),
    key = "person.created.*"
))
public void onPersonCreated(PersonEvent event) {
    sendWelcomeEmail(event);
}
```

### Go
```go
// Publish
ch.PublishWithContext(ctx, "person.topic", "person.created.v1", false, false,
    amqp.Publishing{
        ContentType: "application/json",
        Body:        eventJSON,
    })

// Consume
msgs, _ := ch.Consume("person.notification", "", false, false, false, false, nil)
for msg := range msgs {
    process(msg)
    msg.Ack(false)
}
```

---

## 🐳 Docker Compose

```yaml
services:
  rabbitmq:
    image: rabbitmq:4.1-management
    ports:
      - "5672:5672"    # AMQP
      - "15672:15672"  # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
```

> Management UI: http://localhost:15672 (guest/guest)

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `spring-boot-starter-amqp` |
| Micronaut | `micronaut-rabbitmq` |
| Quarkus | `quarkus-smallrye-reactive-messaging-amqp` |
| Go | `github.com/rabbitmq/amqp091-go` |

---

## 🔗 Referências

- [RabbitMQ Tutorials](https://www.rabbitmq.com/tutorials)
- [AMQP Concepts](https://www.rabbitmq.com/tutorials/amqp-concepts)
- [Spring AMQP](https://spring.io/projects/spring-amqp)
- [Topic Exchange](https://www.rabbitmq.com/tutorials/tutorial-five-java)
