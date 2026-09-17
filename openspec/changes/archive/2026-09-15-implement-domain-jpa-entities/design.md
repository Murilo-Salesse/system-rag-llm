## Context

As tabelas do banco de dados relacional e a extensão `vector` já foram definidas e provisionadas no PostgreSQL local via scripts SQL (`init.sql` / `DOMAIN.md`). O backend Spring Boot necessita do mapeamento das entidades JPA para operações transacionais e queries de domínio.

## Goals / Non-Goals

**Goals:**
- Mapear as 6 entidades relacionais (`User`, `Notebook`, `Source`, `SourceChunk`, `Conversation`, `ConversationMessage`) e a tabela de junção `conv_active_sources`.
- Suporte ao tipo vetorial `vector(1536)` no JPA utilizando a biblioteca oficial `com.pgvector:pgvector` (`PGvector`).
- Mapear a tabela de junção `conv_active_sources` via `@ManyToMany` com `@JoinTable` direto na entidade `Conversation`.
- Criar os Spring Data Repositories correspondentes com métodos de consulta com filtros de multitenancy (`owner_id`) e busca de mensagens ordenadas por data.
- Garantir que todas as entidades e métodos de domínio passem com 100% de cobertura de código e 0 mutantes sobreviventes via `java-quality-gate`.

**Non-Goals:**
- Endpoints de API REST (Controllers) ou serviços de orquestração de upload/chat (serão implementados em specs futuras de chat e ingestão).
- Alteração no schema do banco de dados (`init.sql`).

## Decisions

### D1 — Integração com pgvector no JPA (Opção A)
**Decisão:** Adicionar a biblioteca oficial `com.pgvector:pgvector:0.1.6` ao `pom.xml` e mapear o atributo `embedding` em `SourceChunk` usando o tipo `PGvector` (com `@Column(columnDefinition = "vector(1536)")`).
**Rationale:** Permite manipular o vetor diretamente na entidade JPA de forma tipada, sem gambiarras de conversão manual de string ou dependência exclusiva de native queries puras.

### D2 — Mapeamento Many-to-Many de Fontes Ativas (Opção 1)
**Decisão:** Utilizar `@ManyToMany` direto em `Conversation`:
```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "conv_active_sources",
    joinColumns = @JoinColumn(name = "conversation_id"),
    inverseJoinColumns = @JoinColumn(name = "source_id")
)
private Set<Source> activeSources = new HashSet<>();
```
**Rationale:** Segue o princípio *Ponytail* (solução mais limpa e minimalista sem classes de junção desnecessárias, já que a tabela não possui atributos adicionais).

### D3 — Coleções Lazy e Prevenção de OOM
**Decisão:** Não mapear coleção bidirecional `List<SourceChunk>` dentro de `Source`. Em vez disso, `SourceChunk` possui `@ManyToOne(fetch = FetchType.LAZY) private Source source`.
**Rationale:** Um único documento grande pode ter milhares de chunks. Carregar `source.getChunks()` em memória pode causar `OutOfMemory`. O acesso a chunks deve sempre ser paginado via `SourceChunkRepository`.

### D4 — Timestamps e Identificadores
**Decisão:** Usar `UUID` com geração automática `@GeneratedValue(strategy = GenerationType.UUID)` e `Instant` para colunas `TIMESTAMPTZ` com `@CreationTimestamp` e `@UpdateTimestamp` do Hibernate.
**Rationale:** Compatibilidade máxima com PostgreSQL e padrão Jakarta Persistence / Java 21.

## Risks / Trade-offs

- **[Trade-off] `PGvector` e serialização JSON direta** → `PGvector` não é automaticamente serializável para JSON se exposto diretamente em DTOs.
  - *Mitigação:* As entidades não serão expostas na camada web. Os DTOs (records) nunca retornarão o vetor bruto de 1536 dimensões ao cliente.
- **[Risco] Compatibilidade do tipo de coluna `vector` no Hibernate 7 / Spring Boot 4** → O Hibernate pode exigir `@JdbcTypeCode(SqlTypes.OTHER)` ou conversor explícito caso tente interpretar o tipo SQL.
  - *Mitigação:* Usar `columnDefinition = "vector(1536)"` com o tipo `PGvector` devidamente testado na suíte de testes.
