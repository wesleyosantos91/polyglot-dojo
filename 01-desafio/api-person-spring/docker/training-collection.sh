#!/bin/bash
set -e

BASE_URL="http://localhost:8080"
WAIT_TIME=45

echo "🔥 Aguardando aplicação iniciar..."
for i in $(seq 1 $WAIT_TIME); do
  if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo "✅ Aplicação está pronta!"
    break
  fi
  if [ $i -eq $WAIT_TIME ]; then
    echo "❌ Timeout aguardando aplicação"
    exit 1
  fi
  sleep 1
done

echo "🚀 Executando collection de treinamento AOT (baseada em Insomnia)..."

# 1. SAVE - Criar pessoa principal
echo "📝 [1/14] SAVE: Criando pessoa principal..."
PERSON_ID=$(curl -s -X POST "$BASE_URL/api/persons" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: req-001-person-create" \
  -d '{
    "name": "Wesley Santos",
    "email": "wesley@example.com",
    "birthDate": "1991-01-15"
  }' | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "   ✓ Pessoa criada: $PERSON_ID"

# 2. SAVE - Criar pessoa para deletar
echo "📝 [2/14] SAVE: Criando pessoa para deletar..."
DELETE_PERSON_ID=$(curl -s -X POST "$BASE_URL/api/persons" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: req-001-person-create" \
  -d '{
    "name": "Wesley Santos",
    "email": "wesley_delete@example.com",
    "birthDate": "1991-01-15"
  }' | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "   ✓ Pessoa para deletar criada: $DELETE_PERSON_ID"

# 3. FIND_BY_ID - Buscar por ID
echo "📖 [3/14] FIND_BY_ID: Buscando pessoa por ID..."
curl -s -X GET "$BASE_URL/api/persons/$PERSON_ID" \
  -H "X-Correlation-Id: req-002-person-get" > /dev/null
echo "   ✓ Pessoa encontrada"

# 4. UPDATE - Atualizar pessoa completa
echo "✏️  [4/14] UPDATE: Atualizando pessoa completa..."
curl -s -X PUT "$BASE_URL/api/persons/$PERSON_ID" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: req-003-person-update" \
  -d '{
    "name": "Wesley Oliveira Santos",
    "email": "wesley@example.com",
    "birthDate": "1991-01-15"
  }' > /dev/null
echo "   ✓ Pessoa atualizada"

# 5. SAVE - ERRO 400 (validação)
echo "❌ [5/14] SAVE - ERRO 400: Testando validação..."
curl -s -X POST "$BASE_URL/api/persons" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: req-001-person-create" \
  -d '{
    "name": "",
    "email": "email-invalido",
    "birthDate": "2099-01-01"
  }' > /dev/null
echo "   ✓ Erro 400 testado"

# 6. FIND_BY_ID - ERRO 400 (ID inválido)
echo "❌ [6/14] FIND_BY_ID - ERRO 400: Testando ID inválido..."
curl -s -X GET "$BASE_URL/api/persons/abc" \
  -H "X-Correlation-Id: req-002-person-get" > /dev/null
echo "   ✓ Erro 400 (ID inválido) testado"

# 7. FIND_BY_ID - ERRO 404 (não encontrado)
echo "❌ [7/14] FIND_BY_ID - ERRO 404: Testando não encontrado..."
curl -s -X GET "$BASE_URL/api/persons/11111111-1111-1111-1111-111111111111" \
  -H "X-Correlation-Id: req-002-person-get" > /dev/null
echo "   ✓ Erro 404 testado"

# 8. SAVE - ERRO 409/422 (conflito de email)
echo "❌ [8/14] SAVE - ERRO 409/422: Testando conflito..."
curl -s -X POST "$BASE_URL/api/persons" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: req-001-person-create" \
  -d '{
    "name": "Outro Nome",
    "email": "wesley@example.com",
    "birthDate": "1990-01-01"
  }' > /dev/null
echo "   ✓ Erro 409/422 testado"

# 9. FIND_PAGED - Listagem paginada
echo "📋 [9/14] FIND_PAGED: Listagem paginada..."
curl -s -X GET "$BASE_URL/api/persons?page=0&size=5&sort=createdAt,desc" \
  -H "X-Correlation-Id: req-list-001" > /dev/null
echo "   ✓ Listagem paginada OK"

# 10. FIND_PAGED_BY_NAME - Busca por nome
echo "🔍 [10/14] FIND_PAGED_BY_NAME: Busca por nome..."
curl -s -X GET "$BASE_URL/api/persons?page=0&size=10&sort=name,asc&name=wesley" \
  -H "X-Correlation-Id: req-list-002" > /dev/null
echo "   ✓ Busca por nome OK"

# 11. FIND_PAGED_BY_EMAIL - Busca por email
echo "🔍 [11/14] FIND_PAGED_BY_EMAIL: Busca por email..."
curl -s -X GET "$BASE_URL/api/persons?page=0&size=10&email=wesley@example.com" \
  -H "X-Correlation-Id: req-list-003" > /dev/null
echo "   ✓ Busca por email OK"

# 12. UPDATE_PARTIAL_BY_NAME - Patch nome
echo "🔧 [12/14] UPDATE_PARTIAL: Patch por nome..."
curl -s -X PATCH "$BASE_URL/api/persons/$PERSON_ID" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: req-patch-001" \
  -d '{"name": "Wesley O. Santos"}' > /dev/null
echo "   ✓ Patch nome OK"

# 13. UPDATE_PARTIAL_BY_EMAIL - Patch email
echo "🔧 [13/14] UPDATE_PARTIAL: Patch por email..."
curl -s -X PATCH "$BASE_URL/api/persons/$PERSON_ID" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: req-patch-001" \
  -d '{"email": "wesley.novo@example.com"}' > /dev/null
echo "   ✓ Patch email OK"

# 14. UPDATE_PARTIAL_MULTIPLES - Patch múltiplos campos
echo "🔧 [14/14] UPDATE_PARTIAL: Patch múltiplos campos..."
curl -s -X PATCH "$BASE_URL/api/persons/$PERSON_ID" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: req-patch-001" \
  -d '{
    "name": "Wesley O. S.",
    "birthDate": "1991-01-16"
  }' > /dev/null
echo "   ✓ Patch múltiplos campos OK"

# 15. DELETE_BY_ID - Deletar pessoa
echo "🗑️  [15/15] DELETE_BY_ID: Deletando pessoa..."
curl -s -X DELETE "$BASE_URL/api/persons/$DELETE_PERSON_ID" \
  -H "X-Correlation-Id: req-delete-001" > /dev/null
echo "   ✓ Pessoa deletada"

echo ""
echo "✅ Collection de treinamento concluída com sucesso!"
echo "🎯 AOT cache treinado com 15 cenários reais:"
echo "   ✓ CRUD completo (Create, Read, Update, Patch, Delete)"
echo "   ✓ Validações e erros (400, 404, 409/422)"
echo "   ✓ Buscas e filtros (nome, email)"
echo "   ✓ Paginação e ordenação"
echo "   ✓ Atualizações parciais (múltiplos cenários)"
