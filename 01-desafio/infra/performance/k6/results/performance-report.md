# Relatorio de Performance k6

Gerado em: 2026-03-09 00:20:39 -03:00

## Ambiente e comando

- Base URL: `http://host.docker.internal:8080`
- Runner: `grafana/k6` via Docker
- Script: `infra/performance/k6/persons-workload.js`

## Resumo geral

| Cenario | Status | Requisicoes | Iteracoes | VUs max | Falha HTTP | Checks | p95 (ms) | p99 (ms) |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| smoke | PASSOU | 184 | 147 | 1 | 0,00% | 100,00% | 5,53 | 13,10 |
| load | PASSOU | 14237 | 11840 | 10 | 0,00% | 100,00% | 6,19 | 12,26 |
| stress | PASSOU | 111768 | 92741 | 60 | 0,03% | 99,96% | 3,65 | 4,37 |

## Validacao de thresholds

| Cenario | checks | http_req_failed | p95 global | p99 global | p95 list | p95 getById | p95 create |
|---|---|---|---|---|---|---|---|
| smoke | OK (100,00% > 99,00%) | OK (0,00% < 1,00%) | OK (5,53 < 500) | OK (13,10 < 1200) | OK (3,84 < 400) | OK (3,49 < 400) | OK (13,31 < 700) |
| load | OK (100,00% > 98,00%) | OK (0,00% < 2,00%) | OK (6,19 < 350) | OK (12,26 < 900) | OK (5,35 < 400) | OK (4,17 < 400) | OK (12,96 < 700) |
| stress | OK (99,96% > 95,00%) | OK (0,03% < 5,00%) | OK (3,65 < 900) | OK (4,37 < 2000) | OK (3,30 < 400) | OK (2,77 < 400) | OK (4,57 < 700) |

## Arquivos de resultado

- `infra/performance/k6/results/smoke-summary.json`
- `infra/performance/k6/results/load-summary.json`
- `infra/performance/k6/results/stress-summary.json`
- `infra/performance/k6/results/performance-report.md`

## Observacoes

- No cenario `stress` houve falhas pontuais (`dial: i/o timeout`), mas a taxa final de erro ficou dentro do limite configurado (< 5%).
- O campo interno `thresholds` do export JSON do k6 pode aparecer inconsistente; neste relatorio o status foi calculado pelos valores medidos e limites do script.
