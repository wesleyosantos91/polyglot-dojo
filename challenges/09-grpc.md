# 🏆 Desafio 10 — gRPC Server & Client

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** gRPC · Protocol Buffers · Streaming · Service-to-Service
> **Estimativa:** 8–10 horas por stack

---

## 📋 Descrição

Criar um serviço **gRPC** para gerenciamento de Person com os 4 tipos de comunicação gRPC: Unary, Server Streaming, Client Streaming e Bidirectional Streaming. Implementar também um **gRPC Gateway** que expõe os endpoints como REST/JSON.

---

## 🎯 Objetivos de Aprendizado

- [ ] Protocol Buffers (protobuf) — definição de schema
- [ ] gRPC Unary RPC (request/response simples)
- [ ] Server Streaming (servidor envia stream de dados)
- [ ] Client Streaming (cliente envia stream de dados)
- [ ] Bidirectional Streaming (ambos enviam streams)
- [ ] gRPC interceptors (logging, auth, metrics)
- [ ] gRPC error handling (status codes)
- [ ] gRPC ↔ REST transcoding (gateway)
- [ ] Health check protocol (gRPC Health v1)

---

## 📐 Especificação

### Proto Definition

```protobuf
syntax = "proto3";

package person.v1;

option java_package = "io.github.wesleyosantos91.grpc";
option go_package = "github.com/wesleyosantos91/api-person-go-gin/proto";

import "google/protobuf/timestamp.proto";
import "google/protobuf/empty.proto";

// ===== Messages =====

message Person {
  int64 id = 1;
  string name = 2;
  string email = 3;
  string birth_date = 4;
  google.protobuf.Timestamp created_at = 5;
  google.protobuf.Timestamp updated_at = 6;
}

message CreatePersonRequest {
  string name = 1;
  string email = 2;
  string birth_date = 3;
}

message UpdatePersonRequest {
  int64 id = 1;
  string name = 2;
  string email = 3;
  string birth_date = 4;
}

message GetPersonRequest {
  int64 id = 1;
}

message DeletePersonRequest {
  int64 id = 1;
}

message ListPersonsRequest {
  int32 page = 1;
  int32 size = 2;
}

message ListPersonsResponse {
  repeated Person persons = 1;
  int32 total = 2;
  int32 page = 3;
}

message PersonFilter {
  optional string name = 1;
  optional string email = 2;
}

// ===== Service =====

service PersonService {
  // Unary RPCs
  rpc CreatePerson(CreatePersonRequest) returns (Person);
  rpc GetPerson(GetPersonRequest) returns (Person);
  rpc UpdatePerson(UpdatePersonRequest) returns (Person);
  rpc DeletePerson(DeletePersonRequest) returns (google.protobuf.Empty);
  rpc ListPersons(ListPersonsRequest) returns (ListPersonsResponse);

  // Server Streaming — retorna stream de persons filtrados
  rpc SearchPersons(PersonFilter) returns (stream Person);

  // Client Streaming — recebe stream de persons para criar em batch
  rpc BatchCreatePersons(stream CreatePersonRequest) returns (BatchResult);

  // Bidirectional Streaming — sync em tempo real
  rpc SyncPersons(stream SyncRequest) returns (stream SyncResponse);
}

message BatchResult {
  int32 created = 1;
  int32 failed = 2;
  repeated string errors = 3;
}

message SyncRequest {
  oneof action {
    CreatePersonRequest create = 1;
    UpdatePersonRequest update = 2;
    DeletePersonRequest delete = 3;
  }
}

message SyncResponse {
  string action = 1;
  Person person = 2;
  string error = 3;
}
```

### gRPC Status Codes Esperados

| Situação | Status Code |
|---|---|
| Sucesso | `OK` (0) |
| Person não encontrada | `NOT_FOUND` (5) |
| Validação falhou | `INVALID_ARGUMENT` (3) |
| Email duplicado | `ALREADY_EXISTS` (6) |
| Erro interno | `INTERNAL` (13) |

---

## ✅ Critérios de Aceite

- [ ] Proto file compilando para todas as 4 stacks
- [ ] 4 Unary RPCs (Create, Get, Update, Delete) + List
- [ ] Server Streaming (SearchPersons) funcionando
- [ ] Client Streaming (BatchCreatePersons) funcionando
- [ ] Bidirectional Streaming (SyncPersons) funcionando
- [ ] Interceptors de logging em todas as chamadas
- [ ] gRPC Health Check implementado
- [ ] gRPC Reflection habilitado (para `grpcurl`)
- [ ] Teste com `grpcurl` documentado
- [ ] Cliente gRPC cross-stack (ex: Go client → Java server)

---

## 🛠️ Implementar em

| Stack | Framework gRPC | Observações |
|---|---|---|
| **Spring Boot** | `grpc-spring-boot-starter` | Auto-config, `@GrpcService` |
| **Micronaut** | `micronaut-grpc` | `@GrpcService`, protobuf plugin |
| **Quarkus** | `quarkus-grpc` | `@GrpcService`, Mutiny streams |
| **Go** | `google.golang.org/grpc` | Nativo, `protoc-gen-go-grpc` |

---

## 💡 Dicas

### Testando com grpcurl

```bash
# Listar serviços (reflection)
grpcurl -plaintext localhost:9090 list

# Unary — Criar Person
grpcurl -plaintext -d '{"name":"Wesley","email":"w@x.com","birth_date":"1991-01-15"}' \
  localhost:9090 person.v1.PersonService/CreatePerson

# Server Streaming — Buscar
grpcurl -plaintext -d '{"name":"Wes"}' \
  localhost:9090 person.v1.PersonService/SearchPersons
```

### Spring Boot
```java
@GrpcService
public class PersonGrpcService extends PersonServiceGrpc.PersonServiceImplBase {
    @Override
    public void getPerson(GetPersonRequest request, 
                          StreamObserver<Person> response) {
        var person = repository.findById(request.getId());
        response.onNext(toProto(person));
        response.onCompleted();
    }
}
```

### Go
```go
type personServer struct {
    pb.UnimplementedPersonServiceServer
    repo *repository.PersonRepository
}

func (s *personServer) GetPerson(ctx context.Context, 
    req *pb.GetPersonRequest) (*pb.Person, error) {
    person, err := s.repo.FindByID(uint(req.Id))
    if err != nil {
        return nil, status.Errorf(codes.NotFound, "person not found")
    }
    return toProto(person), nil
}
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `net.devh:grpc-spring-boot-starter` |
| Micronaut | `micronaut-grpc-server-runtime` |
| Quarkus | `quarkus-grpc` |
| Go | `google.golang.org/grpc` + `google.golang.org/protobuf` |

---

## 🔗 Referências

- [gRPC Documentation](https://grpc.io/docs/)
- [Protocol Buffers Language Guide](https://protobuf.dev/programming-guides/proto3/)
- [gRPC Status Codes](https://grpc.github.io/grpc/core/md_doc_statuscodes.html)
- [grpcurl](https://github.com/fullstorydev/grpcurl)
- [gRPC Health Checking](https://github.com/grpc/grpc/blob/master/doc/health-checking.md)
