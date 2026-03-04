# 🏆 Desafio 25 — SQL Puro vs ORM

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** Database · SQL · ORM · JDBC · Performance · Comparison
> **Estimativa:** 8–10 horas por stack

---

## 📋 Descrição

Implementar a **mesma API Person com duas abordagens de acesso a dados** lado a lado:

1. **SQL Puro** — queries escritas manualmente (JDBC, sqlx, jOOQ)
2. **ORM** — mapeamento objeto-relacional (JPA/Hibernate, GORM, Panache)

O objetivo é entender trade-offs de **performance**, **controle**, **legibilidade** e **manutenibilidade**, e saber quando usar cada abordagem.

---

## 🎯 Objetivos de Aprendizado

### SQL Puro
- [ ] JDBC puro + `PreparedStatement` (Spring/Micronaut)
- [ ] `JdbcTemplate` / `NamedParameterJdbcTemplate` (Spring)
- [ ] jOOQ — type-safe SQL DSL
- [ ] Migrations com Flyway/Liquibase
- [ ] Query complexa: JOINs, subqueries, CTEs, window functions
- [ ] Batch inserts performáticos
- [ ] Paginação manual (`LIMIT` + `OFFSET` vs cursor-based)

### ORM
- [ ] JPA/Hibernate — entity mapping, lazy/eager, N+1
- [ ] Quarkus Panache — Active Record + Repository pattern
- [ ] Micronaut Data — compile-time query generation
- [ ] GORM (Go) — soft delete, hooks, preload
- [ ] N+1 problem — detecção e resolução
- [ ] Bulk operations — performance de updates em massa
- [ ] Second-level cache

### Go Específico
- [ ] `database/sql` + `pgx` — driver nativo
- [ ] `sqlx` — extensão com struct scanning
- [ ] `squirrel` — query builder
- [ ] GORM — ORM completo
- [ ] Comparação: `sqlx` (SQL puro) vs GORM (ORM)

---

## 📐 Especificação

### Schema (Migrations com Flyway)

```sql
-- V1__create_persons.sql
CREATE TABLE persons (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    birth_date  DATE,
    status      VARCHAR(20) DEFAULT 'ACTIVE',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- V2__create_addresses.sql
CREATE TABLE addresses (
    id          BIGSERIAL PRIMARY KEY,
    person_id   BIGINT NOT NULL REFERENCES persons(id),
    street      VARCHAR(200),
    city        VARCHAR(100),
    state       CHAR(2),
    zip_code    VARCHAR(10),
    type        VARCHAR(20) DEFAULT 'HOME'
);

CREATE INDEX idx_addresses_person_id ON addresses(person_id);

-- V3__create_orders.sql
CREATE TABLE orders (
    id          BIGSERIAL PRIMARY KEY,
    person_id   BIGINT NOT NULL REFERENCES persons(id),
    total       NUMERIC(10,2) NOT NULL,
    status      VARCHAR(20) DEFAULT 'PENDING',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_person_id ON orders(person_id);
CREATE INDEX idx_orders_status ON orders(status);
```

### Endpoints — Duas implementações lado a lado

| Método | Rota SQL Puro | Rota ORM | Descrição |
|---|---|---|---|
| `GET` | `/api/sql/persons` | `/api/orm/persons` | Listar com paginação |
| `GET` | `/api/sql/persons/{id}` | `/api/orm/persons/{id}` | Buscar por ID |
| `POST` | `/api/sql/persons` | `/api/orm/persons` | Criar |
| `PUT` | `/api/sql/persons/{id}` | `/api/orm/persons/{id}` | Atualizar |
| `DELETE` | `/api/sql/persons/{id}` | `/api/orm/persons/{id}` | Deletar |
| `GET` | `/api/sql/persons/{id}/full` | `/api/orm/persons/{id}/full` | Person + addresses + orders (JOIN) |
| `GET` | `/api/sql/reports/top-buyers` | `/api/orm/reports/top-buyers` | Query complexa com aggregation |
| `POST` | `/api/sql/persons/batch` | `/api/orm/persons/batch` | Insert batch 1000 registros |
| `GET` | `/api/benchmark` | — | Compara tempos SQL vs ORM |

