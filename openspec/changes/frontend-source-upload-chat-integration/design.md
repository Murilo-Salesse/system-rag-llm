## Context

O backend Spring Boot já possui o pipeline assíncrono implementado (`SourceService`, `SourceIngestionProcessor`, `S3StorageService`, `DocumentParserService`), recebendo arquivos via `POST /api/v1/notebooks/{notebookId}/sources/upload` e disponibilizando consulta unitária de status em `GET /api/v1/notebooks/{notebookId}/sources/{sourceId}`.
No entanto, o endpoint de listagem `GET /api/v1/notebooks/{notebookId}/sources` ainda não foi exposto no controller, causando 404/500 quando a tela abre. Além disso, o frontend na `WorkspacePage` precisa orquestrar o envio de arquivos multipart, o polling de status das fontes não finalizadas e a ativação seletiva para a query do RAG.

## Goals / Non-Goals

**Goals:**
- Expor `GET /api/v1/notebooks/{notebookId}/sources` no backend retornando a lista de `SourceStatusResponse` do notebook.
- Implementar no frontend o upload com validação de extensão (`.md`, `.docx`) e tamanho (máximo 25MB).
- Implementar polling reativo com `setInterval` ou timeout recursivo para fontes em `PENDING`/`PROCESSING`.
- Exibir feedback visual de status (`PENDING`, `PROCESSING`, `READY`, `FAILED`) com ícones, cores e mensagens de erro do backend.
- Manter fontes `READY` selecionáveis com sincronização direta nos `activeSourceIds` enviados no stream de chat.

**Non-Goals:**
- Ingestão de Web URL nesta change (escopo focado em upload de arquivos conforme solicitação).
- Exclusão ou renomeação de fontes existentes.
- WebSockets para status de fontes (polling REST simples a cada 2.5s é suficiente, stateless e de baixo overhead).

## Decisions

### D1 — Endpoint de listagem reaproveitando `SourceStatusResponse`
O `SourceController` terá o endpoint:
```java
@GetMapping
public ResponseEntity<List<SourceStatusResponse>> listSources(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID notebookId
)
```
**Alternativas consideradas:** Criar um `SourceListResponse` separado. Rejeitado por YAGNI: `SourceStatusResponse` já contém exatamente os campos necessários (`id`, `name`, `type`, `status`, `errorMessage`, `createdAt`).

### D2 — Mecanismo de Polling no React
Utilizar um efeito dedicado no React que identifica fontes com `status === 'PENDING' || status === 'PROCESSING'` e dispara requisições a cada 2500ms para verificar o progresso individualmente ou re-consultar a lista.
**Decisão:** Re-consultar a lista ou atualizar o item via `GET /sources/{sourceId}`. Quando transitar para `READY`, adiciona automaticamente o ID da nova fonte ao conjunto `activeSources`.

### D3 — Upload Multipart via helper centralizado ou `fetch` nativo com `getToken()`
O upload multipart no frontend não deve definir manualmente `Content-Type: multipart/form-data` no header para permitir que o browser calcule automaticamente o boundary correto (`multipart/form-data; boundary=...`). O token JWT deve vir de `getToken()`.

## Risks / Trade-offs

- **[Risco] Múltiplos uploads simultâneos gerarem muitas requisições de polling** → Mitigação: intervalo de 2.5s por ciclo e cancelamento do timer ao desmontar componente ou ao concluir todas as fontes.
- **[Risco] Arquivo corrompido ou falha no parsing** → Mitigação: o status vai para `FAILED` e a mensagem de erro é exibida na interface sem travar a aplicação ou o chat.
