# 🏆 Desafio 03 — Kafka Producer & Consumer

> **Nível:** ⭐⭐ Intermediário
> **Tipo:** Mensageria · Kafka · Event-Driven · Async · DLQ
> **Estimativa:** 8–12 horas por stack

---

## 📋 Descrição

Criar um **sistema completo de mensageria Kafka** com dois componentes:

1. **Producer** — publica eventos de domínio (`PersonCreated`, `PersonUpdated`, `PersonDeleted`) sempre que uma operação CRUD é realizada na API Person
2. **Consumer** — escuta o tópico `person-events`, processa os eventos e mantém uma **view materializada** em um banco separado

O Kafka deve rodar em modo **KRaft** (sem Zookeeper).

---

## 🎯 Objetivos de Aprendizado

### Producer
- [ ] Configuração de Kafka Producer
- [ ] Serialização de eventos (JSON / Avro)
- [ ] Chaveamento de mensagens (partition key = person.id)
- [ ] Headers de mensagens (correlation-id, event-type, source)
- [ ] Garantias de entrega (`acks=all`, `enable.idempotence=true`)

### Consumer
- [ ] Consumer Group (`person-events-materializer`)
- [ ] Desserialização de eventos
- [ ] Offset commit manual (após processamento bem-sucedido)
- [ ] Dead Letter Queue (DLQ) — tópico `person-events-dlq`
- [ ] Retry com backoff exponencial (3 tentativas: 1s → 2s → 4s)
- [ ] Processamento idempotente (tabela `processed_events`)
- [ ] Consumer lag monitoring via health check

### Transversal
- [ ] Docker Compose com Kafka KRaft
- [ ] Testes de integração com Embedded Kafka / Testcontainers

---

## 📐 Especificação

### Tópicos

| Tópico | Partições | Retenção | Uso |
|---|---|---|---|
| `person-events` | 3 | 7 dias | Eventos de domínio |
| `person-events-dlq` | 1 | 30 dias | Mensagens com falha |

### Eventos

#### PersonCreated
```json
{
  "event_type": "PERSON_CREATED",
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-03-04T10:30:00Z",
  "source": "api-person-spring",
  "data": {
    "id": 1,
    "name": "Wesley Santos",
    "email": "wesley@example.com",
    "birth_date": "1991-01-15"
  }
}
```

#### PersonUpdated
```json
{
  "event_type": "PERSON_UPDATED",
  "event_id": "...",
  "timestamp": "...",
  "source": "api-person-spring",
  "data": { ... },
  "previous_data": { ... }
}
```

#### PersonDeleted
```json
{
  "event_type": "PERSON_DELETED",
  "event_id": "...",
  "timestamp": "...",
  "source": "api-person-spring",
  "data": { "id": 1 }
}
```

### Headers das Mensagens

| Header | Exemplo |
|---|---|
| `X-Event-Type` | `PERSON_CREATED` |
| `X-Correlation-Id` | UUID v4 |
| `X-Source` | `api-person-spring` |
| `X-Timestamp` | ISO 8601 |

### View Materializada (tabela `person_view`)

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | Long | Mesmo ID da origem |
| `name` | String | Nome |
| `email` | String | Email |
| `birth_date` | Date | Data nascimento |
| `event_type` | String | Último evento processado |
| `event_id` | UUID | ID do último evento |
| `processed_at` | Timestamp | Quando foi processado |

### Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/persons` | Cria person + publica `PERSON_CREATED` |
| `PUT` | `/api/persons/{id}` | Atualiza + publica `PERSON_UPDATED` |
| `DELETE` | `/api/persons/{id}` | Remove + publica `PERSON_DELETED` |
| `GET` | `/api/persons/view` | Lista a view materializada |
| `GET` | `/api/persons/view/{id}` | Busca na view por ID |
| `GET` | `/health/kafka` | Status do consumer (lag, partitions) |

---

## ✅ Critérios de Aceite

### Producer
- [ ] Evento publicado a cada operação CRUD
- [ ] JSON schema consistente para todos os eventos
- [ ] Partition key = person ID (garante ordenação por pessoa)
- [ ] Headers com metadados do evento
- [ ] `acks=all` + `enable.idempotence=true`

### Consumer
- [ ] Consumer escutando `person-events` corretamente
- [ ] View materializada atualizada em tempo real
- [ ] Processamento idempotente (reprocessar sem duplicar)
- [ ] DLQ para eventos que falharam após 3 retries
- [ ] Retry com backoff exponencial
- [ ] Manual offset commit
- [ ] Health check expondo consumer lag

### Integração
- [ ] Docker Compose com Kafka KRaft (sem Zookeeper)
- [ ] Teste de integração validando publish → consume → view
- [ ] Teste de DLQ (enviar evento malformado)

---

## 🛠️ Implementar em

