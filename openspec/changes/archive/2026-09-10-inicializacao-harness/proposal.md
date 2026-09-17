## Why
Inicializar o harness de engenharia e definir a arquitetura e especificações de sistema (SDD via OpenSpec) para o clone simplificado do NotebookLM. O projeto requer conformidade com os requisitos de nuvem AWS stateless, autenticação Cognito SSO (Google/GitHub), ingestão de fontes multiformato (Markdown, DOCX, Web URL) com processamento assíncrono (Async Request-Reply), chat conversacional com seleção granular de fontes ativas e streaming SSE sobre PostgreSQL com extensão pgvector.

## What Changes
- Configuração do framework OpenSpec no repositório com suporte a ferramentas de IA (Claude Code, Antigravity).
- Definição do documento canônico de arquitetura `ARCHITECTURE.md` contendo ERD, schemas DDL PostgreSQL com extensão pgvector e contratos funcionais de endpoints REST HTTP e streaming SSE.
- Criação das diretrizes operacionais em `AGENTS.md` e histórico de decisões arquiteturais em `MEMORY.md`.
- Especificações formais em `openspec/specs/` cobrindo autenticação, notebooks, ingestão de fontes e chat RAG.
- Criação do contrato OpenAPI 3.0 em `openspec/api/openapi.yaml`.
- Configuração do ambiente local com `docker-compose.yml` (PostgreSQL pgvector e LocalStack S3).

## Capabilities

### New Capabilities
- `auth-cognito`: Autenticação SSO stateless com Google e GitHub via AWS Cognito User Pool e validação JWT via JWKS.
- `notebook-management`: Gerenciamento e isolamento multitenancy de notebooks como unidades lógicas de agrupamento de fontes.
- `source-ingestion`: Ingestão assíncrona (Async Request-Reply) de arquivos Markdown, DOCX e Web URLs para o bucket S3 e fragmentação em chunks no pgvector.
- `chat-sse-rag`: Chat conversacional com busca vetorial filtrada por fontes ativas, persistência de histórico e streaming via Server-Sent Events (SSE).

### Modified Capabilities
<!-- Nenhuma capacidade existente modificada (inicialização inicial) -->

## Impact
- Define todos os contratos de interface REST e eventos SSE para o frontend e backend.
- Modela as tabelas relacionais e índice vetorial HNSW no PostgreSQL 16 com pgvector.
- Estabelece as regras inegociáveis para agentes e desenvolvedores trabalhando no projeto.
