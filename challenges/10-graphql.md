# 🏆 Desafio 11 — GraphQL API

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** GraphQL · Query · Mutation · Subscription · DataLoader
> **Estimativa:** 7–9 horas por stack

---

## 📋 Descrição

Criar uma API **GraphQL** completa para Person com Queries, Mutations, Subscriptions e resolução de relacionamentos (Person → Addresses → Contacts). Implementar DataLoader para evitar o problema N+1.

---

## 🎯 Objetivos de Aprendizado

- [ ] Schema-first vs Code-first GraphQL
- [ ] Queries com filtros e paginação
- [ ] Mutations com input validation
- [ ] Subscriptions (WebSocket real-time)
- [ ] DataLoader (batching para evitar N+1)
- [ ] Error handling no GraphQL
- [ ] Introspection e Playground
- [ ] Custom scalars (Date, DateTime)

---

## 📐 Especificação

### Schema GraphQL

```graphql
scalar Date
scalar DateTime

type Person {
  id: ID!
  name: String!
  email: String!
  birthDate: Date!
  addresses: [Address!]!
  contacts: [Contact!]!
  createdAt: DateTime!
  updatedAt: DateTime!
}

type Address {
  id: ID!
  street: String!
  city: String!
  state: String!
  zipCode: String!
  primary: Boolean!
}

type Contact {
  id: ID!
  type: ContactType!
  value: String!
}

enum ContactType {
  PHONE
  MOBILE
  WHATSAPP
}

input CreatePersonInput {
  name: String!
  email: String!
  birthDate: Date!
  addresses: [CreateAddressInput!]
  contacts: [CreateContactInput!]
}

input CreateAddressInput {
  street: String!
  city: String!
  state: String!
  zipCode: String!
  primary: Boolean! = false
}

input CreateContactInput {
  type: ContactType!
  value: String!
}

input PersonFilter {
  name: String
  email: String
  city: String
  state: String
}

type PersonPage {
  content: [Person!]!
  totalElements: Int!
  totalPages: Int!
  page: Int!
  size: Int!
}

type Query {
  person(id: ID!): Person
  persons(filter: PersonFilter, page: Int = 0, size: Int = 20): PersonPage!
  searchPersons(query: String!): [Person!]!
}

type Mutation {
  createPerson(input: CreatePersonInput!): Person!
  updatePerson(id: ID!, input: CreatePersonInput!): Person!
  deletePerson(id: ID!): Boolean!
  addAddress(personId: ID!, input: CreateAddressInput!): Address!
  addContact(personId: ID!, input: CreateContactInput!): Contact!
}

type Subscription {
  personCreated: Person!
  personUpdated: Person!
  personDeleted: ID!
}
```

### Exemplos de Query

```graphql
# Buscar person com endereços e contatos
query {
  person(id: 1) {
    name
    email
    addresses {
      street
      city
    }
    contacts {
      type
      value
    }
  }
}

# Listar com filtro e paginação
query {
  persons(filter: { city: "São Paulo" }, page: 0, size: 10) {
    content {
      id
      name
      email
    }
    totalElements
    totalPages
  }
}
```

### Exemplo de Mutation

```graphql
mutation {
  createPerson(input: {
    name: "Wesley Santos"
    email: "wesley@example.com"
    birthDate: "1991-01-15"
    addresses: [{
      street: "Praça da Sé"
      city: "São Paulo"
      state: "SP"
      zipCode: "01001-000"
      primary: true
    }]
  }) {
    id
    name
    addresses { city }
  }
}
```

---

## ✅ Critérios de Aceite

- [ ] Schema GraphQL completo (Person + Address + Contact)
- [ ] Queries com filtro e paginação cursor/offset
- [ ] Mutations com validação de input
- [ ] Subscriptions via WebSocket
- [ ] DataLoader para addresses e contacts (evitar N+1)
- [ ] Custom scalars (Date, DateTime)
- [ ] Error handling com extensions (field errors)
- [ ] GraphQL Playground/GraphiQL acessível
- [ ] Testes de integração para queries e mutations

---

## 🛠️ Implementar em

| Stack | Framework GraphQL |
|---|---|
| **Spring Boot** | Spring for GraphQL (`@QueryMapping`, `@MutationMapping`) |
| **Micronaut** | Micronaut GraphQL (graphql-java) |
| **Quarkus** | Quarkus SmallRye GraphQL (`@Query`, `@Mutation`) |
| **Go** | `gqlgen` (code-first) ou `graphql-go` |

---

## 💡 Dicas

### Spring Boot (Spring for GraphQL)
```java
@Controller
public class PersonGraphQLController {
    @QueryMapping
    public Person person(@Argument Long id) {
        return personService.findById(id);
    }

    @MutationMapping
    public Person createPerson(@Argument CreatePersonInput input) {
        return personService.create(input);
    }

    @BatchMapping
    public Map<Person, List<Address>> addresses(List<Person> persons) {
        return addressService.findByPersons(persons); // DataLoader!
    }

    @SubscriptionMapping
    public Flux<Person> personCreated() {
        return personService.personCreatedStream();
    }
}
```

### Go (gqlgen)
```go
// schema.resolvers.go (auto-generated)
func (r *queryResolver) Person(ctx context.Context, id string) (*model.Person, error) {
    return r.PersonRepo.FindByID(id)
}

// dataloader
func (r *personResolver) Addresses(ctx context.Context, obj *model.Person) ([]*model.Address, error) {
    return dataloader.For(ctx).AddressesByPersonID.Load(obj.ID)
}
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `spring-boot-starter-graphql` |
| Micronaut | `micronaut-graphql` |
| Quarkus | `quarkus-smallrye-graphql` |
| Go | `github.com/99designs/gqlgen` |

---

## 🔗 Referências

- [GraphQL Specification](https://spec.graphql.org/)
- [Spring for GraphQL](https://spring.io/projects/spring-graphql)
- [gqlgen (Go)](https://gqlgen.com/)
- [DataLoader Pattern](https://github.com/graphql/dataloader)
- [GraphQL Best Practices](https://graphql.org/learn/best-practices/)
