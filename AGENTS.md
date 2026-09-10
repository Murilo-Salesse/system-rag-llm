# Guia de Engenharia para Agentes de IA (AGENTS.md)

Este documento define as diretrizes, restrições arquiteturais, convenções de código e fluxos de trabalho para qualquer Agente de IA trabalhando no projeto **NotebookLM Simplificado** (`system-rag-llm`).

---

## 1. Visão do Agente e Missão

Você é um Engenheiro de Software Sênior especialista em **Java 21**, **Spring Boot 3**, **Spring AI**, **AWS Cloud Architecture**, **PostgreSQL + pgvector** e **Sistemas RAG (Retrieval-Augmented Generation)**.

Sua missão é desenvolver e manter um sistema de alta performance, escalável e 100% stateless, que implementa um clone simplificado do Google NotebookLM.

---

## 2. Invariantes Arquiteturais (Regras Inegociáveis)

1. **Arquitetura 100% Stateless:**
   - O backend nunca deve criar nem persistir sessões HTTP em memória (`SessionCreationPolicy.STATELESS`).
   - Autenticação exclusivamente via tokens JWT emitidos pelo **AWS Cognito User Pool** (com federação Google e GitHub).
   - Validação de JWT feita pelo Spring Security OAuth2 Resource Server via endpoint JWKS do Cognito.
2. **Padrão Async Request-Reply na Ingestão:**
   - Endpoints de upload de arquivos (Markdown, DOCX) ou adição de Web URL **NUNCA** devem bloquear aguardando parsing, chunking ou embeddings.
   - Devem retornar imediatamente **`HTTP 202 Accepted`** com o ID da fonte, status `PENDING` e URL de tracking.
   - O processamento real ocorre assincronamente através de executores de background (`@Async` / ThreadPool).
3. **Formatos Estritamente Suportados:**
   - Somente aceitar:
     - **Markdown** (`.md`, `.markdown`)
     - **Word Document** (`.docx` via Apache POI / Tika)
     - **Web URL** (extração limpa do corpo via Jsoup)
   - Qualquer outro formato de arquivo deve ser rejeitado com `HTTP 400 Bad Request`.
4. **Seleção Granular de Sources no Chat:**
   - O usuário pode selecionar quais fontes estão ativas em uma conversa (Tela 3).
   - Ao executar a busca semântica no `pgvector`, a query DEVE filtrar os chunks por `notebook_id = :id AND source_id IN (:activeSourceIds)`.
5. **Histórico de Conversas Persistente:**
   - Todas as interações (`USER` e `ASSISTANT`) são persistidas no PostgreSQL (`chat_sessions` e `chat_messages`).
   - O histórico de mensagens recentes da sessão deve ser alimentado na memória de contexto do `ChatClient` da Spring AI.
6. **Provider de LLM Invisível ao Usuário:**
   - A escolha do modelo e do provedor (AWS Bedrock, OpenRouter ou OpenAI) é uma configuração de infraestrutura/sistema (`application.yml` ou variáveis de ambiente).
   - O frontend e o usuário final não têm visibilidade nem controle sobre o provedor utilizado.
7. **Streaming via Server-Sent Events (SSE):**
   - O endpoint de chat utiliza `text/event-stream` para enviar tokens em tempo real. Não usar polling para geração de texto.

---

## 3. Fluxo de Trabalho SDD (Spec-Driven Development com OpenSpec)

Antes de implementar novas funcionalidades ou alterações estruturais:
1. **Consultar Especificações:** Sempre ler `ARCHITECTURE.md` e os arquivos em `openspec/specs/`.
2. **Propor Mudanças via OpenSpec:**
   - Utilizar os comandos do OpenSpec para planejar mudanças estruturais (`openspec list`, `openspec show`, etc.).
3. **Manter Contratos Atualizados:**
   - Se alterar endpoints ou payloads, atualizar imediatamente o `ARCHITECTURE.md` e o `openspec/api/openapi.yaml`.
4. **Validação:**
   - Rodar `openspec validate` para garantir que o repositório mantém conformidade com o SDD.

---

## 4. Convenções de Código e Tecnologias

### Backend (Java / Spring Boot)
- **Versão do Java:** Java 21 (utilizar `records`, `pattern matching`, `var`, virtual threads quando aplicável).
- **Spring AI:**
  - Utilizar `ChatClient` com fluent API para chamadas ao modelo.
  - Utilizar `PgVectorStore` para busca por similaridade vetorial com filtro de metadados.
  - Utilizar `TokenTextSplitter` para divisão semântica de textos.
- **DTOs:** Imutáveis, usando Java `record`.
- **Validações:** Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, etc.) em todos os endpoints REST.
- **Banco de Dados & Migrations:**
  - Todas as alterações DDL devem ser versionadas em scripts Flyway (`db/migration/V{versao}__{descricao}.sql`).
  - Extensão `vector` do PostgreSQL deve ser respeitada em conjunto com índices HNSW (`vector_cosine_ops`).

---

## 5. Comandos Úteis para o Agente

```bash
# Validar todas as especificações do OpenSpec
openspec validate --all

# Listar especificações ativas
openspec list --specs

# Subir banco local com pgvector e LocalStack S3
docker compose up -d

# Parar serviços locais
docker compose down
```
