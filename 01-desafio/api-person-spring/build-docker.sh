#!/bin/bash
set -e

echo "🐳 Building Docker image with AOT training..."
echo ""

# Build sem cache para garantir que usa o Dockerfile atualizado
docker build --no-cache -t api-person-spring .

echo ""
echo "✅ Build concluído com sucesso!"
echo ""
echo "Para executar:"
echo "  docker run -p 8080:8080 api-person-spring"