### Query Complexa: Top Buyers Report

```sql
-- Implementar com SQL puro E com ORM
WITH order_summary AS (
    SELECT
        p.id,
        p.name,
        p.email,
        COUNT(o.id) AS total_orders,
        SUM(o.total) AS total_spent,
        AVG(o.total) AS avg_order_value,
        MAX(o.created_at) AS last_order_date,
        RANK() OVER (ORDER BY SUM(o.total) DESC) AS spending_rank
    FROM persons p
    LEFT JOIN orders o ON o.person_id = p.id AND o.status = 'COMPLETED'
    WHERE p.status = 'ACTIVE'
    GROUP BY p.id, p.name, p.email
    HAVING COUNT(o.id) > 0
)
SELECT *
FROM order_summary
WHERE spending_rank <= 10
ORDER BY spending_rank;
```

### Benchmark Endpoint Response

```json
{
  "operation": "findAll_1000_records",
  "results": {
    "sql_pure": {
      "avg_ms": 12,
      "min_ms": 8,
      "max_ms": 45,
      "p95_ms": 18,
      "iterations": 100
    },
    "orm": {
      "avg_ms": 35,
      "min_ms": 15,
      "max_ms": 120,
      "p95_ms": 55,
      "iterations": 100
    },
    "ratio": "ORM is 2.9x slower"
  }
}
```

---

## ✅ Critérios de Aceite

- [ ] Duas implementações completas (SQL puro + ORM) lado a lado
- [ ] CRUD completo em ambas
- [ ] Query complexa com JOIN + aggregation + window function
- [ ] Batch insert 1000 registros (comparar tempo)
- [ ] N+1 problem demonstrado e resolvido no ORM
- [ ] Paginação implementada de 2 formas (offset vs cursor)
- [ ] Migrations com Flyway/Liquibase
- [ ] Benchmark endpoint comparando performance
- [ ] Testes de integração para ambas implementações
- [ ] README documentando trade-offs observados

---

## 🛠️ Implementar em

| Stack | SQL Puro | ORM |
|---|---|---|
| **Spring Boot** | `JdbcTemplate` + `RowMapper` ou jOOQ | Spring Data JPA + Hibernate |
| **Micronaut** | `JdbcOperations` + SQL nativo | Micronaut Data JPA / JDBC |
| **Quarkus** | `AgroalDataSource` + JDBC ou jOOQ | Hibernate ORM + Panache |
| **Go** | `pgx` + `sqlx` | GORM |

---

## 💡 Dicas

### Spring Boot — SQL Puro (JdbcTemplate)
```java
@Repository
public class PersonSqlRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public Optional<PersonFull> findByIdFull(Long id) {
        String sql = """
            SELECT p.*, a.id as addr_id, a.street, a.city,
                   o.id as order_id, o.total, o.status as order_status
            FROM persons p
            LEFT JOIN addresses a ON a.person_id = p.id
            LEFT JOIN orders o ON o.person_id = p.id
            WHERE p.id = :id
            """;

        Map<Long, PersonFull> results = new HashMap<>();
        jdbc.query(sql, Map.of("id", id), rs -> {
            results.computeIfAbsent(rs.getLong("id"), k -> mapPerson(rs))
                   .addAddress(mapAddress(rs))
                   .addOrder(mapOrder(rs));
        });
        return Optional.ofNullable(results.get(id));
    }

    public int[] batchInsert(List<Person> persons) {
        return jdbc.batchUpdate(
            "INSERT INTO persons (name, email, birth_date) VALUES (:name, :email, :birthDate)",
            SqlParameterSourceUtils.createBatch(persons)
        );
    }
}
```

### Spring Boot — ORM (JPA)
```java
@Entity
@Table(name = "persons")
public class PersonEntity {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;
    private String name;
    private String email;

    @OneToMany(mappedBy = "person", fetch = LAZY)
    private List<AddressEntity> addresses;

    @OneToMany(mappedBy = "person", fetch = LAZY)
    private List<OrderEntity> orders;
}

// N+1 fix: EntityGraph
@EntityGraph(attributePaths = {"addresses", "orders"})
Optional<PersonEntity> findWithDetailsById(Long id);
```

