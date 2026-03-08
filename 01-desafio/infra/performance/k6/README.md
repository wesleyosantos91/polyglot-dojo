# k6 - Testes de Carga

Script principal: `persons-workload.js`

## Perfis

- `smoke`: validacao rapida de estabilidade (1 VU por 30s)
- `load`: carga sustentada para baseline de capacidade
- `stress`: aumento progressivo para identificar limite

## Pre-requisitos

- API em execucao e acessivel pelo `BASE_URL`
- Banco de dados disponivel (PostgreSQL)
- k6 instalado localmente ou via Docker
- Execute os comandos a partir da pasta `01-desafio/infra`

## Execucao local

```bash
k6 run performance/k6/persons-workload.js -e BASE_URL=http://localhost:8080 -e TEST_TYPE=smoke
k6 run performance/k6/persons-workload.js -e BASE_URL=http://localhost:8080 -e TEST_TYPE=load
k6 run performance/k6/persons-workload.js -e BASE_URL=http://localhost:8080 -e TEST_TYPE=stress
```

## Execucao via Docker (sem instalar k6)

```bash
docker run --rm -i \
  -v "${PWD}:/work" \
  -w /work \
  grafana/k6 run performance/k6/persons-workload.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e TEST_TYPE=load
```

## Variaveis de ambiente

- `BASE_URL` (default: `http://localhost:8080`)
- `TEST_TYPE` (`smoke`, `load`, `stress`; default: `load`)
- `SEED_COUNT` (override da massa inicial)
- `THINK_TIME_SECONDS` (default: `0.2`)
- `SKIP_TEARDOWN=true` para manter dados criados
- `DEBUG=true` para logar respostas de erro

## Exportar resumo

```bash
k6 run performance/k6/persons-workload.js \
  -e BASE_URL=http://localhost:8080 \
  -e TEST_TYPE=load \
  --summary-export=performance/k6/results/load-summary.json
```
