## Why

A tela de Workspace (WorkspacePage) necessita interagir de ponta a ponta com a API de `/sources` implementada no backend. Atualmente, os uploads de arquivos (.md e .docx) não realizam o acompanhamento do ciclo assíncrono (polling de status), a listagem de fontes existentes no notebook precisa de endpoint dedicado e a interface precisa exibir claramente o status de processamento das fontes (`PENDING`, `PROCESSING`, `READY`, `FAILED`) e permitir a seleção para o contexto do chat.

## What Changes

- Implementação do endpoint `GET /api/v1/notebooks/{notebookId}/sources` no backend para listagem de todas as fontes de um notebook.
- Integração completa no frontend do fluxo de upload multipart (`POST /api/v1/notebooks/{notebookId}/sources/upload`) aceitando `.md` e `.docx` (<= 25MB) via Bearer JWT.
- Implementação de polling no frontend (`GET /api/v1/notebooks/{notebookId}/sources/{sourceId}`) para fontes em status `PENDING` ou `PROCESSING` até transitarem para `READY` ou `FAILED`.
- Atualização visual e funcional da sidebar de fontes na `WorkspacePage`: indicação de progresso de upload/processamento, mensagens de erro detalhadas, badges com cores do design system e ativação/desativação de fontes com checkboxes para o chat RAG.
- Tratamento de upload de arquivos inválidos diretamente no frontend com validação prévia de extensão e tamanho máximo (25MB).

## Capabilities

### New Capabilities
- `frontend/source-management`: Gerenciamento e acompanhamento de fontes no frontend, incluindo upload com feedback de progresso, polling de status assíncrono e seleção granular de fontes ativas para o chat.

### Modified Capabilities
- `source-ingestion`: Adiciona o endpoint de listagem de fontes `GET /api/v1/notebooks/{notebookId}/sources` para consulta de fontes associadas ao notebook.

## Impact

- **Affected code:** `WorkspacePage.tsx`, `client.ts`, `SourceController.java`, `SourceService.java`.
- **APIs:** Novo endpoint `GET /api/v1/notebooks/{notebookId}/sources`.
- **Statelessness & AWS Costs:** Mantém modelo 100% stateless com validação JWT via Cognito. Sem custos adicionais em nuvem (S3 local/Floci).
