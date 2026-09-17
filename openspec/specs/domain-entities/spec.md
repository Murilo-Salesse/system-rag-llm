# domain-entities Specification

## Purpose
Fornece o modelo relacional de persistência JPA mapeando as entidades de domínio do NotebookLM (Users, Notebooks, Sources, SourceChunks com pgvector, Conversations com active sources e ConversationMessages) com integridade referencial e suporte a busca vetorial.

## Requirements

### Requirement: Mapeamento e Persistência de Usuários
O sistema SHALL persistir a entidade `User` sincronizada com o Cognito contendo `id` (UUID), `cognito_sub`, `email`, `name` e `created_at`.

#### Scenario: Persistir novo usuário
- **WHEN** um novo usuário autenticado pelo Cognito é registrado
- **THEN** a entidade é persistida no banco com `id` gerado via UUID e timestamp de criação.

### Requirement: Mapeamento e Persistência de Notebooks
O sistema SHALL persistir a entidade `Notebook` associada ao seu `User` proprietário (`owner_id`), com controle de criação e atualização temporal.

#### Scenario: Persistir notebook com proprietário
- **WHEN** um notebook é criado por um usuário
- **THEN** o notebook é salvo com relacionamento `@ManyToOne` para o usuário e timestamps atualizados.

### Requirement: Mapeamento de Sources e Ciclo de Vida Assíncrono
O sistema SHALL persistir fontes associadas a um notebook contendo tipo (`MARKDOWN`, `DOCX`, `WEB_URL`), status (`PENDING`, `PROCESSING`, `READY`, `FAILED`), referências S3/URL e mensagens de erro.

#### Scenario: Transição de status da fonte
- **WHEN** uma fonte é processada assincronamente
- **THEN** o status é atualizado de `PENDING` para `PROCESSING` e subsequentemente para `READY` ou `FAILED`.

### Requirement: Mapeamento de SourceChunks com Vetor HNSW (pgvector)
O sistema SHALL persistir fragmentos de texto associados a uma fonte com índice sequencial (`chunk_index`), texto limpo e vetor de embedding de 1536 dimensões compatível com o tipo `vector` do PostgreSQL.

#### Scenario: Persistir chunk com vetor embedding
- **WHEN** um fragmento textual de ~800 tokens com vetor float[1536] é gerado
- **THEN** o chunk é persistido com o vetor armazenado na coluna `vector(1536)` indexada via HNSW.

### Requirement: Mapeamento de Conversas e Fontes Ativas
O sistema SHALL persistir conversas de um notebook e gerenciar a relação Many-to-Many de fontes ativas via tabela de junção `conv_active_sources`.

#### Scenario: Associar fontes ativas a uma conversa
- **WHEN** o usuário seleciona fontes específicas para uma sessão de chat
- **THEN** as fontes selecionadas são associadas à conversa através da tabela de junção `conv_active_sources`.

### Requirement: Mapeamento de Mensagens da Conversa
O sistema SHALL persistir mensagens em ordem temporal com papel do autor restrito aos valores `user` e `assistant`.

#### Scenario: Registrar mensagem enviada
- **WHEN** uma mensagem do usuário ou resposta do assistente é gravada
- **THEN** a mensagem é persistida associada à conversa com role e timestamp de envio.
