# NotebookLM Simplificado (`system-rag-llm`)

Plataforma de **System Design & RAG (Retrieval-Augmented Generation)** inspirada no Google NotebookLM, projetada para arquitetura em nuvem **AWS**, alta escalabilidade, modelo **100% stateless**, banco de dados **PostgreSQL com extensão pgvector** e desenvolvimento orientado a especificações (**OpenSpec / SDD**).

---

## 1. Visão Geral do Sistema

O objetivo deste projeto é fornecer uma ferramenta de anotações inteligentes e chat conversacional com IA sobre fontes de dados do usuário (documentos e páginas web).

```
                             ARQUITETURA CLOUD AWS
  +------------------+         +---------------------+
  |   Frontend Web   |  OIDC   |  AWS Cognito IdP    |
  |  (React/Next.js) +<=======>+ - Login Google      |
  +--------+---------+         | - Login GitHub      |
           |                   +---------------------+
           | Bearer JWT (Stateless)
           v
  +------------------+
  |  API Gateway     |
  +--------+---------+
           |
           v
  +------------------+
  | Application      |
  | Load Balancer    |
  +--------+---------+
           |
           v
  +-------------------------------------------------------------+
  | Backend Stateless (Spring Boot 3.3+ & Spring AI)            |
  | - Validação de JWT via JWKS do Cognito (Sem sessão local)   |
  | - Ingestão Assíncrona (Async Request-Reply: HTTP 202)       |
  | - Chat RAG com Seleção Dinâmica de Sources e Streaming SSE  |
  +----+----------------------+--------------------------+------+
       |                      |                          |
       v                      v                          v
  +----+------+        +------+-------+        +---------+---------+
  | Amazon S3 |        | PostgreSQL   |        | Provedor de LLM   |
  | (Raw Docs)|        | + pgvector   |        | (Invisível)       |
  |           |        | (HNSW Index) |        | Bedrock/OpenRouter|
  +-----------+        +--------------+        +-------------------+
```

---

## 2. Telas da Aplicação (Wireframes)

| Tela | Título | Descrição |
|---|---|---|
| **Tela 1** | **Autenticação SSO** | Interface limpa e centralizada com opções de login federado: **Login Google** e **Login GitHub** integrados via **AWS Cognito User Pool**. |
| **Tela 2** | **Dashboard de Notebooks** | Listagem em cards dos notebooks do usuário ("Notebook 1", "Notebook 2", "Notebook 3"), botão **+ Criar** e acesso ao perfil. Cada notebook atua como unidade lógica de agrupamento. |
| **Tela 3** | **Workspace (Fontes & Chat)** | **Painel Esquerdo (Sources):** listagem de fontes anexadas (`document.docx`, `.md`, URLs) com checkboxes para seleção dinâmica de fontes ativas na conversa e botão de upload.<br/>**Painel Direito (Chat):** histórico de mensagens e área de prompt com streaming em tempo real via **Server-Sent Events (SSE)**. |

---

## 3. Diretrizes e Decisões de Arquitetura

1. **Stateless por Design:** O backend nunca mantém sessões HTTP em memória. Toda requisição é autenticada através do cabeçalho `Authorization: Bearer <jwt>`, validado contra as chaves públicas (JWKS) do Cognito.
2. **Ingestão Assíncrona (Async Request-Reply):** O upload de fontes responde imediatamente com **`HTTP 202 Accepted`** e executa a extração de texto, chunking (`TokenTextSplitter`) e cálculo de embeddings em background.
3. **Formatos Suportados:** Apenas arquivos **Markdown** (`.md`, `.markdown`), documentos **Word** (`.docx`) e **Web URLs** públicas.
4. **Seleção Granular de Fontes:** O usuário escolhe quais fontes ativam o contexto do RAG na Tela 3. A query no `pgvector` filtra:
   ```sql
   WHERE notebook_id = :id AND source_id IN (:activeSourceIds)
   ```
5. **Histórico Persistente:** Todas as interações são salvas nas tabelas `chat_sessions` e `chat_messages` e enviadas como histórico recente na janela de contexto do LLM.
6. **Provedor de LLM Invisível:** O provedor (AWS Bedrock, OpenRouter ou OpenAI) é definido na infraestrutura/configuração do sistema, sem exposição ao usuário final.
7. **Streaming Real-time via SSE:** Streaming de tokens contínuo via `text/event-stream`.

---

## 4. Estrutura do Repositório e Documentação

```
system-rag-llm/
├── ARCHITECTURE.md                 # Visão de alto nível da arquitetura AWS, componentes e fluxos
├── DOMAIN.md                       # Regras de negócio, modelo de dados (ERD ASCII), tabelas e DDL pgvector
├── API.md                          # Contratos funcionais REST HTTP e streaming Server-Sent Events (SSE)
├── CLAUDE.md                       # Índice de arquivos do repositório e guia para Claude Code
├── README.md                       # Apresentação do projeto em pt-br e visão geral das telas
├── AGENTS.md                       # Diretrizes e regras inegociáveis para Agentes de IA
├── MEMORY.md                       # Registro histórico de decisões de arquitetura (ADRs)
├── config.yaml                     # Configuração global do Harness do projeto
├── docker-compose.yml              # Ambiente local (PostgreSQL 16 com pgvector + LocalStack S3)
├── system-rag.drawio               # Diagramas originais de arquitetura e telas
└── openspec/                       # Framework de Spec-Driven Development (SDD)
    ├── config.yaml                 # Configuração de governança e validação do OpenSpec
    ├── api/
    │   └── openapi.yaml            # Contrato OpenAPI 3.0 completo da API
    ├── specs/                      # Especificações formais por capacidade
    │   ├── auth-cognito/spec.md    # Autenticação SSO Stateless
    │   ├── notebook-management/    # Gerenciamento e isolamento de Notebooks
    │   ├── source-ingestion/       # Ingestão assíncrona (MD/DOCX/URL) e S3
    │   └── chat-sse-rag/           # Chat RAG, histórico e SSE Streaming
    └── changes/                    # Histórico e propostas de mudanças controladas
```

---

## 5. Harness de Engenharia & Comandos Úteis

### Validação das Especificações SDD
```bash
# Validar todas as especificações formais e propostas do OpenSpec
openspec validate --all

# Listar especificações ativas
openspec list --specs
```

### Ambiente Local de Infraestrutura
```bash
# Iniciar banco de dados PostgreSQL com extensão pgvector e LocalStack S3
docker compose up -d

# Parar os serviços
docker compose down
```

---

## 6. Documentos de Referência Detalhada

- **[`ARCHITECTURE.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/ARCHITECTURE.md)**: Visão macro da nuvem AWS, serviços e diagramas de sequência.
- **[`DOMAIN.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/DOMAIN.md)**: Regras de negócio (RN-01 a RN-06), mapa de caixas ASCII do ERD, dicionário e DDL PostgreSQL 16 com pgvector.
- **[`API.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/API.md)**: Contratos detalhados de endpoints REST HTTP e canal de streaming SSE.
- **[`CLAUDE.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/CLAUDE.md)**: Índice completo da árvore de arquivos e diretrizes de IA.
- **[`AGENTS.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/AGENTS.md)**: Regras operacionais e diretrizes inegociáveis para agentes.
- **[`MEMORY.md`](file:///Users/ltcloud/Documents/Outros/Java/llm/MEMORY.md)**: Histórico de decisões arquiteturais (ADR-001 a ADR-007).

