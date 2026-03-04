# 🏆 Desafio 07 — Batch Processing

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** Batch · ETL · CSV/JSON · Chunk Processing
> **Estimativa:** 8–10 horas por stack

---

## 📋 Descrição

Criar um serviço de **processamento em lote** que importa um arquivo CSV/JSON com milhares de registros de Person, valida, transforma e persiste no banco de dados. O processamento deve ser feito em **chunks** com controle de erros, retry e relatório final.

---

## 🎯 Objetivos de Aprendizado

- [ ] Processamento em chunks (não carregar tudo na memória)
- [ ] Padrão Reader → Processor → Writer
- [ ] Controle transacional por chunk
- [ ] Skip de registros inválidos (com threshold)
- [ ] Retry de chunks com erro transiente
- [ ] Job execution tracking (status, progresso)
- [ ] Agendamento de jobs
- [ ] Processamento paralelo (partitioning)

---

## 📐 Especificação

### Input: Arquivo CSV

```csv
name,email,birth_date
Wesley Santos,wesley@example.com,1991-01-15
Maria Silva,maria@example.com,1995-06-20
,invalid-email,not-a-date
João Souza,joao@example.com,1988-03-10
```

### API de Upload e Controle

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/batch/import` | Upload CSV, inicia job |
| `GET` | `/api/batch/jobs` | Lista execuções de jobs |
| `GET` | `/api/batch/jobs/{id}` | Status/progresso do job |
| `GET` | `/api/batch/jobs/{id}/errors` | Registros com erro |
| `POST` | `/api/batch/jobs/{id}/restart` | Re-executa job falhado |

### Response do Job

```json
{
  "job_id": "uuid",
  "status": "COMPLETED",
  "started_at": "2026-03-04T10:30:00Z",
  "finished_at": "2026-03-04T10:30:45Z",
  "total_records": 10000,
  "processed": 9850,
  "skipped": 150,
  "errors": [
    {
      "line": 3,
      "data": ",invalid-email,not-a-date",
      "reason": "name: must not be blank; email: invalid format"
    }
  ],
  "chunk_size": 100,
  "duration_ms": 45000
}
```

### Configuração do Batch

| Parâmetro | Valor | Descrição |
|---|---|---|
| `chunk-size` | 100 | Registros por chunk |
| `skip-limit` | 500 | Max erros antes de abortar |
| `retry-limit` | 3 | Retries por chunk |
| `retry-interval` | 2s | Intervalo entre retries |
| `thread-count` | 4 | Threads para processamento |

---

## ✅ Critérios de Aceite

- [ ] Upload de CSV via endpoint
- [ ] Processamento em chunks de 100
- [ ] Skip de registros inválidos (até 500)
- [ ] Retry de erros transientes (3x)
- [ ] Status em tempo real via GET endpoint
- [ ] Relatório de erros com linha e motivo
- [ ] Restart de jobs falhados
- [ ] Processamento de 10.000 registros em < 60s
- [ ] Teste com arquivos de diferentes tamanhos

---

## 🛠️ Implementar em

| Stack | Abordagem |
|---|---|
| **Spring Boot** | Spring Batch (`Job`, `Step`, `ItemReader/Processor/Writer`) |
| **Micronaut** | Custom implementation (Reader/Processor/Writer pattern) |
| **Quarkus** | JBeret (JSR 352) ou custom implementation |
| **Go** | Custom com goroutines + channels (pipeline pattern) |

---

## 💡 Dicas

### Spring Batch
```java
@Bean
public Job importPersonJob(Step importStep) {
    return new JobBuilder("importPersonJob", jobRepository)
        .start(importStep)
        .build();
}

@Bean
public Step importStep(ItemReader<PersonCsv> reader,
                       ItemProcessor<PersonCsv, Person> processor,
                       ItemWriter<Person> writer) {
    return new StepBuilder("importStep", jobRepository)
        .<PersonCsv, Person>chunk(100, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .faultTolerant()
        .skipLimit(500)
        .skip(ValidationException.class)
        .retryLimit(3)
        .retry(DataAccessException.class)
        .build();
}
```

### Go (Pipeline com channels)
```go
func pipeline(ctx context.Context, file io.Reader) {
    records := make(chan CsvRecord, 100)
    validated := make(chan Person, 100)
    
    // Stage 1: Reader
    go readCSV(file, records)
    
    // Stage 2: Processor (N workers)
    for i := 0; i < 4; i++ {
        go validate(records, validated)
    }
    
    // Stage 3: Writer (batch insert)
    go batchInsert(db, validated, 100) // chunks de 100
}
```

### Arquivo CSV para testes

```bash
# Gerar CSV com 10.000 registros
python3 -c "
import csv, random, string
with open('persons.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerow(['name','email','birth_date'])
    for i in range(10000):
        name = f'Person {i}'
        email = f'person{i}@test.com'
        date = f'{random.randint(1960,2005)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}'
        w.writerow([name, email, date])
"
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `spring-boot-starter-batch` |
| Micronaut | (custom — sem framework batch nativo) |
| Quarkus | `quarkus-jberet` ou custom |
| Go | `encoding/csv` + goroutines |

---

## 🔗 Referências

- [Spring Batch Reference](https://docs.spring.io/spring-batch/reference/)
- [JSR 352 — Batch Applications](https://jcp.org/en/jsr/detail?id=352)
- [Go Concurrency Patterns: Pipelines](https://go.dev/blog/pipelines)
