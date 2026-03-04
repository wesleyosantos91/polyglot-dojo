# 🏆 Desafio 15 — Event Sourcing + CQRS

> **Nível:** ⭐⭐⭐⭐ Expert
> **Tipo:** Event Sourcing · CQRS · Event Store · Projections
> **Estimativa:** 10–14 horas por stack

---

## 📋 Descrição

Reimplementar a API Person usando **Event Sourcing** (estado derivado de eventos) e **CQRS** (separação de leitura e escrita). Toda mudança é registrada como um evento imutável. As leituras são feitas em projeções otimizadas.

---

## 🎯 Objetivos de Aprendizado

- [ ] Event Sourcing — estado = replay de eventos
- [ ] CQRS — Command/Query responsibility segregation
- [ ] Event Store (append-only log)
- [ ] Aggregates e invariantes de domínio
- [ ] Projeções (read models) atualizadas por eventos
- [ ] Snapshots para performance
- [ ] Eventual consistency
- [ ] Event versioning e upcasting

---

## 📐 Especificação

### Arquitetura CQRS

```
                    WRITE SIDE                          READ SIDE
                  ┌──────────────┐                    ┌──────────────┐
 POST/PUT/DELETE  │  Command     │    Events          │  Query       │  GET
────────────────▶ │  Handler     │──────────────────▶  │  Handler     │◀────────
                  │              │                    │              │
                  │  Aggregate   │                    │  Projections │
                  │  (Person)    │                    │  (Read Model)│
                  └──────┬───────┘                    └──────┬───────┘
                         │                                   │
                  ┌──────▼───────┐                    ┌──────▼───────┐
                  │  Event Store │                    │  PostgreSQL  │
                  │  (Events DB) │                    │  (Read DB)   │
                  └──────────────┘                    └──────────────┘
```

### Eventos de Domínio

```java
// Base
abstract class PersonEvent {
    UUID eventId;
    Long aggregateId;
    int version;
    Instant timestamp;
    String eventType;
}
```

| Evento | Dados |
|---|---|
| `PersonCreated` | name, email, birthDate |
| `PersonNameUpdated` | oldName, newName |
| `PersonEmailUpdated` | oldEmail, newEmail |
| `PersonDeleted` | reason |

### Event Store (tabela)

| Campo | Tipo | Descrição |
|---|---|---|
| `event_id` | UUID | PK |
| `aggregate_id` | Long | ID do aggregate (Person) |
| `aggregate_type` | String | `"Person"` |
| `event_type` | String | `"PersonCreated"` |
| `version` | Int | Versão sequencial |
| `payload` | JSONB | Dados do evento |
| `metadata` | JSONB | correlation_id, user_id, etc. |
| `created_at` | Timestamp | Quando o evento ocorreu |

### Commands

| Command | Resultado |
|---|---|
| `CreatePersonCommand` | → `PersonCreated` event |
| `UpdatePersonNameCommand` | → `PersonNameUpdated` event |
| `UpdatePersonEmailCommand` | → `PersonEmailUpdated` event |
| `DeletePersonCommand` | → `PersonDeleted` event |

### Endpoints

#### Write (Commands)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/persons/commands/create` | Envia CreatePersonCommand |
| `POST` | `/api/persons/commands/update-name` | Envia UpdatePersonNameCommand |
| `POST` | `/api/persons/commands/update-email` | Envia UpdatePersonEmailCommand |
| `POST` | `/api/persons/commands/delete` | Envia DeletePersonCommand |

#### Read (Queries)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/persons` | Lista projeção (read model) |
| `GET` | `/api/persons/{id}` | Busca na projeção |
| `GET` | `/api/persons/{id}/events` | Lista eventos do aggregate |
| `GET` | `/api/persons/{id}/snapshot` | Snapshot atual |

### Replay de Eventos

```
PersonCreated(name="Wesley", email="w@x.com")     → state = {name:"Wesley", email:"w@x.com"}
PersonNameUpdated(oldName="Wesley", newName="Wes") → state = {name:"Wes", email:"w@x.com"}
PersonEmailUpdated(oldEmail="w@x.com", newEmail="wes@x.com") → state = {name:"Wes", email:"wes@x.com"}
```

---

## ✅ Critérios de Aceite

- [ ] Event Store append-only (nunca UPDATE/DELETE)
- [ ] Aggregate reconstruído via replay de eventos
- [ ] Projeção (read model) atualizada por events
- [ ] Versionamento de eventos (optimistic concurrency)
- [ ] Snapshot a cada N eventos (ex: 10)
- [ ] Endpoint para ver histórico de eventos
- [ ] Eventual consistency entre write e read
- [ ] Teste: replay completo reconstrói estado correto

---

## 🛠️ Implementar em

| Stack | Abordagem |
|---|---|
| **Spring Boot** | Custom Event Store + Spring Events / Axon Framework (opcional) |
| **Micronaut** | Custom Event Store + Application Events |
| **Quarkus** | Custom Event Store + CDI Events |
| **Go** | Custom Event Store + interfaces |

---

## 💡 Dicas

### Aggregate (Person)

```java
public class PersonAggregate {
    private Long id;
    private String name;
    private String email;
    private int version;
    private List<PersonEvent> uncommittedEvents = new ArrayList<>();
    
    // Reconstruct from events
    public static PersonAggregate fromEvents(List<PersonEvent> events) {
        var aggregate = new PersonAggregate();
        events.forEach(aggregate::apply);
        return aggregate;
    }
    
    // Command → Event
    public void updateName(String newName) {
        if (newName.equals(this.name)) throw new IllegalArgumentException("Same name");
        apply(new PersonNameUpdated(this.id, this.name, newName));
    }
    
    // Apply event (mutate state)
    private void apply(PersonEvent event) {
        switch (event) {
            case PersonCreated e -> { this.id = e.id(); this.name = e.name(); this.email = e.email(); }
            case PersonNameUpdated e -> { this.name = e.newName(); }
            // ...
        }
        this.version++;
    }
}
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | (custom) ou `org.axonframework:axon-spring-boot-starter` |
| Micronaut | (custom event store) |
| Quarkus | (custom event store) |
| Go | (custom — interfaces + structs) |

---

## 🔗 Referências

- [Event Sourcing Pattern (Martin Fowler)](https://martinfowler.com/eaaDev/EventSourcing.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Axon Framework](https://developer.axoniq.io/)
- [Event Store (Greg Young)](https://www.eventstore.com/)
