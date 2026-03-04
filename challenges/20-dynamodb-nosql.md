# 🏆 Desafio 20 — DynamoDB & NoSQL Patterns

> **Nível:** ⭐⭐⭐ Avançado
> **Tipo:** NoSQL · AWS DynamoDB · Single-Table Design · Streams
> **Estimativa:** 8–12 horas por stack

---

## 📋 Descrição

Criar uma API de **catálogo de produtos** usando **AWS DynamoDB** como banco principal, aplicando patterns de modelagem NoSQL: **Single-Table Design**, **Global Secondary Indexes (GSI)**, **DynamoDB Streams** para reação a eventos, e **DAX** para cache. Ambiente local com **LocalStack**.

---

## 🎯 Objetivos de Aprendizado

- [ ] Modelagem NoSQL — Single-Table Design (pk/sk patterns)
- [ ] Global Secondary Indexes (GSI) para queries alternativas
- [ ] Local Secondary Indexes (LSI)
- [ ] DynamoDB Streams para reagir a alterações
- [ ] Operações em batch (`BatchWriteItem`, `BatchGetItem`)
- [ ] Conditional writes (optimistic locking com `version`)
- [ ] TTL automático para dados temporários
- [ ] Pagination com `ExclusiveStartKey`
- [ ] Transaction (`TransactWriteItems`)
- [ ] Cache com DAX (DynamoDB Accelerator)
- [ ] LocalStack para desenvolvimento local

---

## 📐 Especificação

### Domínio: Catálogo de Produtos

**Entidades:** Product, Category, Review

### Single-Table Design

| Entity | PK | SK | Exemplo |
|---|---|---|---|
| Product | `PRODUCT#<id>` | `METADATA` | Dados do produto |
| Category | `CATEGORY#<name>` | `METADATA` | Dados da categoria |
| Product-Category | `CATEGORY#<name>` | `PRODUCT#<id>` | Produto na categoria |
| Review | `PRODUCT#<id>` | `REVIEW#<timestamp>#<user>` | Review do produto |

### GSI-1: Busca por status

| GSI1PK | GSI1SK | Uso |
|---|---|---|
| `STATUS#ACTIVE` | `<created_at>` | Listar produtos ativos por data |
| `STATUS#INACTIVE` | `<created_at>` | Listar inativos |

### GSI-2: Busca por preço

| GSI2PK | GSI2SK | Uso |
|---|---|---|
| `CATEGORY#<name>` | `PRICE#<price>` | Produtos por categoria ordenados por preço |

### Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/products` | Criar produto (transaction: item + category) |
| `GET` | `/api/products/{id}` | Buscar por ID |
| `GET` | `/api/products?category=X` | Listar por categoria (GSI-2) |
| `GET` | `/api/products?status=active&page=X` | Listar ativos paginado (GSI-1) |
| `PUT` | `/api/products/{id}` | Update com conditional write (version) |
| `DELETE` | `/api/products/{id}` | Soft delete (muda status) |
| `POST` | `/api/products/{id}/reviews` | Adicionar review |
| `GET` | `/api/products/{id}/reviews` | Listar reviews (query por PK + SK begins_with) |
| `POST` | `/api/products/batch` | Import batch de produtos |
| `GET` | `/api/products/search?q=X` | Full scan com filter (demonstrar limitação) |

### DynamoDB Stream Processing

```
Product alterado → DynamoDB Stream → Lambda/Consumer
  ├── Se ACTIVE→INACTIVE: Notificar admin
  ├── Se novo review: Recalcular rating médio
  └── Se preço alterado: Registrar histórico de preços
```

---

## ✅ Critérios de Aceite

- [ ] Single-Table Design com PK/SK patterns
- [ ] 2 GSIs funcionando (status + preço)
- [ ] Pagination com cursor (`ExclusiveStartKey`)
- [ ] Conditional write com `version` (optimistic locking)
- [ ] Transaction para criar produto + link categoria
- [ ] Batch write para import
- [ ] TTL configurado para reviews (90 dias)
- [ ] DynamoDB Stream processando alterações
- [ ] LocalStack para testes locais
- [ ] Teste de integração com Testcontainers (LocalStack)

---

## 🛠️ Implementar em

