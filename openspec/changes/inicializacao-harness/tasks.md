## 1. Configuração do Harness e Governança SDD

- [x] 1.1 Inicializar framework OpenSpec e ferramentas de IA (Claude Code, Antigravity)
- [x] 1.2 Configurar `config.yaml` e `openspec/config.yaml` com stack AWS, Java 21 e regras SDD
- [x] 1.3 Criar diretrizes e regras inegociáveis para agentes de IA em `AGENTS.md`
- [x] 1.4 Criar registro histórico de decisões de arquitetura e restrições em `MEMORY.md`
- [x] 1.5 Criar documentação principal do repositório em `README.md` (pt-br)

## 2. Documentação Canônica de Arquitetura

- [x] 2.1 Mapear Telas (1: Login SSO, 2: Dashboard Notebooks, 3: Workspace Sources/Chat SSE)
- [x] 2.2 Elaborar diagrama de Entidades e Relacionamentos (ERD em Mermaid)
- [x] 2.3 Escrever DDL PostgreSQL 16 com extensão `vector`, índices relacionais e índice HNSW vetorial
- [x] 2.4 Documentar fluxo de ingestão assíncrona (Async Request-Reply: HTTP 202 Accepted)
- [x] 2.5 Documentar fluxo RAG com filtro de fontes ativas e streaming SSE (`text/event-stream`)
- [x] 2.6 Especificar contratos funcionais REST HTTP completos de todos os endpoints em `ARCHITECTURE.md`
- [x] 2.7 Documentar abstração de provedor de LLM transparente e invisível ao usuário

## 3. Especificações Formais OpenSpec (SDD)

- [x] 3.1 Criar especificação formal de autenticação SSO em `openspec/specs/auth-cognito/spec.md`
- [x] 3.2 Criar especificação formal de notebooks em `openspec/specs/notebook-management/spec.md`
- [x] 3.3 Criar especificação formal de ingestão assíncrona em `openspec/specs/source-ingestion/spec.md`
- [x] 3.4 Criar especificação formal de chat RAG e SSE em `openspec/specs/chat-sse-rag/spec.md`
- [x] 3.5 Elaborar contrato OpenAPI 3.0 completo em `openspec/api/openapi.yaml`
- [x] 3.6 Validar especificações com o comando `openspec validate --specs`

## 4. Ambiente Local de Infraestrutura

- [x] 4.1 Criar `docker-compose.yml` com serviço PostgreSQL 16 e extensão `pgvector`
- [x] 4.2 Adicionar serviço LocalStack para emulação local do bucket Amazon S3
- [x] 4.3 Configurar arquivo `.gitignore` para o repositório
