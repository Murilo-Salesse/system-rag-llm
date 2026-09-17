## 1. Setup do Projeto Frontend

- [x] 1.1 Inicializar a estrutura do projeto SPA com Vite, React (TypeScript) e Tailwind CSS em `app/frontend` e verificar arquivos gerados
- [x] 1.2 Configurar `vite.config.ts` com proxy reverso apontando `/api/v1` para `http://localhost:8080` e testar resolução de configuração
- [x] 1.3 Criar o cliente HTTP modular (`src/api/client.ts`) com tratamento de cabeçalho `Authorization: Bearer` e `AuthContext.tsx` para gerenciar o token da aplicação

## 2. Implementação das Telas (Wireframes)

- [x] 2.1 Implementar a Tela 1 (`src/pages/LoginPage.tsx`) com layout centralizado contendo o título "NotebookLM" e os botões "Login Google" e "Login GitHub", e verificar renderização
- [x] 2.2 Implementar a Tela 2 (`src/pages/DashboardPage.tsx`) com header, botão `[ Criar ]`, grid de cards de notebooks com botão `[ Abrir ]` e integração com `GET/POST /api/v1/notebooks`
- [x] 2.3 Implementar a Tela 3 (`src/pages/WorkspacePage.tsx`) com Split View: Painel esquerdo de Sources (upload de .md/.docx, Web URL, status badges e checkboxes de fontes ativas com `PUT .../active-sources`)
- [x] 2.4 Implementar no Painel direito da Tela 3 o Chat com histórico de mensagens e área de input com streaming de tokens em tempo real consumindo SSE via `ReadableStream` (`POST .../messages/stream`)

## 3. Verificação e Build

- [x] 3.1 Configurar rotas da aplicação no `App.tsx` com `react-router-dom` conectando `/login`, `/` (Dashboard) e `/notebooks/:id` (Workspace)
- [x] 3.2 Executar build de produção do frontend (`npm run build` em `app/frontend`) e verificar se os assets compilam sem erros de TypeScript ou bundle
