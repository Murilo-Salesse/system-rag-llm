# Memória do Projeto (MEMORY.md)

Este documento registra o contexto histórico, as decisões arquiteturais (ADRs), o estado atual do sistema e as diretrizes de evolução do **NotebookLM Simplificado**.

---

## 1. Mapa Conceitual do Domínio

- **Notebook:** Unidade lógica de agrupamento pertencente a um usuário. Isola conjuntos de fontes documentais e conversas.
- **Source:** Documento bruto ingerido (Markdown, DOCX ou Web URL). Armazenado no S3 e processado assincronamente.
- **Document Chunk:** Fragmento semântico de texto (~800 tokens com overlap de 100) associado a um vetor numérico de 1536 dimensões (`vector(1536)`).
- **Chat Session:** Linha temporal de conversa dentro de um Notebook, mantendo histórico de perguntas do usuário e respostas do assistente.
- **Active Sources:** Subconjunto de fontes selecionadas pelo usuário no frontend (Tela 3) para delimitar o contexto da busca por similaridade vetorial.

---

## 2. Decisões Arquiteturais Registradas (ADRs)

### ADR-001: Autenticação Stateless via AWS Cognito e JWKS
- **Decisão:** O backend Spring Boot opera puramente como OAuth2 Resource Server. O frontend obtém JWTs do Cognito (Google/GitHub IdPs) e envia via header `Authorization: Bearer <token>`.
- **Justificativa:** Garante escalabilidade horizontal sem necessidade de sessão sticky ou cache distribuído de sessões (ex: Redis).

### ADR-002: Padrão Async Request-Reply para Ingestão de Documentos
- **Decisão:** O upload de arquivos (MD/DOCX) e envio de URLs responde com `HTTP 202 Accepted` acompanhado do status `PENDING` e header `Location`. O processamento (parsing, chunking e embeddings) roda em background.
- **Justificativa:** Evita estouro de timeout no API Gateway (limite rígido de 29s) e protege a aplicação contra picos de tráfego e latências de rede de scraping ou APIs de embedding.

### ADR-003: PostgreSQL 16 com pgvector e Índice HNSW
- **Decisão:** Unificar os dados relacionais (usuários, notebooks, fontes, mensagens) e o armazenamento vetorial no mesmo banco PostgreSQL utilizando a extensão `pgvector` com índice HNSW (`vector_cosine_ops`).
- **Justificativa:** Reduz complexidade operacional, elimina a necessidade de sincronização entre bancos distintos (ex: Postgres + Pinecone/Qdrant), permitindo filtros relacionais nativos (`notebook_id` e `source_id`) diretamente na query SQL.

### ADR-004: Provedor de LLM Agnóstico e Invisível via Contrato OpenAI
- **Decisão:** Utilizar a camada de abstração do Spring AI compatível com a API da OpenAI.
- **Justificativa:** Permite que a infraestrutura aponte para **AWS Bedrock**, **OpenRouter** ou **OpenAI** alterando apenas a `base-url` e `api-key` nas configurações, mantendo o provedor 100% invisível para os usuários finais.

### ADR-005: Streaming em Tempo Real com Server-Sent Events (SSE)
- **Decisão:** A interação na camada de chat utiliza `text/event-stream` (SSE) via Spring AI `ChatClient.stream()`.
- **Justificativa:** SSE é unidirecional (servidor $\rightarrow$ cliente), opera sobre HTTP comum (compatível com API Gateway e ALB), é mais simples e leve que WebSockets e oferece excelente experiência de usuário com baixa latência percebida.

### ADR-006: Seleção Granular de Sources no RAG
- **Decisão:** A requisição de chat aceita um array `activeSourceIds`. A busca por similaridade no `PgVectorStore` aplica filtro de metadados para buscar apenas chunks das fontes ativas.
- **Justificativa:** Dá ao usuário o controle fino sobre quais materiais de referência devem ser consultados para responder a cada pergunta específica.

### ADR-007: Restrição Estrita de Formatos de Fonte
- **Decisão:** Permitir unicamente arquivos **Markdown** (`.md`, `.markdown`), **DOCX** (`.docx` via Apache POI) e **Web URLs** (via scraping limpo).
- **Justificativa:** Foco no escopo de notas, documentações e páginas web de referência com alta qualidade de extração textual.

---

## 3. Estado Atual dos Artefatos

- [x] Diagrama de Engenharia de Harness (`system-rag.drawio`)
- [x] Wireframes das Telas 1 (Login), 2 (Dashboard) e 3 (Workspace) analisados
- [x] OpenSpec SDD inicializado no projeto (`openspec/`)
- [x] `config.yaml` e `openspec/config.yaml` configurados
- [x] `ARCHITECTURE.md` canônico gerado com ERD, DDL pgvector e contratos funcionais REST/SSE
- [x] `AGENTS.md` e `MEMORY.md` estabelecidos
- [ ] Especificações formais OpenSpec (`openspec/specs/*.md`) e `openapi.yaml`
- [ ] Scaffolding de código Spring Boot 3.3+ com Spring AI e Flyway migrations
