# 🏆 Desafio 01 — CRUD REST API

> **Nível:** ⭐ Iniciante
> **Tipo:** REST API · CRUD · ORM · Docker
> **Estimativa:** 4–6 horas por stack

---

## 📋 Descrição

Construir uma API REST completa para gerenciamento de **Person** com operações CRUD, persistência em PostgreSQL, OpenTelemetry e Docker multi-stage otimizado.

Este é o projeto base do workshop — já implementado nas 4 stacks como referência.

---

## 🎯 Objetivos de Aprendizado

- [ ] Estrutura de projeto idiomática em cada stack
- [ ] Mapeamento ORM (JPA/Hibernate/GORM)
- [ ] Validação de request/response
- [ ] Tratamento de erros e status HTTP corretos
- [ ] Docker multi-stage build otimizado
- [ ] JEP 483 AOT Cache (Java stacks)
- [ ] OpenTelemetry básico (traces)

---

## 📐 Especificação

### Entidade: Person

| Campo | Tipo | Regras |
|---|---|---|
| `id` | Long/uint | PK, auto-increment |
| `name` | String | obrigatório, max 100 chars |
| `email` | String | obrigatório, único, formato email |
| `birth_date` | Date | obrigatório |
| `created_at` | Timestamp | automático |
| `updated_at` | Timestamp | automático |

### Endpoints

| Método | Rota | Status Sucesso |
|---|---|---|
| `GET` | `/api/persons` | `200 OK` |
| `GET` | `/api/persons/{id}` | `200 OK` |
| `POST` | `/api/persons` | `201 Created` |
| `PUT` | `/api/persons/{id}` | `200 OK` |
| `DELETE` | `/api/persons/{id}` | `204 No Content` |

### Respostas de Erro

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Person not found with id: 99"
}
```

---

## ✅ Critérios de Aceite

- [ ] Todos os 5 endpoints funcionando
- [ ] Validação de campos com mensagens claras
- [ ] Status HTTP corretos (201, 204, 400, 404, 500)
- [ ] PostgreSQL como banco de dados
- [ ] Docker build funcional com imagem otimizada
- [ ] Testes unitários e/ou integração
- [ ] OpenTelemetry exportando traces

---

## 🛠️ Implementar em

| Stack | Referência |
|---|---|
| Spring Boot 4 | `api-person-spring/` |
| Micronaut 4 | `api-person-micronaut/` |
| Quarkus 3 | `api-person-quarkus/` |
| Go + Gin | `api-person-go-gin/` |

---

## 💡 Dicas

- **Spring**: Use `@RestController`, `JpaRepository`, `@Valid`
- **Micronaut**: Use `@Controller`, `JpaRepository`, `@Valid`
- **Quarkus**: Use `@Path`, `PanacheEntity`, `@Valid`
- **Go**: Use `gin.Context`, `gorm.DB`, struct tags `binding:"required"`

---

## 🔗 Referência

> Este desafio já está implementado em `api-person-*/`. Use como base para os próximos.
