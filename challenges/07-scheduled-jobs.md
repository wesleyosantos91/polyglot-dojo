# 🏆 Desafio 08 — Scheduled Jobs (Rotinas)

> **Nível:** ⭐⭐ Intermediário
> **Tipo:** Scheduling · Cron · Background Tasks · Monitoring
> **Estimativa:** 4–6 horas por stack

---

## 📋 Descrição

Criar um serviço com **rotinas agendadas** (cron jobs) que executam tarefas periódicas: limpeza de dados expirados, sincronização com API externa, geração de relatórios e health checks.

---

## 🎯 Objetivos de Aprendizado

- [ ] Agendamento com expressões Cron
- [ ] Fixed rate vs fixed delay
- [ ] Controle de concorrência (evitar execuções sobrepostas)
- [ ] Distributed locking (para múltiplas instâncias)
- [ ] Logging e monitoramento de execuções
- [ ] Timezone-aware scheduling
- [ ] Graceful shutdown de tasks em andamento

---

## 📐 Especificação

### Rotinas a Implementar

| # | Job | Cron | Descrição |
|---|---|---|---|
| 1 | `CleanExpiredTokens` | `0 0 * * * *` (toda hora) | Remove tokens de sessão expirados |
| 2 | `SyncExternalAPI` | `0 */5 * * * *` (a cada 5 min) | Busca dados de API externa e atualiza |
| 3 | `DailyReport` | `0 0 6 * * *` (6h diária) | Gera relatório diário de cadastros |
| 4 | `HealthCheck` | `0 */30 * * * *` (a cada 30s — fixed rate) | Verifica saúde dos serviços dependentes |
| 5 | `DataArchive` | `0 0 2 1 * *` (2h, dia 1, mensal) | Arquiva dados antigos (> 1 ano) |

### Tabela: job_executions

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | Long | PK |
| `job_name` | String | Nome do job |
| `status` | Enum | `RUNNING`, `COMPLETED`, `FAILED` |
| `started_at` | Timestamp | Início |
| `finished_at` | Timestamp | Fim |
| `duration_ms` | Long | Duração |
| `records_processed` | Int | Registros processados |
| `error_message` | String | Mensagem de erro (se falhou) |
| `instance_id` | String | Hostname/ID da instância |

### API de Monitoramento

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/jobs` | Lista todos os jobs configurados |
| `GET` | `/api/jobs/executions` | Histórico de execuções |
| `GET` | `/api/jobs/{name}/last` | Última execução do job |
| `POST` | `/api/jobs/{name}/trigger` | Execução manual imediata |

### Response

```json
{
  "job_name": "CleanExpiredTokens",
  "status": "COMPLETED",
  "started_at": "2026-03-04T10:00:00Z",
  "finished_at": "2026-03-04T10:00:02Z",
  "duration_ms": 2340,
  "records_processed": 156,
  "next_execution": "2026-03-04T11:00:00Z"
}
```

---

## ✅ Critérios de Aceite

- [ ] 5 jobs agendados rodando conforme cron
- [ ] Cada execução registrada na tabela `job_executions`
- [ ] Não permitir execução concorrente do mesmo job
- [ ] API de monitoramento funcional
- [ ] Trigger manual via endpoint
- [ ] Graceful shutdown (jobs em andamento completam)
- [ ] Distributed lock (Shedlock / DB-based)
- [ ] Logs estruturados com job context

---

## 🛠️ Implementar em

| Stack | Abordagem |
|---|---|
| **Spring Boot** | `@Scheduled` + `@SchedulerLock` (ShedLock) |
| **Micronaut** | `@Scheduled` + custom locking |
| **Quarkus** | `@Scheduled` + `@ConcurrentExecution(SKIP)` |
| **Go** | `robfig/cron` + `sync.Mutex` / DB lock |

---

## 💡 Dicas

### Spring Boot
```java
@Scheduled(cron = "0 0 * * * *")
@SchedulerLock(name = "CleanExpiredTokens", lockAtMostFor = "55m")
public void cleanExpiredTokens() {
    var execution = startExecution("CleanExpiredTokens");
    try {
        int count = tokenRepository.deleteExpired();
        completeExecution(execution, count);
    } catch (Exception e) {
        failExecution(execution, e);
    }
}
```

### Quarkus
```java
@Scheduled(cron = "0 0 * * * *", identity = "clean-tokens")
@ConcurrentExecution(ConcurrentExecution.Strategy.SKIP)
void cleanExpiredTokens() {
    // processar
}
```

### Go
```go
c := cron.New(cron.WithSeconds())
c.AddFunc("0 0 * * * *", func() {
    mu.Lock()
    defer mu.Unlock()
    cleanExpiredTokens(db)
})
c.Start()
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `shedlock-spring` + `shedlock-provider-jdbc-template` |
| Micronaut | (built-in `@Scheduled`) |
| Quarkus | `quarkus-scheduler` |
| Go | `github.com/robfig/cron/v3` |

---

## 🔗 Referências

- [Cron Expressions](https://crontab.guru/)
- [ShedLock](https://github.com/lukas-krecan/ShedLock)
- [Quarkus Scheduler](https://quarkus.io/guides/scheduler)
- [robfig/cron](https://github.com/robfig/cron)
