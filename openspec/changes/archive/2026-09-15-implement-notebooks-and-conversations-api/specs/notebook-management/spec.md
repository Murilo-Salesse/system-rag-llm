## MODIFIED Requirements

### Requirement: Criação e Listagem de Notebooks
O sistema SHALL permitir a criação e listagem paginada de notebooks pertencentes ao usuário autenticado (Tela 2).

#### Scenario: Criar novo notebook
- **WHEN** O usuário autenticado envia uma requisição `POST /api/v1/notebooks` com título e descrição
- **THEN** O sistema persiste o notebook associado ao `userId` do token JWT e retorna `HTTP 201 Created` com o ID e metadados.

#### Scenario: Listar notebooks do usuário
- **WHEN** O usuário acessa a Tela 2 e o frontend dispara `GET /api/v1/notebooks` (suportando paginação `Pageable`)
- **THEN** O sistema retorna `HTTP 200 OK` com a lista paginada apenas dos notebooks criados por aquele usuário, incluindo a contagem de fontes ativas.

## ADDED Requirements

### Requirement: Atualização de Notebook
O sistema SHALL permitir a atualização do nome e descrição de um notebook existente pertencente ao usuário autenticado (`PUT /api/v1/notebooks/{notebookId}`).

#### Scenario: Atualizar notebook próprio
- **WHEN** o usuário envia `PUT /api/v1/notebooks/{notebookId}` com novos valores de nome e descrição para um notebook que lhe pertence
- **THEN** o sistema atualiza o notebook e retorna `HTTP 200 OK` com os dados atualizados.

#### Scenario: Tentativa de atualizar notebook de outro usuário
- **WHEN** o usuário tenta atualizar um notebook que não lhe pertence
- **THEN** o sistema rejeita com `HTTP 404 Not Found`.
