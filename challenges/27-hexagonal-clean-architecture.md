# 🏆 Desafio 27 — Hexagonal Architecture & Clean Architecture

> **Nível:** ⭐⭐⭐⭐ Expert
> **Tipo:** Architecture · Hexagonal · Clean · Ports & Adapters · Domain-Driven
> **Estimativa:** 10–14 horas por stack

---

## 📋 Descrição

Refatorar a API Person aplicando **Hexagonal Architecture (Ports & Adapters)** com princípios de **Clean Architecture**. O domínio fica isolado no centro, sem dependências de frameworks, e toda interação externa é feita através de **ports** (interfaces) e **adapters** (implementações).

---

## 🎯 Objetivos de Aprendizado

- [ ] Hexagonal Architecture — Ports & Adapters
- [ ] Clean Architecture — Dependency Rule (de fora para dentro)
- [ ] Domain Layer — entities, value objects, domain services
- [ ] Application Layer — use cases, ports (interfaces)
- [ ] Infrastructure Layer — adapters (DB, HTTP, messaging)
- [ ] Inversão de dependências — domínio NÃO depende de framework
- [ ] Testabilidade — domínio testável sem infraestrutura
- [ ] Modularização — módulos/pacotes por camada
- [ ] Anti-corruption Layer (ACL)

---

## 📐 Especificação

### Estrutura de Pacotes

#### Java (Spring/Micronaut/Quarkus)
```
src/main/java/io/github/workshop/
├── domain/                          # ← Centro (ZERO dependências externas)
│   ├── model/
│   │   ├── Person.java              # Entity (domain rules inside)
│   │   ├── Email.java               # Value Object (self-validating)
│   │   ├── PersonName.java          # Value Object
│   │   └── BirthDate.java           # Value Object
│   ├── port/
│   │   ├── in/                      # ← Ports de ENTRADA (use cases)
│   │   │   ├── CreatePersonUseCase.java
│   │   │   ├── FindPersonUseCase.java
│   │   │   ├── UpdatePersonUseCase.java
│   │   │   └── DeletePersonUseCase.java
│   │   └── out/                     # ← Ports de SAÍDA (infra interfaces)
│   │       ├── PersonRepository.java
│   │       ├── PersonEventPublisher.java
│   │       └── NotificationSender.java
│   ├── service/
│   │   └── PersonDomainService.java  # Domain service (implements use cases)
│   └── exception/
│       ├── PersonNotFoundException.java
│       └── DuplicateEmailException.java
│
├── application/                      # ← Orquestração
│   ├── usecase/
│   │   ├── CreatePersonUseCaseImpl.java
│   │   ├── FindPersonUseCaseImpl.java
│   │   └── ...
│   └── dto/
│       ├── CreatePersonCommand.java
│       └── PersonResult.java
│
└── infrastructure/                   # ← Adapters (framework-dependent)
    ├── adapter/
    │   ├── in/                       # ← Adapters de ENTRADA
    │   │   ├── rest/
    │   │   │   ├── PersonController.java
    │   │   │   ├── PersonRequest.java
    │   │   │   ├── PersonResponse.java
    │   │   │   └── PersonMapper.java
    │   │   └── grpc/
    │   │       └── PersonGrpcAdapter.java
    │   └── out/                      # ← Adapters de SAÍDA
    │       ├── persistence/
    │       │   ├── PersonJpaAdapter.java      # implements PersonRepository
    │       │   ├── PersonJpaEntity.java       # JPA entity (infra concern)
    │       │   └── PersonJpaRepository.java   # Spring Data interface
    │       ├── messaging/
    │       │   └── KafkaPersonEventAdapter.java  # implements PersonEventPublisher
    │       └── notification/
    │           └── EmailNotificationAdapter.java # implements NotificationSender
    └── config/
        └── BeanConfig.java           # Wiring ports → adapters
```

#### Go
```
internal/
├── domain/                           # ← Centro
│   ├── person.go                     # Entity + Value Objects
│   ├── person_repository.go          # Port interface (out)
│   ├── person_service.go             # Domain service
│   ├── event_publisher.go            # Port interface (out)
│   └── errors.go                     # Domain errors
│
├── application/                      # ← Use cases
│   ├── create_person.go              # Use case (port in)
│   ├── find_person.go
│   └── dto.go                        # Commands/Results
│
├── adapter/                          # ← Infrastructure
│   ├── in/
│   │   └── http/
│   │       ├── person_handler.go     # HTTP adapter
│   │       ├── request.go
│   │       └── response.go
│   └── out/
│       ├── postgres/
│       │   └── person_repo.go        # implements domain.PersonRepository
│       └── kafka/
│           └── event_publisher.go    # implements domain.EventPublisher
│
└── config/
    └── wire.go                       # Dependency injection
```

### Dependency Rule

```
             ┌─────────────────────────────────────┐
             │        Infrastructure Layer          │
             │  (Controllers, DB, Kafka, HTTP)      │
             │                                      │
             │    ┌──────────────────────────┐      │
             │    │    Application Layer      │      │
             │    │   (Use Cases, DTOs)       │      │
             │    │                           │      │
             │    │   ┌──────────────────┐   │      │
             │    │   │  Domain Layer     │   │      │
             │    │   │ (Entities, VOs,   │   │      │
             │    │   │  Ports, Services) │   │      │
             │    │   └──────────────────┘   │      │
             │    └──────────────────────────┘      │
             └─────────────────────────────────────┘

Dependency direction: OUTSIDE → INSIDE (nunca o contrário!)
Domain Layer NÃO importa nada de Spring, Hibernate, Kafka, etc.
```

