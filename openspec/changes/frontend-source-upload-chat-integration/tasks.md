## 1. Backend: Endpoint de Listagem de Fontes

- [ ] 1.1 Implementar método `listSources(User currentUser, UUID notebookId)` em `SourceService` e expor `GET /api/v1/notebooks/{notebookId}/sources` em `SourceController` retornando `List<SourceStatusResponse>`. Adicionar testes unitários com JUnit 5 + Mockito no service e no controller, verificando retorno 200 e isolamento anti-IDOR (404).
- [ ] 1.2 Executar testes automatizados do backend (`mvn test-compile` e `mvn test -Dtest=SourceControllerTest,SourceServiceTest`) e verificar que passam com sucesso.

## 2. Frontend: API Client de Fontes e Upload

- [ ] 2.1 Criar ou enriquecer funções de API em `app/frontend/src/api/sources.ts` (ou `client.ts`) para `listSources(notebookId)`, `uploadSource(notebookId, file, customName)` e `getSourceStatus(notebookId, sourceId)`, incluindo tratamento correto de cabeçalhos multipart sem sobrescrever o boundary.
- [ ] 2.2 Verificar compilação TypeScript com `npm run build` na pasta `app/frontend/`.

## 3. Frontend: Atualização da Tela de Workspace e Polling de Status

- [ ] 3.1 Atualizar `WorkspacePage.tsx` para carregar fontes existentes com `GET /api/v1/notebooks/{notebookId}/sources` no carregamento da tela e tratar status `PENDING`, `PROCESSING`, `READY` e `FAILED`.
- [ ] 3.2 Implementar rotina de polling reativo (a cada 2.5 segundos) que consulta fontes em `PENDING`/`PROCESSING` até finalizarem, adicionando automaticamente fontes `READY` recém-concluídas ao conjunto de `activeSources`.
- [ ] 3.3 Adicionar validações client-side no botão de upload (rejeitar extensões diferentes de `.md`, `.markdown`, `.docx` e arquivos > 25MB) com mensagens de erro na UI e indicador de progresso durante o envio.
- [ ] 3.4 Exibir na listagem de fontes badges claros de status (`Processando...`, `Pendente`, `Erro`), mensagem de erro caso `FAILED`, e habilitar seleção de checkbox apenas para fontes `READY`.

## 4. Verificação de Integração e Build

- [ ] 4.1 Executar `npm run build` no frontend garantindo zero erros de tipagem e empacotamento.
- [ ] 4.2 Executar a verificação do Quality Gate no backend com `quality-gate.sh` e testes unitários completos.
