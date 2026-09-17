## ADDED Requirements

### Requirement: Listagem de Fontes do Notebook

O backend SHALL disponibilizar um endpoint REST `GET /api/v1/notebooks/{notebookId}/sources` que retorna a lista de todas as fontes cadastradas no notebook do usuário autenticado, ordenadas da mais recente para a mais antiga.

#### Scenario: Listagem bem-sucedida de fontes do notebook
- **WHEN** o usuário autenticado envia `GET /api/v1/notebooks/{notebookId}/sources` para um notebook de sua propriedade
- **THEN** o sistema retorna `HTTP 200 OK` com a lista de fontes contendo `id`, `notebookId`, `name`, `type`, `status`, `errorMessage` e `createdAt`.

#### Scenario: Consulta de fontes de notebook de outro usuário
- **WHEN** o usuário autenticado tenta listar fontes de um notebook pertencente a outro usuário ou inexistente
- **THEN** o sistema rejeita a requisição retornando `HTTP 404 Not Found`.
