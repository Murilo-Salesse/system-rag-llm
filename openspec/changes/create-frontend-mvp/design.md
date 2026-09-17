## Context

O backend Spring Boot expõe a API REST e o fluxo SSE em `/api/v1` com autenticação Bearer JWT via AWS Cognito / Floci. O diretório `app/frontend` deve abrigar o client web SPA. A aplicação deve ser um Mínimo Viável Funcional (MVP), priorizando simplicidade, ausência de dependências supérfluas e velocidade de execução, traduzindo fielmente os wireframes das 3 telas do projeto.

## Goals / Non-Goals

**Goals:**
- Inicializar o app frontend com **Vite + React (TypeScript) + Tailwind CSS** dentro de `app/frontend`.
- Configurar o servidor de desenvolvimento do Vite para atuar como proxy reverso de `/api/v1` para `http://localhost:8080`, simplificando chamadas locais e evitando problemas de CORS.
- Implementar as 3 telas essenciais do wireframe:
  1. **Tela 1 (`LoginPage`)**: Centralizada, botões de login Google e GitHub apontando para os endpoints OIDC do Cognito (com suporte a fallback/token local em `localStorage`).
  2. **Tela 2 (`DashboardPage`)**: Header simples com botão `[ Criar ]` e grid de cards dos notebooks com botão `[ Abrir ]`.
  3. **Tela 3 (`WorkspacePage`)**: Layout Split View de 2 colunas — painel esquerdo para upload de arquivos/URLs e checkboxes de seleção de fontes ativas; painel direito para mensagens de chat e streaming SSE com auto-scroll.
- Consumir o streaming SSE de mensagens via `fetch` nativo com `ReadableStream` (`POST /api/v1/notebooks/:id/conversations/:convId/messages/stream`).

**Non-Goals:**
- Componentes visuais ultra-customizados, temas complexos ou animações pesadas (seguir filosofia enxuta do Ponytail).
- Gerenciamento de estado global complexo com Redux/Zustand (React Context e hooks locais bastam para o MVP).
- Servidor intermediário Node.js ou SSR (Next.js).

## Decisions

### 1. Vite + React (TypeScript) + Tailwind CSS
- **Decisão**: Utilizar Vite com template React-TS e Tailwind CSS para estilização utilitária.
- **Rationale**: Inicialização instantânea, build ultra-leve e sem boilerplate complexo. Permite reproduzir exatamente as caixas com bordas arredondadas e o layout limpo dos wireframes desenhados à mão.

### 2. Proxy Reverso no Vite (`vite.config.ts`)
- **Decisão**: Configurar `server.proxy` no Vite encaminhando requisições `/api/v1` para `http://localhost:8080`.
- **Rationale**: Permite que o frontend faça chamadas relativas (ex: `fetch('/api/v1/notebooks')`), sem necessidade de configurar URLs absolutas ou lidar com cabeçalhos CORS manuais no backend durante o desenvolvimento.

### 3. Consumo de SSE via `fetch` + `ReadableStream`
- **Decisão**: Usar a API nativa do navegador `fetch(url, { headers: { Accept: 'text/event-stream' } })` processando `response.body.getReader()` com `TextDecoder`.
- **Rationale**: Como o endpoint de streaming do NotebookLM é um `POST` com body JSON (`content`, `activeSourceIds`), a API padrão de `EventSource` do navegador não é adequada (pois só aceita `GET` sem headers de autorização customizados). `fetch` nativo lida perfeitamente com `POST`, Bearer token e streaming de chunks em tempo real sem bibliotecas externas.

### 4. Gerenciamento de Sessão Stateless
- **Decisão**: Criar um `AuthContext` que mantém o Bearer token no `localStorage`. Ao inicializar, injeta o token nas chamadas da API. Se não houver token, o usuário permanece na Tela 1.

## Risks / Trade-offs

- **[Risco] Formato dos Chunks SSE no Browser**: O backend pode enviar múltiplos eventos em um mesmo chunk de rede TCP ou quebrar mensagens JSON entre chunks.
  - *Mitigação*: Criar um parser simples de buffer de linhas no leitor de stream que bufferiza até encontrar quebras de linha (`\n\n`) antes de processar `data: {...}`.
- **[Risco] Upload de arquivos grandes bloqueando a interface**:
  - *Mitigação*: Exibir estado de loading no botão de upload e transicionar a fonte para a lista com status `PENDING` imediatamente ao receber `202 Accepted`.
