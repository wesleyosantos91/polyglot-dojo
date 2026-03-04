# 🏆 Desafio 13 — File Upload & S3 Integration

> **Nível:** ⭐⭐ Intermediário
> **Tipo:** File Upload · AWS S3 · Multipart · Pre-signed URLs
> **Estimativa:** 5–7 horas por stack

---

## 📋 Descrição

Criar um serviço de **upload e download de arquivos** (foto de perfil de Person) com armazenamento no AWS S3. Implementar upload direto, pre-signed URLs e thumbnails.

---

## 🎯 Objetivos de Aprendizado

- [ ] Multipart file upload
- [ ] AWS S3 SDK (PutObject, GetObject, DeleteObject)
- [ ] Pre-signed URLs (upload e download seguros)
- [ ] Validação de tipo/tamanho de arquivo
- [ ] Geração de thumbnails (resize)
- [ ] Streaming de arquivos grandes (não carregar na memória)
- [ ] MinIO como S3 local (desenvolvimento)

---

## 📐 Especificação

### Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/persons/{id}/avatar` | Upload direto (multipart) |
| `GET` | `/api/persons/{id}/avatar` | Download do avatar |
| `DELETE` | `/api/persons/{id}/avatar` | Remove avatar |
| `GET` | `/api/persons/{id}/avatar/url` | Gera pre-signed URL (download) |
| `POST` | `/api/persons/{id}/avatar/upload-url` | Gera pre-signed URL (upload) |
| `GET` | `/api/persons/{id}/avatar/thumbnail` | Thumbnail 150x150 |

### Estrutura no S3

```
bucket: person-avatars/
├── originals/
│   ├── 1/avatar.jpg
│   ├── 2/avatar.png
│   └── 3/avatar.jpg
└── thumbnails/
    ├── 1/avatar_150x150.jpg
    ├── 2/avatar_150x150.png
    └── 3/avatar_150x150.jpg
```

### Validações

| Regra | Valor |
|---|---|
| Tipos aceitos | `image/jpeg`, `image/png`, `image/webp` |
| Tamanho máximo | 5 MB |
| Dimensão mínima | 100x100 px |

### Response (upload)

```json
{
  "person_id": 1,
  "file_name": "avatar.jpg",
  "content_type": "image/jpeg",
  "size_bytes": 245760,
  "url": "https://s3.amazonaws.com/person-avatars/originals/1/avatar.jpg",
  "thumbnail_url": "https://s3.amazonaws.com/person-avatars/thumbnails/1/avatar_150x150.jpg",
  "uploaded_at": "2026-03-04T10:30:00Z"
}
```

---

## ✅ Critérios de Aceite

- [ ] Upload multipart funcionando
- [ ] Download com streaming (sem carregar na memória)
- [ ] Pre-signed URLs (expiração 15 min)
- [ ] Validação de tipo e tamanho
- [ ] Thumbnail gerado automaticamente
- [ ] MinIO rodando via Docker Compose
- [ ] Metadados salvos no banco (filename, size, content_type)
- [ ] Delete remove do S3 e do banco

---

## 🐳 Docker Compose (MinIO)

```yaml
services:
  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ":9001"
    volumes:
      - minio-data:/data

  createbucket:
    image: minio/mc:latest
    depends_on:
      - minio
    entrypoint: >
      /bin/sh -c "
      mc alias set local http://minio:9000 minioadmin minioadmin;
      mc mb local/person-avatars --ignore-existing;
      exit 0;
      "
```

---

## 🛠️ Implementar em

| Stack | S3 Client | Image Processing |
|---|---|---|
| **Spring Boot** | AWS SDK v2 / Spring Cloud AWS | Thumbnailator |
| **Micronaut** | Micronaut Object Storage | Thumbnailator |
| **Quarkus** | Quarkus Amazon S3 | Thumbnailator |
| **Go** | AWS SDK Go v2 | `disintegration/imaging` |

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `software.amazon.awssdk:s3` + `net.coobird:thumbnailator` |
| Micronaut | `micronaut-object-storage-aws` |
| Quarkus | `quarkus-amazon-s3` |
| Go | `github.com/aws/aws-sdk-go-v2/service/s3` + `github.com/disintegration/imaging` |

---

## 🔗 Referências

- [AWS S3 Developer Guide](https://docs.aws.amazon.com/AmazonS3/latest/userguide/)
- [Pre-signed URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html)
- [MinIO](https://min.io/)
