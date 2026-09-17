## 1. DTOs and Shared Infrastructure

- [x] 1.1 Criar DTOs records imutáveis para Notebooks (CreateNotebookRequest, UpdateNotebookRequest, NotebookResponse) e Conversas/Mensagens (CreateConversationRequest, ConversationResponse, MessageResponse) com Jakarta Validation e verificar compilação
- [x] 1.2 Implementar exceções de domínio (ResourceNotFoundException, etc.) e GlobalExceptionHandler com formato RFC 7807 (ProblemDetail)
- [x] 1.3 Implementar testes unitários para GlobalExceptionHandler, rodar mvn test e verificar qualidade via app/backend-api/local/quality-gate.sh

## 2. User Service (Cognito JWT Upsert)

- [x] 2.1 Implementar UserService para extrair claims do Jwt (sub, email, name) e realizar upsert transparente no banco de dados
- [x] 2.2 Implementar testes unitários para UserService cobrindo cenários de novo usuário, usuário existente sem alterações e usuário existente com dados alterados
- [x] 2.3 Executar app/backend-api/local/quality-gate.sh para validar 100% de line coverage e mutation kill rate no UserService

## 3. Notebook Management (API & Anti-IDOR Service)

- [x] 3.1 Atualizar NotebookRepository com queries seguras (findByOwnerId, findByIdAndOwnerId)
- [x] 3.2 Implementar NotebookService contendo listagem paginada (Page<NotebookResponse>), criação, busca por ID, atualização (PUT) e deleção com blindagem anti-IDOR (lançando 404 para não proprietários)
- [x] 3.3 Implementar NotebookController expondo endpoints GET /api/v1/notebooks, POST /api/v1/notebooks, GET /api/v1/notebooks/{notebookId}, PUT /api/v1/notebooks/{notebookId} e DELETE /api/v1/notebooks/{notebookId}
- [x] 3.4 Implementar testes unitários para NotebookService e NotebookController cobrindo todos os fluxos de sucesso e anti-IDOR (404)
- [x] 3.5 Executar app/backend-api/local/quality-gate.sh garantindo cobertura e mutation kill rate

## 4. Conversation History (API & Anti-IDOR Service)

- [x] 4.1 Atualizar ConversationRepository e ConversationMessageRepository com queries seguras (findByIdAndNotebookId, findByConversationIdOrderByCreatedAtAsc)
- [x] 4.2 Implementar ConversationService contendo criação de conversa, listagem por notebook e listagem de mensagens cronológicas com blindagem anti-IDOR (404 se o notebook não pertencer ao usuário autenticado)
- [x] 4.3 Implementar ConversationController expondo POST /api/v1/notebooks/{notebookId}/conversations, GET /api/v1/notebooks/{notebookId}/conversations e GET /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages
- [x] 4.4 Implementar testes unitários para ConversationService e ConversationController cobrindo todos os fluxos de sucesso e anti-IDOR (404)
- [x] 4.5 Executar app/backend-api/local/quality-gate.sh garantindo cobertura e mutation kill rate

## 5. End-to-End & Quality Gate Final Verification

- [x] 5.1 Executar suíte completa de testes (./mvnw test), relatório de mutações (./mvnw org.pitest:pitest-maven:mutationCoverage) e app/backend-api/local/quality-gate.sh
- [x] 5.2 Realizar testes integrados ponta-a-ponta chamando a API com JWT gerado pelo login.sh e validando o comportamento de upsert e anti-IDOR
