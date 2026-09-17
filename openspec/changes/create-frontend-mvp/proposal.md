## Why

O backend do NotebookLM Simplificado já possui entidades de domínio, persistência em PostgreSQL + pgvector, autenticação stateless via Cognito no Floci e endpoints REST/SSE implementados. Contudo, o diretório `app/frontend` está vazio, impedindo que usuários finais interajam com o sistema. É necessário implementar a camada de frontend como um Mínimo Viável Funcional (MVP) enxuto e direto ao ponto, cobrindo as 3 telas essenciais do wireframe sem sobrecarga estética: Tela 1 (Login Google/GitHub), Tela 2 (Dashboard de Notebooks) e Tela 3 (Workspace com seleção granular de fontes e chat com streaming SSE).

## What Changes

- **Setup da Aplicação SPA**: Inicialização do projeto frontend em `app/frontend` com **Vite + React 18/19 (TypeScript)** e estilização funcional com **Tailwind CSS**.
- **Configuração de Proxy de API**: Configuração do `vite.config.ts` com proxy reverso apontando `/api/v1` para o backend (`http://localhost:8080`), eliminando problemas de CORS em desenvolvimento.
- **Camada de Cliente HTTP & Auth**: Cliente `fetch` modular configurado para anexar o Bearer Token JWT e Contexto de Autenticação (`AuthContext`) que gerencia a sessão stateless no frontend.
- **Tela 1 - Autenticação SSO (`LoginPage`)**:
  - Interface minimalista centralizada com os botões **Login Google** e **Login GitHub** apontando para o fluxo Cognito.
- **Tela 2 - Dashboard de Notebooks (`DashboardPage`)**:
  - Cabeçalho com título `Notebooks`, indicador de usuário e botão `[ Criar ]`.
  - Grid com cards de notebooks exibindo nome, descrição e botão `[ Abrir ]` que navega para o workspace do notebook.
  - Criação de novo notebook via modal ou formulário simples (`POST /api/v1/notebooks`).
- **Tela 3 - Workspace com Painel Duplo (`WorkspacePage`)**:
  - **Painel Esquerdo (Sources)**:
    - Listagem de fontes (`GET /api/v1/notebooks/:id/sources`) com status (`PENDING`, `PROCESSING`, `READY`).
    - Checkboxes para seleção de fontes ativas (`conv_active_sources`) sincronizadas via `PUT .../active-sources`.
    - Upload direto de arquivos (`.md`, `.docx`) via multipart form data (`POST .../sources/upload`) e adição de Web URL (`POST .../sources/url`).
  - **Painel Direito (Chat)**:
    - Histórico de turnos de conversa (`user` e `assistant`) recuperado via `GET .../messages`.
    - Caixa de prompt inferior com botão de envio `[ > ]`.
    - Recepção e renderização reativa de tokens em tempo real consumindo o endpoint SSE `POST .../messages/stream`.

## Capabilities

### New Capabilities
- `frontend-mvp`: Interface de usuário web reativa e minimalista implementando as 3 telas funcionais (Login, Dashboard de Notebooks e Workspace com Fontes e Chat SSE).

### Modified Capabilities
<!-- Nenhuma especificação de backend existente é alterada; o frontend consome estritamente os contratos de API.md -->

## Impact

- **Affected Directory**: `app/frontend/` (novo projeto SPA com Vite, React, TypeScript e Tailwind).
- **Dependencies**: React, React DOM, React Router DOM, Vite, Tailwind CSS, Lucide React (ícones básicos opcionais).
- **APIs Consumidas**:
  - `GET /api/v1/notebooks`, `POST /api/v1/notebooks`, `DELETE /api/v1/notebooks/:id`
  - `GET /api/v1/notebooks/:id/sources`, `POST /api/v1/notebooks/:id/sources/upload`, `POST /api/v1/notebooks/:id/sources/url`
  - `GET /api/v1/notebooks/:id/conversations`, `POST /api/v1/notebooks/:id/conversations`
  - `GET /api/v1/notebooks/:id/conversations/:convId/messages`
  - `PUT /api/v1/notebooks/:id/conversations/:convId/active-sources`
  - `POST /api/v1/notebooks/:id/conversations/:convId/messages/stream` (SSE)
