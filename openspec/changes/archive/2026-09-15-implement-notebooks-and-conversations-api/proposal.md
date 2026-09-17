## Why

As entidades de domínio JPA e a infraestrutura de banco de dados e autenticação já estão implementadas e validadas. No entanto, o sistema ainda não expõe os endpoints REST para que o usuário final autenticado gerencie seus notebooks, gerencie conversas e consulte o histórico de mensagens conforme especificado no `API.md` e `DOMAIN.md`. Esta mudança implementa esses endpoints REST com proteção rigorosa contra IDOR (Insecure Direct Object Reference) e sincronização transparente (upsert) dos dados do usuário logado via token Cognito.

## What Changes

- **Serviço de Usuário com Upsert (`UserService`):**
  - Identifica o usuário a partir do token JWT (`cognito_sub`, `email`, `name`).
  - Cria o usuário na tabela `users` caso seja seu primeiro acesso ou atualiza dados alterados.
- **Endpoints do Módulo de Notebooks (`NotebookController` + `NotebookService`):**
  - `GET /api/v1/notebooks`: Listagem paginada retornando `Page<NotebookResponse>` (com contagem de fontes anexadas).
  - `POST /api/v1/notebooks`: Criação de novo notebook associado ao usuário autenticado (`201 Created`).
  - `GET /api/v1/notebooks/{notebookId}`: Consulta detalhada de notebook com blindagem anti-IDOR (`200 OK` ou `404 Not Found`).
  - `PUT /api/v1/notebooks/{notebookId}`: Atualização de nome e descrição com validação de propriedade (`200 OK` ou `404 Not Found`).
  - `DELETE /api/v1/notebooks/{notebookId}`: Exclusão do notebook e cascade com validação anti-IDOR (`204 No Content` ou `404 Not Found`).
- **Endpoints do Módulo de Conversas (`ConversationController` + `ConversationService`):**
  - `POST /api/v1/notebooks/{notebookId}/conversations`: Criação de conversa vinculada ao notebook do usuário (`201 Created`).
  - `GET /api/v1/notebooks/{notebookId}/conversations`: Listagem de conversas de um notebook pertencente ao usuário (`200 OK`).
  - `GET /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages`: Listagem cronológica de mensagens da conversa, validando que a conversa e o notebook pertencem ao usuário logado (`200 OK`).
- **DTOs Imutáveis (Records) com Bean Validation:**
  - `CreateNotebookRequest`, `UpdateNotebookRequest`, `NotebookResponse`, `ConversationResponse`, `ConversationMessageResponse`.
- **Tratamento Global de Erros (RFC 7807):**
  - `GlobalExceptionHandler` padronizando respostas para `ResourceNotFoundException` (404), `MethodArgumentNotValidException` (400) e erros de acesso (403).
- **Testes Unitários & Quality Gate:**
  - Testes unitários JUnit 5 + Mockito para controllers, services e exception handler com 100% line coverage e 100% mutation kill rate via `java-quality-gate`.

## Capabilities

### New Capabilities
- `conversation-history-api`: Endpoints REST para criação e listagem de conversas e consulta cronológica do histórico de mensagens subordinadas a notebooks.

### Modified Capabilities
- `notebook-management`: Adiciona suporte à paginação (`Page<NotebookResponse>`) na listagem de notebooks e endpoint de atualização `PUT /api/v1/notebooks/{notebookId}` com blindagem estrita anti-IDOR.

## Impact

- **Código Afetado:** Novos pacotes `github.salessew.notebooklm.service`, `github.salessew.notebooklm.dto`, `github.salessew.notebooklm.exception` e extensão de `github.salessew.notebooklm.controller`.
- **APIs & Contratos:** Novos endpoints REST disponíveis em `/api/v1/notebooks/**`.
- **Statelessness & Segurança:** Totalmente stateless, autenticado via JWT do Cognito, blindado contra IDOR retornando `HTTP 404 Not Found` para evitar enumeração de recursos de outros usuários.
- **DDL / Banco de Dados:** Nenhuma alteração de DDL necessária (usa as tabelas já provisionadas).
