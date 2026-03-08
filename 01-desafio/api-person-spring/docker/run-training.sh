#!/bin/bash
set -e

echo "🎯 Iniciando processo de AOT Training..."

# Inicia a aplicação em background com AOT recording
echo "🚀 Iniciando aplicação com AOT recording..."
java -XX:AOTCacheOutput=app.aot \
  -XX:+UseZGC \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+ExitOnOutOfMemoryError \
  -Duser.timezone=America/Sao_Paulo \
  -Dspring.profiles.active=aot-training \
  -jar app.jar > /tmp/app.log 2>&1 &

APP_PID=$!
echo "   ✓ Aplicação iniciada (PID: $APP_PID)"

# Aguarda a aplicação estar pronta
echo "⏳ Aguardando aplicação ficar pronta..."
READY=false
for i in {1..60}; do
  if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    READY=true
    echo "   ✓ Aplicação pronta após ${i}s"
    break
  fi
  sleep 1
done

if [ "$READY" = false ]; then
  echo "❌ Timeout: aplicação não ficou pronta em 60s"
  cat /tmp/app.log
  kill $APP_PID 2>/dev/null || true
  exit 1
fi

# Executa a collection de training
echo "🏃 Executando collection de training..."
if ./training-collection.sh; then
  echo "   ✓ Collection executada com sucesso"
else
  echo "❌ Erro ao executar collection"
  cat /tmp/app.log
  kill $APP_PID 2>/dev/null || true
  exit 1
fi

# Finaliza a aplicação gracefully
echo "🛑 Finalizando aplicação para gerar AOT cache..."
kill -TERM $APP_PID

# Aguarda o processo finalizar (AOT pode levar mais tempo para montar o cache)
EXIT_TIMEOUT=180
for i in $(seq 1 $EXIT_TIMEOUT); do
  if ! kill -0 $APP_PID 2>/dev/null; then
    echo "   ✓ Aplicação finalizada após ${i}s"
    break
  fi
  sleep 1
done

# Se ainda estiver rodando, força o kill
if kill -0 $APP_PID 2>/dev/null; then
  echo "   ⚠️  Processo ainda ativo após ${EXIT_TIMEOUT}s, forçando finalização..."
  kill -9 $APP_PID 2>/dev/null || true
  sleep 2
fi

# A materialização do app.aot pode continuar após o processo principal encerrar
AOT_TIMEOUT=120
echo "⏳ Aguardando arquivo app.aot ser materializado..."
for i in $(seq 1 $AOT_TIMEOUT); do
  if [ -f "app.aot" ]; then
    break
  fi
  sleep 1
done

# Verifica se o AOT cache foi gerado
if [ -f "app.aot" ]; then
  AOT_SIZE=$(ls -lh app.aot | awk '{print $5}')
  echo "✅ AOT cache gerado com sucesso: $AOT_SIZE"
  ls -lh app.aot
else
  echo "❌ Erro: AOT cache não foi gerado"
  echo "📋 Logs da aplicação:"
  cat /tmp/app.log
  exit 1
fi

echo ""
echo "🎉 Training concluído com sucesso!"
