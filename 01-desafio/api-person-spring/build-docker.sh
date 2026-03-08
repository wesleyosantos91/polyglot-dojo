#!/bin/bash
set -e

echo "🐳 Building Docker image with AOT training..."
echo ""

# Build sem cache para garantir que usa o Dockerfile atualizado
docker build --no-cache -t wesleyosantos91/api-person-spring:0.0.1-snapshot .

echo ""
echo "✅ Build concluído com sucesso!"
echo ""
echo "Para executar:"
echo "  docker run -p 8080:8080 api-person-spring"
