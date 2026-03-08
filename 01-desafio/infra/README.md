# 🧱 Infra local do Desafio 01

Esta pasta concentra a infraestrutura para executar e observar o CRUD `Person` localmente.

## Conteúdo

- [`docker/`](./docker/README.md): `docker-compose`, banco, observabilidade e API Spring
- [`performance/`](./performance/k6/README.md): testes de carga com k6

## Subir stack

```bash
cd docker
docker compose up -d --build api-person-spring
```

## Desligar stack

```bash
cd docker
docker compose down
```

## Endpoints úteis

- API Spring: `http://localhost:8080`
- Actuator health: `http://localhost:8080/actuator/health`
- Grafana: `http://localhost:3000` (`admin` / `admin`)
- Prometheus: `http://localhost:9090`
- Jaeger: `http://localhost:16686`
- PostgreSQL: `localhost:5432` (db `dev`, user `postgres`, password `postgres`)

## Fluxo recomendado

1. Subir a infra em `docker/`.
2. Validar saúde da API.
3. Executar carga em `performance/k6`.
