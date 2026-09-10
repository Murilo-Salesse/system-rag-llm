# Guia do Projeto (CLAUDE.md)

Índice e guia de referência para Claude Code e agentes de IA trabalhando no projeto **NotebookLM Simplificado** (`system-rag-llm`).

---

## 1. Árvore de Arquivos do Repositório

```
system-rag-llm/
├── 📄 ARCHITECTURE.md          # Visão de alto nível da arquitetura em nuvem AWS, serviços e fluxos sequenciais
├── 📄 DOMAIN.md                # Regras de negócio, modelo de dados (ERD), entidades, relacionamentos e DDL
├── 📄 API.md                   # Funcionalidades, contratos de endpoints REST HTTP e streaming SSE
├── 📄 README.md                # Apresentação geral do projeto, telas (1, 2 e 3) e guia de início
├── 📄 AGENTS.md                # Diretrizes operacionais e regras arquiteturais para agentes de IA
├── 📄 MEMORY.md                # Histórico de decisões arquiteturais (ADRs) e restrições de domínio
├── 📄 CLAUDE.md                # Este índice de navegação do repositório
├── ⚙️ config.yaml              # Configuração global do harness do projeto
├── 🐳 docker-compose.yml       # Ambiente local de infraestrutura (PostgreSQL com pgvector e LocalStack S3)
├── 🎨 system-rag.drawio        # Diagramas visuais das telas e do harness
└── 📁 openspec/                # Framework de Spec-Driven Development (SDD)
    ├── ⚙️ config.yaml          # Configuração de validação e governança do OpenSpec
    ├── 📄 api/
    │   └── openapi.yaml        # Contrato OpenAPI 3.0 formal de rotas e modelos
    ├── 📁 specs/               # Especificações formais por capacidade
    │   ├── auth-cognito/       # Especificação: Autenticação SSO Stateless via AWS Cognito
    │   ├── notebook-management/# Especificação: Gerenciamento e isolamento de Notebooks
    │   ├── source-ingestion/   # Especificação: Ingestão assíncrona (MD, DOCX, Web URL) e S3
    │   └── chat-sse-rag/       # Especificação: Chat RAG, histórico e SSE Streaming
    └── 📁 changes/             # Propostas de mudanças controladas via SDD
        └── inicializacao-harness/ # Proposta, design, tarefas e deltas da inicialização do harness
```

---

## 2. Responsabilidade dos Documentos

| Documento | Conteúdo Principal |
|---|---|
| **[`ARCHITECTURE.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/ARCHITECTURE.md)** | Visão macro da nuvem AWS, papéis de cada serviço e diagramas de sequência dos fluxos de ingestão e chat. |
| **[`DOMAIN.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/DOMAIN.md)** | Regras de negócio, modelo de dados (ERD), cardinalidades, tabelas, atributos e DDL do banco de dados com `pgvector`. |
| **[`API.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/API.md)** | Contratos das rotas HTTP (URLs, métodos, payloads JSON, status codes) e streaming SSE. |
| **[`README.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/README.md)** | Visão geral do produto, wireframes das telas (1, 2 e 3) e instruções de uso do repositório. |
| **[`AGENTS.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/AGENTS.md)** | Princípios e regras inegociáveis para agentes que atuam no repositório. |
| **[`MEMORY.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/MEMORY.md)** | Histórico e contexto das decisões de arquitetura adotadas (ADRs). |
| **[`openspec/`](file:///Users/ltcloud/Documents/Outros/Java/llm/openspec/)** | Especificações formais de requisitos e capacidades gerenciadas via OpenSpec. |

---

## 3. Diretrizes Arquiteturais

1. **Arquitetura 100% Stateless:** Autenticação exclusivamente via tokens JWT emitidos pelo AWS Cognito. Sem sessões em memória no servidor.
2. **Padrão Async Request-Reply:** Upload de fontes responde com **`HTTP 202 Accepted`** e processamento assíncrono em background.
3. **Formatos Estritamente Suportados:** Apenas arquivos **Markdown** (`.md`), **Word** (`.docx`) e **Web URLs**.
4. **Seleção Granular no Chat:** O contexto do RAG filtra exclusivamente as fontes ativadas pelo usuário na conversa.
5. **Histórico de Conversas:** Persistência de sessões e mensagens para contexto multi-turno.
6. **Provedor de LLM Transparente:** Modelo e provedor configurados na infraestrutura do sistema, sem exposição ao usuário.
7. **Streaming via SSE:** Envio contínuo de tokens de resposta em tempo real via Server-Sent Events (`text/event-stream`).

---

## 4. Comandos de Validação e Infraestrutura

```bash
# Validar todas as especificações do OpenSpec
openspec validate --all

# Listar especificações ativas
openspec list --specs

# Iniciar banco de dados e S3 localmente
docker compose up -d

# Parar serviços locais
docker compose down
```
