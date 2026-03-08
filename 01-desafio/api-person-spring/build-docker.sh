#!/bin/bash
set -e

echo "🐳 Building Docker image (prod layers + Spring AOT + AOT cache)..."
echo ""

# Build sem cache para garantir treino full e geração de app.aot.
docker build --no-cache \
  --build-arg TRAINING_MODE=full \
  -t wesleyosantos91/api-person-spring:0.0.1-snapshot .

echo ""
echo "✅ Build concluído com sucesso!"
echo ""
echo "Para executar:"
echo "  docker run -p 8080:8080 wesleyosantos91/api-person-spring:0.0.1-snapshot"
