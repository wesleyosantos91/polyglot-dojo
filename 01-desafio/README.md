# 🚧 Desafio 01 - Person CRUD

Implementacao pratica do desafio [01-crud-rest-api](../challenges/01-crud-rest-api.md) em 4 stacks, com infraestrutura local e observabilidade.

## Objetivo

Construir e comparar o mesmo CRUD de `Person` nas stacks:

- [Spring Boot](./api-person-spring/README.md)
- [Micronaut](./api-person-micronaut/README.md)
- [Quarkus](./api-person-quarkus/README.md)
- [Go + Gin](./api-person-go-gin/README.md)

## Estrutura desta pasta

```text
01-desafio/
├── api-person-spring/
├── api-person-micronaut/
├── api-person-quarkus/
├── api-person-go-gin/
└── infra/
    ├── README.md
    ├── docker/
    └── performance/
```

## Infraestrutura local

Documentacao consolidada:

- [Infra - visão geral](./infra/README.md)
- [Infra Docker - compose e serviços](./infra/docker/README.md)
- [Performance k6](./infra/performance/k6/README.md)

Subir stack local (PostgreSQL + observabilidade + API Spring):

```bash
cd infra/docker
docker compose up -d --build api-person-spring
```

## Como validar o desafio

1. Subir infraestrutura em `infra/docker`.
2. Executar os endpoints CRUD (`GET`, `POST`, `PUT`, `DELETE`) em `/api/persons`.
3. Rodar testes de cada stack (README de cada projeto).
4. Rodar smoke/load/stress com k6 em `infra/performance/k6`.

## Referências

- [README raiz do repositório](../README.md)
- [Challenge index](../challenges/README.md)
- [Desafio 01](../challenges/01-crud-rest-api.md)
