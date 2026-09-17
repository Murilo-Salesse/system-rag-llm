## Why

O banco de dados PostgreSQL com `pgvector` e as tabelas do schema relacional foram inicializados via scripts DDL locais, mas o backend Spring Boot ainda não possui suas entidades JPA mapeadas. Para permitir a persistência, integridade referencial, consultas de negócio e busca vetorial (RAG), é necessário criar o modelo de entidades de persistência JPA de acordo com a especificação formal do `DOMAIN.md`.

## What Changes

- Criação da nova dependência `com.pgvector:pgvector` no `pom.xml` para mapear tipos vetoriais nativos (`PGvector`) no Hibernate/JPA (Opção A).
- Implementação dos enums de domínio:
  - `SourceType` (`MARKDOWN`, `DOCX`, `WEB_URL`)
  - `SourceStatus` (`PENDING`, `PROCESSING`, `READY`, `FAILED`)
  - `MessageRole` (`user`, `assistant`)
- Implementação das entidades JPA mapeando fielmente as tabelas do DDL:
  - `User`: Mapeia a tabela `users` (`id`, `cognito_sub`, `email`, `name`, `created_at`).
  - `Notebook`: Mapeia a tabela `notebooks` (`id`, `owner`, `name`, `description`, `created_at`, `updated_at`).
  - `Source`: Mapeia a tabela `sources` (`id`, `notebook`, `name`, `type`, `s3_key`, `url`, `status`, `error_message`, `created_at`).
  - `SourceChunk`: Mapeia a tabela `source_chunks` (`id`, `source`, `content`, `embedding` tipo `vector(1536)` via `PGvector`, `chunk_index`, `created_at`).
  - `Conversation`: Mapeia a tabela `conversations` (`id`, `notebook`, `created_at`) e o relacionamento `@ManyToMany` com `Source` via tabela de junção `conv_active_sources` (Opção 1).
  - `ConversationMessage`: Mapeia a tabela `conversation_messages` (`id`, `conversation`, `role`, `content`, `created_at`).
- Criação dos Spring Data JPA Repositories para cada entidade agregada:
  - `UserRepository`, `NotebookRepository`, `SourceRepository`, `SourceChunkRepository`, `ConversationRepository`, `ConversationMessageRepository`.
- Testes unitários para validar construtores, enums, métodos de domínio e integridade de mapeamento, garantindo 100% de cobertura de código e 100% mutation kill rate via skill `java-quality-gate`.

## Capabilities

### New Capabilities
- `domain-entities`: Define as entidades JPA de persistência do domínio (User, Notebook, Source, SourceChunk com pgvector, Conversation com active sources e ConversationMessage).

### Modified Capabilities
<!-- Nenhuma especificação de contrato público/API existente foi modificada -->

## Impact

- **Código Afetado:** Novo pacote `github.salessew.notebooklm.domain` com subpacotes `entity`, `enums` e `repository`.
- **Dependências (`pom.xml`):** Adição da dependência `com.pgvector:pgvector:0.1.6`.
- **Statelessness / Latency / AWS:** Nenhum impacto na arquitetura stateless ou de autenticação Cognito; melhora a latência e o consumo de memória ao adotar lazy loading e evitar coleções desnecessárias.
- **DDL / Banco de Dados:** Mapeamento drop-in 100% compatível com o DDL existente em `app/backend-api/local/init.sql` e `DOMAIN.md`.