### Go — SQL Puro (sqlx)
```go
type PersonSQLRepo struct {
    db *sqlx.DB
}

func (r *PersonSQLRepo) FindByIDFull(ctx context.Context, id int64) (*PersonFull, error) {
    query := `
        SELECT p.id, p.name, p.email,
               a.id as "address.id", a.street as "address.street",
               o.id as "order.id", o.total as "order.total"
        FROM persons p
        LEFT JOIN addresses a ON a.person_id = p.id
        LEFT JOIN orders o ON o.person_id = p.id
        WHERE p.id = $1`

    rows, err := r.db.QueryxContext(ctx, query, id)
    // ... manual mapping
}

func (r *PersonSQLRepo) BatchInsert(ctx context.Context, persons []Person) error {
    tx, _ := r.db.BeginTxx(ctx, nil)
    stmt, _ := tx.PreparexContext(ctx,
        "INSERT INTO persons (name, email, birth_date) VALUES ($1, $2, $3)")
    for _, p := range persons {
        stmt.ExecContext(ctx, p.Name, p.Email, p.BirthDate)
    }
    return tx.Commit()
}
```

### Go — ORM (GORM)
```go
type Person struct {
    ID        uint           `gorm:"primaryKey"`
    Name      string         `gorm:"size:100;not null"`
    Email     string         `gorm:"size:150;uniqueIndex"`
    Addresses []Address      `gorm:"foreignKey:PersonID"`
    Orders    []Order        `gorm:"foreignKey:PersonID"`
    CreatedAt time.Time
    DeletedAt gorm.DeletedAt `gorm:"index"` // soft delete
}

// N+1 fix: Preload
func (r *PersonGormRepo) FindByIDFull(id uint) (*Person, error) {
    var person Person
    err := r.db.Preload("Addresses").Preload("Orders").First(&person, id).Error
    return &person, err
}
```

---

## 📊 Trade-offs Esperados

| Critério | SQL Puro | ORM |
|---|---|---|
| **Performance** | ✅ Mais rápido (10-50%) | ❌ Overhead de mapeamento |
| **Controle** | ✅ Total controle da query | ❌ Generated SQL pode surpreender |
| **Produtividade** | ❌ Mais código boilerplate | ✅ CRUD automático |
| **Manutenibilidade** | ❌ SQL espalhado no código | ✅ Centralizado em entities |
| **Queries complexas** | ✅ Escrever SQL otimizado | ❌ Criteria API verbosa |
| **N+1 Problem** | ✅ Inexistente (controle manual) | ❌ Risco alto sem cuidado |
| **Type Safety** | ❌ Strings SQL | ✅ Compile-time (Micronaut Data) |
| **Portabilidade** | ❌ SQL pode ser DB-specific | ✅ HQL/JPQL abstrai |

---

## 📦 Dependências

| Stack | SQL Puro | ORM |
|---|---|---|
| Spring | `spring-boot-starter-jdbc`, `org.jooq:jooq` | `spring-boot-starter-data-jpa` |
| Micronaut | `micronaut-jdbc-hikari` | `micronaut-data-jpa` |
| Quarkus | `quarkus-agroal`, `quarkus-jdbc-postgresql` | `quarkus-hibernate-orm-panache` |
| Go | `github.com/jackc/pgx/v5`, `github.com/jmoiron/sqlx` | `gorm.io/gorm` |

---

## 🔗 Referências

- [Vlad Mihalcea — High-Performance Java Persistence](https://vladmihalcea.com/tutorials/hibernate/)
- [N+1 Problem Explained](https://vladmihalcea.com/n-plus-1-query-problem/)
- [jOOQ Documentation](https://www.jooq.org/doc/latest/)
- [sqlx Go Library](https://github.com/jmoiron/sqlx)
- [GORM Documentation](https://gorm.io/docs/)