| Stack | Producer | Consumer |
|---|---|---|
| **Spring Boot** | `KafkaTemplate.send()` | `@KafkaListener` + `DeadLetterPublishingRecoverer` |
| **Micronaut** | `@KafkaClient` interface | `@KafkaListener` + `@Topic` |
| **Quarkus** | `@Channel` + `Emitter` (Reactive Messaging) | `@Incoming` + `Message.ack()` |
| **Go** | `segmentio/kafka-go` Writer | `kafka-go` Reader + goroutine |

---

## 💡 Dicas

### Spring Boot — Producer
```java
@Service
public class PersonEventProducer {
    private final KafkaTemplate<String, PersonEvent> kafka;

    public void publish(PersonEvent event) {
        var headers = new RecordHeaders();
        headers.add("X-Event-Type", event.getType().getBytes());
        headers.add("X-Correlation-Id", UUID.randomUUID().toString().getBytes());

        kafka.send(new ProducerRecord<>(
            "person-events",
            null, null,
            String.valueOf(event.getData().getId()),
            event, headers
        ));
    }
}
```

### Spring Boot — Consumer
```java
@KafkaListener(topics = "person-events", groupId = "person-events-materializer")
public void consume(@Payload PersonEvent event,
                    @Header("X-Event-Type") String eventType,
                    Acknowledgment ack) {
    if (alreadyProcessed(event.getEventId())) {
        ack.acknowledge();
        return;
    }
    processEvent(event, eventType);
    markProcessed(event.getEventId());
    ack.acknowledge();
}
```

### Micronaut — Producer
```java
@KafkaClient
public interface PersonEventProducer {
    @Topic("person-events")
    void publish(@KafkaKey String key, PersonEvent event);
}
```

### Micronaut — Consumer
```java
@KafkaListener(groupId = "person-events-materializer", offsetReset = EARLIEST)
public class PersonEventConsumer {
    @Topic("person-events")
    public void receive(@KafkaKey String key, PersonEvent event,
                        @MessageHeader("X-Event-Type") String type) {
        processEvent(event, type);
    }
}
```

### Quarkus — Producer
```java
@Channel("person-events")
Emitter<PersonEvent> emitter;

public void publish(PersonEvent event) {
    emitter.send(Message.of(event)
        .withMetadata(OutgoingKafkaRecordMetadata.builder()
            .withKey(String.valueOf(event.getData().getId()))
            .withHeaders(new RecordHeaders()
                .add("X-Event-Type", event.getType().getBytes()))
            .build()));
}
```

### Quarkus — Consumer
```java
@ApplicationScoped
public class PersonEventConsumer {
    @Incoming("person-events")
    public CompletionStage<Void> consume(Message<PersonEvent> message) {
        return processEvent(message.getPayload())
            .thenRun(message::ack);
    }
}
```

### Go — Producer & Consumer
```go
// Producer
writer := kafka.NewWriter(kafka.WriterConfig{
    Brokers: []string{"localhost:9092"},
    Topic:   "person-events",
})
writer.WriteMessages(ctx, kafka.Message{
    Key:   []byte(personID),
    Value: eventJSON,
    Headers: []kafka.Header{
        {Key: "X-Event-Type", Value: []byte("PERSON_CREATED")},
    },
})

// Consumer (goroutine)
reader := kafka.NewReader(kafka.ReaderConfig{
    Brokers: []string{"localhost:9092"},
    Topic:   "person-events",
    GroupID: "person-events-materializer",
})
for {
    msg, err := reader.ReadMessage(ctx)
    if err != nil { break }
    processEvent(msg)
}
```

---

## 🔄 Idempotência

Para garantir processamento idempotente:

```
1. Receber evento com event_id = "abc-123"
2. Verificar: SELECT 1 FROM processed_events WHERE event_id = 'abc-123'
3. Se já existe → skip (já processado)
4. Se não existe → processar + INSERT INTO processed_events (event_id, processed_at)
5. Commit offset
```

---

## 🐳 Docker Compose

```yaml
services:
  kafka:
    image: apache/kafka:3.9.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LOG_DIRS: /tmp/kraft-combined-logs
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk

  postgres-view:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: person_view
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin
    ports:
      - "5433:5432"
```

---

## 📦 Dependências

| Stack | Dependência |
|---|---|
| Spring | `spring-kafka` |
| Micronaut | `micronaut-kafka` |
| Quarkus | `quarkus-smallrye-reactive-messaging-kafka` |
| Go | `github.com/segmentio/kafka-go` |

---

## 🔗 Referências

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://spring.io/projects/spring-kafka)
- [KRaft Mode (Kafka without Zookeeper)](https://kafka.apache.org/documentation/#kraft)
- [Dead Letter Queue Pattern](https://www.enterpriseintegrationpatterns.com/DeadLetterChannel.html)
- [Idempotent Consumer Pattern](https://microservices.io/patterns/communication-style/idempotent-consumer.html)