| Stack | Abordagem |
|---|---|
| **Spring Boot** | `software.amazon.awssdk:dynamodb-enhanced` + `@DynamoDbBean` |
| **Micronaut** | `micronaut-aws-sdk-v2` + DynamoDB Enhanced Client |
| **Quarkus** | `quarkus-amazon-dynamodb-enhanced` + CDI |
| **Go** | `aws-sdk-go-v2/service/dynamodb` + `attributevalue` marshal |

---

## 💡 Dicas

### Spring Boot
```java
@DynamoDbBean
public class CatalogItem {
    private String pk;
    private String sk;
    private String gsi1pk;
    private String gsi1sk;
    private String name;
    private BigDecimal price;
    private Long version;
    
    @DynamoDbPartitionKey
    public String getPk() { return pk; }
    
    @DynamoDbSortKey
    public String getSk() { return sk; }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "GSI1")
    public String getGsi1pk() { return gsi1pk; }
}
```

### Go
```go
type CatalogItem struct {
    PK      string  `dynamodbav:"PK"`
    SK      string  `dynamodbav:"SK"`
    GSI1PK  string  `dynamodbav:"GSI1PK"`
    GSI1SK  string  `dynamodbav:"GSI1SK"`
    Name    string  `dynamodbav:"name"`
    Price   float64 `dynamodbav:"price"`
    Version int     `dynamodbav:"version"`
}

func (r *Repository) PutProduct(ctx context.Context, item CatalogItem) error {
    av, _ := attributevalue.MarshalMap(item)
    _, err := r.client.PutItem(ctx, &dynamodb.PutItemInput{
        TableName: aws.String("catalog"),
        Item:      av,
        ConditionExpression: aws.String("attribute_not_exists(PK) OR version = :v"),
        ExpressionAttributeValues: map[string]types.AttributeValue{
            ":v": &types.AttributeValueMemberN{Value: strconv.Itoa(item.Version - 1)},
        },
    })
    return err
}
```

---

## 🐳 Docker Compose

```yaml
services:
  localstack:
    image: localstack/localstack:4
    ports:
      - "4566:4566"
    environment:
      SERVICES: dynamodb,dynamodb-streams
      DEFAULT_REGION: us-east-1
    volumes:
      - ./init-dynamodb.sh:/etc/localstack/init/ready.d/init.sh

  app:
    build: .
    environment:
      AWS_ENDPOINT: http://localstack:4566
      AWS_REGION: us-east-1
      AWS_ACCESS_KEY_ID: test
      AWS_SECRET_ACCESS_KEY: test
    depends_on:
      - localstack
```

### init-dynamodb.sh
```bash
#!/bin/bash
awslocal dynamodb create-table \
  --table-name catalog \
  --attribute-definitions \
    AttributeName=PK,AttributeType=S \
    AttributeName=SK,AttributeType=S \
    AttributeName=GSI1PK,AttributeType=S \
    AttributeName=GSI1SK,AttributeType=S \
    AttributeName=GSI2PK,AttributeType=S \
    AttributeName=GSI2SK,AttributeType=S \
  --key-schema AttributeName=PK,KeyType=HASH AttributeName=SK,KeyType=RANGE \
  --global-secondary-indexes \
    '[{"IndexName":"GSI1","KeySchema":[{"AttributeName":"GSI1PK","KeyType":"HASH"},{"AttributeName":"GSI1SK","KeyType":"RANGE"}],"Projection":{"ProjectionType":"ALL"}},
      {"IndexName":"GSI2","KeySchema":[{"AttributeName":"GSI2PK","KeyType":"HASH"},{"AttributeName":"GSI2SK","KeyType":"RANGE"}],"Projection":{"ProjectionType":"ALL"}}]' \
  --billing-mode PAY_PER_REQUEST \
  --stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES
```

---

## 📦 Dependências

| Stack | Dependência |
|---|---|
| Spring | `software.amazon.awssdk:dynamodb-enhanced` |
| Micronaut | `micronaut-aws-sdk-v2`, `dynamodb-enhanced` |
| Quarkus | `quarkus-amazon-dynamodb-enhanced` |
| Go | `github.com/aws/aws-sdk-go-v2/service/dynamodb` |

---

## 🔗 Referências

- [DynamoDB Single-Table Design](https://www.alexdebrie.com/posts/dynamodb-single-table/)
- [Best Practices for DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html)
- [DynamoDB Streams](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Streams.html)
- [LocalStack DynamoDB](https://docs.localstack.cloud/user-guide/aws/dynamodb/)
