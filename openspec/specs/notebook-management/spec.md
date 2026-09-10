## Purpose
Gerenciar notebooks como unidades lógicas de agrupamento de fontes e sessões de conversa, garantindo isolamento por usuário.

## Requirements

### Requirement: Criação e Listagem de Notebooks
O sistema deve permitir a criação e listagem de notebooks pertencentes ao usuário autenticado (Tela 2).

#### Scenario: Criar novo notebook
- **WHEN** O usuário autenticado envia uma requisição `POST /api/v1/notebooks` com título e descrição
- **THEN** O sistema persiste o notebook associado ao `userId` do token JWT e retorna `HTTP 201 Created` com o ID e metadados.

#### Scenario: Listar notebooks do usuário
- **WHEN** O usuário acessa a Tela 2 e o frontend dispara `GET /api/v1/notebooks`
- **THEN** O sistema retorna `HTTP 200 OK` com a lista apenas dos notebooks criados por aquele usuário, incluindo a contagem de fontes ativas.

### Requirement: Isolamento Multitenancy por Usuário
Um usuário não pode visualizar, alterar nem excluir notebooks pertencentes a outros usuários.

#### Scenario: Tentativa de acesso a notebook de outro usuário
- **WHEN** Um usuário tenta acessar `GET /api/v1/notebooks/{notebookId}` de um notebook que não lhe pertence
- **THEN** O sistema retorna `HTTP 404 Not Found` (ou `HTTP 403 Forbidden`) para evitar vazamento de dados.

### Requirement: Exclusão em Cascata do Notebook
A exclusão de um notebook remove logicamente todas as fontes, fragmentos no pgvector e conversas associadas.

#### Scenario: Excluir notebook
- **WHEN** O usuário envia `DELETE /api/v1/notebooks/{notebookId}`
- **THEN** O sistema remove o registro de `notebooks`, disparando `ON DELETE CASCADE` nas tabelas `sources`, `document_chunks`, `chat_sessions` e `chat_messages`, retornando `HTTP 204 No Content`.
