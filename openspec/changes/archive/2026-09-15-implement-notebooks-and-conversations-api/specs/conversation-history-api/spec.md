## Purpose

Prover endpoints REST para criação e listagem de conversas e consulta cronológica de mensagens associadas a notebooks pertencentes ao usuário autenticado.

## ADDED Requirements

### Requirement: Criação de Conversas em Notebooks
O sistema SHALL permitir que o usuário autenticado crie uma conversa vinculada a um notebook de sua propriedade (`POST /api/v1/notebooks/{notebookId}/conversations`).

#### Scenario: Criar conversa em notebook próprio
- **WHEN** o usuário autenticado envia `POST /api/v1/notebooks/{notebookId}/conversations` para um notebook que lhe pertence
- **THEN** o sistema cria a conversa, persiste no banco associada ao notebook e retorna `HTTP 201 Created` com o ID da conversa.

#### Scenario: Tentativa de criar conversa em notebook de outro usuário
- **WHEN** o usuário tenta criar conversa em um notebook que não lhe pertence
- **THEN** o sistema rejeita a operação com `HTTP 404 Not Found`.

### Requirement: Listagem de Conversas do Notebook
O sistema SHALL listar todas as conversas existentes em um notebook pertencente ao usuário autenticado (`GET /api/v1/notebooks/{notebookId}/conversations`).

#### Scenario: Listar conversas de notebook próprio
- **WHEN** o usuário autenticado envia `GET /api/v1/notebooks/{notebookId}/conversations`
- **THEN** o sistema retorna `HTTP 200 OK` com a lista de conversas ordenadas por data de criação decrescente.

#### Scenario: Tentativa de listar conversas de notebook de outro usuário
- **WHEN** o usuário tenta listar conversas de um notebook que não lhe pertence
- **THEN** o sistema retorna `HTTP 404 Not Found`.

### Requirement: Consulta de Histórico de Mensagens da Conversa
O sistema SHALL retornar as mensagens de uma conversa em ordem cronológica crescente (`GET /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages`), validando que a conversa pertence ao notebook e o notebook pertence ao usuário autenticado.

#### Scenario: Consultar histórico de mensagens com sucesso
- **WHEN** o usuário envia `GET /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages` para uma conversa válida em seu notebook
- **THEN** o sistema retorna `HTTP 200 OK` com o array de mensagens ordenadas por `created_at ASC`.

#### Scenario: Tentativa de consultar mensagens de conversa ou notebook de outro usuário
- **WHEN** o usuário tenta consultar mensagens de uma conversa que não pertence a um notebook de sua propriedade
- **THEN** o sistema retorna `HTTP 404 Not Found`.
