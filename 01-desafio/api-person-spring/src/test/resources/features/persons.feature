# language: pt
Funcionalidade: Gerenciamento de Pessoas
  Como usuário do sistema
  Quero poder cadastrar, consultar, atualizar e remover pessoas
  Para manter os dados cadastrais atualizados

  Contexto:
    Dado que o banco de dados está limpo

  Cenário: Cadastrar uma nova pessoa com sucesso
    Quando eu envio uma requisição POST para "/api/persons" com o corpo:
      """
      {
        "name": "Wesley Santos",
        "email": "wesley@example.com",
        "birthDate": "1991-01-15"
      }
      """
    Então o status da resposta deve ser 201
    E a resposta deve conter o campo "name" com valor "Wesley Santos"
    E a resposta deve conter o campo "email" com valor "wesley@example.com"
    E a resposta deve conter um campo "id" não vazio
    E o header "Location" deve estar presente na resposta

  Cenário: Cadastrar pessoa com email já existente retorna conflito
    Dado que existe uma pessoa com email "duplicado@example.com"
    Quando eu envio uma requisição POST para "/api/persons" com o corpo:
      """
      {
        "name": "Outro Nome",
        "email": "duplicado@example.com",
        "birthDate": "1990-06-20"
      }
      """
    Então o status da resposta deve ser 422
    E a resposta deve conter o campo "error_code" com valor "PERSON_EMAIL_ALREADY_EXISTS"

  Cenário: Cadastrar pessoa com campos inválidos retorna erro de validação
    Quando eu envio uma requisição POST para "/api/persons" com o corpo:
      """
      {
        "name": "",
        "email": "nao-e-um-email",
        "birthDate": "2099-12-31"
      }
      """
    Então o status da resposta deve ser 400
    E a resposta deve conter o campo "error_code" com valor "VALIDATION_ERROR"
    E a resposta deve conter o campo "violations" como lista não vazia

  Cenário: Buscar pessoa existente por ID
    Dado que existe uma pessoa cadastrada com nome "Carlos Maia" e email "carlos@example.com"
    Quando eu busco a pessoa pelo ID cadastrado
    Então o status da resposta deve ser 200
    E a resposta deve conter o campo "name" com valor "Carlos Maia"
    E a resposta deve conter o campo "email" com valor "carlos@example.com"

  Cenário: Buscar pessoa por ID inexistente retorna 404
    Quando eu envio uma requisição GET para "/api/persons/00000000-0000-0000-0000-000000000099"
    Então o status da resposta deve ser 404
    E a resposta deve conter o campo "error_code" com valor "RESOURCE_NOT_FOUND"

  Cenário: Buscar pessoa por ID com formato inválido retorna 400
    Quando eu envio uma requisição GET para "/api/persons/nao-e-uuid"
    Então o status da resposta deve ser 400
    E a resposta deve conter o campo "error_code" com valor "ARGUMENT_TYPE_MISMATCH"

  Cenário: Atualizar pessoa com sucesso
    Dado que existe uma pessoa cadastrada com nome "Ana Lima" e email "ana@example.com"
    Quando eu envio uma requisição PUT para a pessoa cadastrada com o corpo:
      """
      {
        "name": "Ana Oliveira",
        "email": "ana.novo@example.com",
        "birthDate": "1990-05-10"
      }
      """
    Então o status da resposta deve ser 200
    E a resposta deve conter o campo "name" com valor "Ana Oliveira"
    E a resposta deve conter o campo "email" com valor "ana.novo@example.com"
    E a resposta deve conter um campo "updatedAt" não vazio

  Cenário: Atualizar parcialmente apenas o nome
    Dado que existe uma pessoa cadastrada com nome "Bruno Costa" e email "bruno@example.com"
    Quando eu envio uma requisição PATCH para a pessoa cadastrada com o corpo:
      """
      { "name": "Bruno Ferreira" }
      """
    Então o status da resposta deve ser 200
    E a resposta deve conter o campo "name" com valor "Bruno Ferreira"
    E a resposta deve conter o campo "email" com valor "bruno@example.com"

  Cenário: Deletar pessoa existente
    Dado que existe uma pessoa cadastrada com nome "Delete Me" e email "delete@example.com"
    Quando eu deleto a pessoa cadastrada
    Então o status da resposta deve ser 204
    E ao buscar a pessoa deletada o status deve ser 404

  Cenário: Deletar pessoa inexistente retorna 404
    Quando eu envio uma requisição DELETE para "/api/persons/00000000-0000-0000-0000-000000000099"
    Então o status da resposta deve ser 404

  Cenário: Listar pessoas com paginação
    Dado que existem 3 pessoas cadastradas
    Quando eu envio uma requisição GET para "/api/persons?page=0&size=2"
    Então o status da resposta deve ser 200
    E a resposta deve conter o campo "page.totalElements" com valor numérico 3
    E a resposta deve conter o campo "page.totalPages" com valor numérico 2

  Cenário: Filtrar pessoas por nome
    Dado que existe uma pessoa cadastrada com nome "Filtrado Santos" e email "filtrado@example.com"
    Quando eu envio uma requisição GET para "/api/persons?name=filtrado"
    Então o status da resposta deve ser 200
    E a resposta deve conter o campo "content[0].name" com valor "Filtrado Santos"