# =============================================================================
# Multi-stage Dockerfile — Go + Gin (API Person)
# Binário estático | Scratch | Container best practices
#
# Go compila para um binário estático — não precisa de JVM, runtime, nem libs.
# A imagem final usa scratch (0 KB base) para máxima segurança e mínimo tamanho.
# =============================================================================

# ============================================================
# Stage 1: BUILD — Compilação com cache de módulos
# ============================================================
FROM golang:1.26-alpine AS build

WORKDIR /workspace

# Segurança: instala certificados CA (necessários para HTTPS em scratch)
RUN apk add --no-cache ca-certificates tzdata

# Cache de dependências Go — copia apenas os descritores primeiro
COPY go.mod go.sum ./
RUN go mod download && go mod verify

# Copia o código-fonte e compila binário estático otimizado
COPY . .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
  -ldflags="-s -w -extldflags '-static'" \
  -trimpath \
  -o /app \
  ./cmd/api

# ============================================================
# Stage 2: RUNTIME — Imagem final mínima (scratch)
# ============================================================
FROM scratch AS runtime

# Importa certificados CA e timezone data do builder
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=build /usr/share/zoneinfo /usr/share/zoneinfo

# Segurança: roda como usuário não-root (UID 65534 = nobody)
USER 65534:65534

ENV TZ=America/Sao_Paulo

# Copia o binário estático
COPY --from=build --chown=65534:65534 /app /app

EXPOSE 8080

ENTRYPOINT ["/app"]