### Domain Model (Value Objects)

```java
// Value Object — self-validating, immutable
public record Email(String value) {
    public Email {
        if (value == null || !value.matches("^[\\w.]+@[\\w.]+\\.[a-z]{2,}$")) {
            throw new InvalidEmailException(value);
        }
        value = value.toLowerCase().trim();
    }
}

public record PersonName(String value) {
    public PersonName {
        if (value == null || value.trim().length() < 2) {
            throw new InvalidPersonNameException(value);
        }
        value = value.trim();
    }
}

// Entity — domain rules inside
public class Person {
    private final PersonId id;
    private PersonName name;
    private Email email;
    private BirthDate birthDate;

    public void changeName(PersonName newName) {
        if (newName.equals(this.name)) return;
        this.name = newName;
        // raise domain event: PersonNameChanged
    }

    public int getAge() {
        return birthDate.calculateAge();
    }

    public boolean isAdult() {
        return getAge() >= 18;
    }
}
```

---

## ✅ Critérios de Aceite

- [ ] Domain layer com ZERO imports de framework (verificável por ArchUnit/lint)
- [ ] Value Objects imutáveis e auto-validantes (Email, PersonName, BirthDate)
- [ ] Ports (interfaces) definidos no domain
- [ ] Adapters implementando ports na camada de infrastructure
- [ ] Use Cases orquestrando domain logic
- [ ] Dependency injection configurada (wiring adapters → ports)
- [ ] Testes unitários do domain sem nenhuma infra (sem DB, sem HTTP)
- [ ] Testes de integração dos adapters com Testcontainers
- [ ] Regra arquitetural validada (ArchUnit / Go lint)
- [ ] Mapper entre domain entities e persistence entities (separação)
- [ ] Funcionamento idêntico ao API original (mesmos endpoints)

---

## 🛠️ Implementar em

| Stack | DI / Wiring | Arch Test |
|---|---|---|
| **Spring Boot** | `@Bean` configuration | ArchUnit |
| **Micronaut** | `@Factory` + `@Bean` | ArchUnit |
| **Quarkus** | CDI `@Produces` | ArchUnit |
| **Go** | Constructor injection (manual) ou Wire | `go/analysis` custom |

---

## 💡 Dicas

### Spring Boot — Port Interface
```java
// domain/port/out/PersonRepository.java (NO Spring imports!)
public interface PersonRepository {
    Person save(Person person);
    Optional<Person> findById(PersonId id);
    Optional<Person> findByEmail(Email email);
    List<Person> findAll(int page, int size);
    void deleteById(PersonId id);
    boolean existsByEmail(Email email);
}
```

### Spring Boot — Adapter Implementation
```java
// infrastructure/adapter/out/persistence/PersonJpaAdapter.java
@Repository
@RequiredArgsConstructor
public class PersonJpaAdapter implements PersonRepository {
    private final PersonJpaRepository jpaRepo; // Spring Data
    private final PersonPersistenceMapper mapper;

    @Override
    public Person save(Person person) {
        var entity = mapper.toJpaEntity(person);
        var saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Person> findByEmail(Email email) {
        return jpaRepo.findByEmail(email.value())
            .map(mapper::toDomain);
    }
}
```

### Spring Boot — ArchUnit Test
```java
@AnalyzeClasses(packages = "io.github.workshop")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..infrastructure..",
                "org.springframework..",
                "jakarta.persistence..",
                "io.micronaut..",
                "io.quarkus.."
            );

    @ArchTest
    static final ArchRule useCasesShouldOnlyDependOnDomain =
        classes()
            .that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "..application..", "java..");
}
```

### Go — Domain Interface
```go
// internal/domain/person_repository.go
// Nenhum import de GORM, pgx, ou qualquer framework
package domain

type PersonRepository interface {
    Save(ctx context.Context, person *Person) error
    FindByID(ctx context.Context, id PersonID) (*Person, error)
    FindByEmail(ctx context.Context, email Email) (*Person, error)
    FindAll(ctx context.Context, page, size int) ([]*Person, error)
    Delete(ctx context.Context, id PersonID) error
    ExistsByEmail(ctx context.Context, email Email) (bool, error)
}
```

### Go — Adapter
```go
// internal/adapter/out/postgres/person_repo.go
package postgres

import (
    "context"
    "workshop/internal/domain"
    "gorm.io/gorm" // dependency only HERE, not in domain
)

type PersonPostgresRepo struct {
    db *gorm.DB
}

func (r *PersonPostgresRepo) Save(ctx context.Context, person *domain.Person) error {
    entity := toEntity(person) // mapper
    if err := r.db.WithContext(ctx).Create(&entity).Error; err != nil {
        return err
    }
    person.SetID(domain.PersonID(entity.ID))
    return nil
}
```

---

## 📦 Dependências

| Stack | Dependência Extra |
|---|---|
| Spring | `com.tngtech.archunit:archunit-junit5` |
| Micronaut | `com.tngtech.archunit:archunit-junit5` |
| Quarkus | `com.tngtech.archunit:archunit-junit5` |
| Go | Nenhuma extra (interfaces nativas) |

---

## 🔗 Referências

- [Alistair Cockburn — Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture — Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [ArchUnit](https://www.archunit.org/)
- [Ports & Adapters in Go](https://threedots.tech/post/introducing-clean-architecture/)
