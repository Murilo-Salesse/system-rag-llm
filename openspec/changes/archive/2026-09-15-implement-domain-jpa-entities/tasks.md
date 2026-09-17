## 1. Dependências e Enums de Domínio

- [x] 1.1 Adicionar dependência `com.pgvector:pgvector:0.1.6` ao `pom.xml` de `app/backend-api/notebooklm` e verificar compilação com `./mvnw compile -q`
- [x] 1.2 Criar enums de domínio `SourceType`, `SourceStatus` e `MessageRole` no pacote `github.salessew.notebooklm.domain.enums` com valores correspondentes ao DDL e testes unitários cobrindo todos os valores

## 2. Entidades Principais e Relacionamentos

- [x] 2.1 Criar entidade JPA `User` mapeando tabela `users` (`id`, `cognitoSub`, `email`, `name`, `createdAt`) e `UserRepository`
- [x] 2.2 Criar entidade JPA `Notebook` mapeando tabela `notebooks` (`id`, `owner` `@ManyToOne`, `name`, `description`, `createdAt`, `updatedAt`) e `NotebookRepository` com busca por proprietário
- [x] 2.3 Criar entidade JPA `Source` mapeando tabela `sources` (`id`, `notebook` `@ManyToOne`, `name`, `type`, `s3Key`, `url`, `status`, `errorMessage`, `createdAt`) e `SourceRepository`
- [x] 2.4 Criar entidade JPA `SourceChunk` mapeando tabela `source_chunks` (`id`, `source` `@ManyToOne`, `content`, `embedding` tipo `PGvector`, `chunkIndex`, `createdAt`) e `SourceChunkRepository`
- [x] 2.5 Criar entidades JPA `Conversation` (com `@ManyToMany` para `Source` via `conv_active_sources`) e `ConversationMessage` (`id`, `conversation` `@ManyToOne`, `role`, `content`, `createdAt`) junto aos seus repositórios `ConversationRepository` e `ConversationMessageRepository`

## 3. Testes Unitários e Validação do Quality Gate

- [x] 3.1 Implementar testes unitários JUnit 5 + Mockito para as entidades e repositórios cobrindo integridade de instâncias, getters, construtores e transições de status
- [x] 3.2 Executar testes e verificar conformidade determinística com a skill `java-quality-gate` executando `./mvnw test`, `./mvnw org.pitest:pitest-maven:mutationCoverage` e `app/backend-api/local/quality-gate.sh` garantindo 100% line coverage e 100% mutation kill rate (exit 0)
