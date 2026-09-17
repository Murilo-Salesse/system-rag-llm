## Context

O backend Spring Boot possui a camada de dados JPA mapeada e o Spring Security configurado para validar JWTs do Cognito. Esta especificação projeta a implementação das camadas de Serviço (`Service`), Controladores REST (`Controller`), Objetos de Transferência (`DTOs/Records`), Validações e Tratamento de Erro Global (RFC 7807), garantindo blindagem contra IDOR e sincronização transparente de usuários via upsert.

## Goals / Non-Goals

**Goals:**
- Prover serviço `UserService` para sincronizar (upsert) usuários a partir de claims do JWT (`sub`, `email`, `name`).
- Prover endpoints REST para notebooks (`GET` paginado, `POST`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`).
- Prover endpoints REST para conversas (`POST` e `GET`) e histórico de mensagens (`GET .../messages`).
- Garantir blindagem anti-IDOR: qualquer tentativa de acesso ou manipulação a notebooks ou conversas de outros usuários resulta estritamente em `404 Not Found` (evitando enumeração de recursos).
- Adotar DTOs imutáveis com Java `record` e validações com Jakarta Validation (`@NotBlank`, `@Size`).
- Padronizar respostas de erro conforme a RFC 7807 via `@RestControllerAdvice`.
- Garantir 100% de cobertura de código e 100% mutation kill rate via `java-quality-gate`.

**Non-Goals:**
- Ingestão assíncrona de arquivos S3/DOCX/Web URLs (coberta em spec separada).
- Streaming de chat SSE via LLM (coberto em spec separada).

## Decisions

### D1 — Sincronização Transparente de Usuário (Upsert)
**Decisão:** Toda requisição autenticada extrai o `Jwt` injetado pelo Spring Security (`@AuthenticationPrincipal Jwt jwt`). O serviço `UserService.getOrCreateCurrentUser(jwt)` realiza o upsert: busca por `cognito_sub`; se existir, atualiza `email`/`name` caso tenham mudado; se não existir, cria e persiste a nova entidade `User`.
**Rationale:** Elimina a necessidade de um webhook de cadastro síncrono ou pré-registro de usuários, operando de forma 100% stateless e resiliente.

### D2 — Blindagem Anti-IDOR (Where Owner + 404)
**Decisão:** Nenhuma query de busca, atualização ou deleção busca apenas por `id`. Sempre se utiliza:
- `NotebookRepository.findByIdAndOwnerId(id, currentUser.getId())`
- `NotebookRepository.findByOwnerId(currentUser.getId(), pageable)`
- `ConversationRepository.findByIdAndNotebookId(convId, notebookId)` + verificação de que o notebook pertence a `currentUser.getId()`.
Caso o registro não pertença ao usuário logado, lança-se `ResourceNotFoundException("Notebook não encontrado")` retornando `HTTP 404 Not Found`.
**Rationale:** Impede que um usuário descubra ou manipule dados de terceiros e previne enumeração maliciosa de UUIDs válidos no banco (RN-01 do `DOMAIN.md`).

### D3 — Paginação Spring Page<NotebookResponse>
**Decisão:** O endpoint `GET /api/v1/notebooks` recebe parâmetros `Pageable` (`page`, `size`, `sort`) com `size` default de 10 e ordenação padrão por `createdAt DESC`, retornando `Page<NotebookResponse>`.
**Rationale:** Permite ao frontend da Tela 2 carregar notebooks de forma eficiente e escalável à medida que o volume de cadernos do usuário cresce.

### D4 — DTOs Imutáveis com Java 21 Records
**Decisão:** Todas as entradas e saídas utilizam Java `record`, com Bean Validation nos payloads de entrada (`@NotBlank`, `@Size(max = 255)`).
**Rationale:** Imutabilidade estrita, concisão sem boilerplate (princípio Ponytail) e prevenção de dados inconsistentes.

## Risks / Trade-offs

- **[Trade-off] Overhead de upsert a cada requisição** → Executar `findByCognitoSub` em toda requisição poderia gerar queries adicionais.
  - *Mitigação:* `cognito_sub` possui índice único no PostgreSQL (`idx_users_cognito_sub`), sendo uma busca indexada em memória extremamente rápida (<1ms).
- **[Risco] Deleção em cascata e bloqueios no banco** → Excluir um notebook grande com muitas mensagens e chunks pode demorar se não for transacional.
  - *Mitigação:* A constraint no PostgreSQL possui `ON DELETE CASCADE` nativo, e os métodos no service são anotados com `@Transactional`.
