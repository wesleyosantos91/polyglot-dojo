# 🏆 Desafio 17 — Full-Text Search com Elasticsearch

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** Search · Elasticsearch · Indexing · Autocomplete
> **Estimativa:** 7–9 horas por stack

---

## 📋 Descrição

Adicionar **busca textual avançada** à API Person usando Elasticsearch. Implementar busca por nome/email com fuzzy matching, autocomplete, filtros combinados e highlighting dos termos encontrados.

---

## 🎯 Objetivos de Aprendizado

- [ ] Elasticsearch index mapping e analyzers
- [ ] Full-text search vs keyword search
- [ ] Fuzzy matching (tolerância a typos)
- [ ] Autocomplete com edge_ngram
- [ ] Filtros combinados (bool query)
- [ ] Highlight de termos encontrados
- [ ] Paginação e sorting
- [ ] Sync entre PostgreSQL e Elasticsearch

---

## 📐 Especificação

### Elasticsearch Index: `persons`

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "name": { 
        "type": "text",
        "analyzer": "brazilian",
        "fields": {
          "autocomplete": { "type": "text", "analyzer": "autocomplete" },
          "keyword": { "type": "keyword" }
        }
      },
      "email": {
        "type": "text",
        "fields": { "keyword": { "type": "keyword" } }
      },
      "birth_date": { "type": "date", "format": "yyyy-MM-dd" },
      "city": { "type": "keyword" },
      "state": { "type": "keyword" },
      "created_at": { "type": "date" }
    }
  },
  "settings": {
    "analysis": {
      "analyzer": {
        "autocomplete": {
          "tokenizer": "autocomplete_tokenizer",
          "filter": ["lowercase"]
        }
      },
      "tokenizer": {
        "autocomplete_tokenizer": {
          "type": "edge_ngram",
          "min_gram": 2,
          "max_gram": 10
        }
      }
    }
  }
}
```

### Endpoints de Busca

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/search/persons?q=wesley` | Full-text search |
| `GET` | `/api/search/persons/autocomplete?q=wes` | Autocomplete |
| `GET` | `/api/search/persons/advanced` | Busca com filtros |
| `POST` | `/api/search/reindex` | Re-indexa todos do banco |

### Query Parameters (advanced)

| Param | Exemplo | Descrição |
|---|---|---|
| `q` | `wesley santos` | Termo de busca |
| `name` | `wesley` | Filtro por nome |
| `email` | `@gmail.com` | Filtro por email |
| `city` | `São Paulo` | Filtro por cidade |
| `state` | `SP` | Filtro por estado |
| `from_date` | `1990-01-01` | Data nascimento mín. |
| `to_date` | `2000-12-31` | Data nascimento máx. |
| `sort` | `name.keyword:asc` | Ordenação |
| `page` | `0` | Página |
| `size` | `20` | Itens por página |

### Response (com highlight)

```json
{
  "total": 42,
  "page": 0,
  "size": 20,
  "results": [
    {
      "id": 1,
      "name": "Wesley Santos",
      "email": "wesley@example.com",
      "birth_date": "1991-01-15",
      "score": 8.5,
      "highlight": {
        "name": ["<em>Wesley</em> Santos"],
        "email": ["<em>wesley</em>@example.com"]
      }
    }
  ]
}
```

---

## ✅ Critérios de Aceite

- [ ] Full-text search com relevância (score)
- [ ] Fuzzy matching (typo tolerance: "weslye" → "Wesley")
- [ ] Autocomplete com edge_ngram
- [ ] Filtros combinados (bool query)
- [ ] Highlight nos resultados
- [ ] Paginação e sorting
- [ ] Sync após CRUD (create/update/delete no index)
- [ ] Re-index completo via endpoint
- [ ] Docker Compose com Elasticsearch
- [ ] Busca retornando < 100ms

---

## 🐳 Docker Compose

```yaml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.17.4
    ports:
      - "9200:9200"
    environment:
      discovery.type: single-node
      xpack.security.enabled: "false"
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
```

---

## 🛠️ Implementar em

| Stack | Client |
|---|---|
| **Spring Boot** | Spring Data Elasticsearch / Elasticsearch Java Client |
| **Micronaut** | Elasticsearch Java Client (REST API) |
| **Quarkus** | Quarkus Elasticsearch / Hibernate Search |
| **Go** | `olivere/elastic` ou `elastic/go-elasticsearch` |

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `spring-boot-starter-data-elasticsearch` |
| Micronaut | `co.elastic.clients:elasticsearch-java` |
| Quarkus | `quarkus-elasticsearch-rest-client` ou `quarkus-hibernate-search-orm-elasticsearch` |
| Go | `github.com/elastic/go-elasticsearch/v8` |

---

## 🔗 Referências

- [Elasticsearch Guide](https://www.elastic.co/guide/en/elasticsearch/reference/current/)
- [Elasticsearch Query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)
- [Spring Data Elasticsearch](https://spring.io/projects/spring-data-elasticsearch)
- [Hibernate Search (Quarkus)](https://quarkus.io/guides/hibernate-search-orm-elasticsearch)
