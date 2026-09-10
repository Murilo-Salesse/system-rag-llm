# Modelo de Domínio e Regras de Negócio (DOMAIN.md)

Este documento define o modelo de dados conceitual, lógico e físico, os relacionamentos e as regras de negócio para a plataforma do **NotebookLM Simplificado**.

---

## 1. Regras de Negócio do Sistema

### RN-01: Isolamento de Dados por Usuário (Multitenancy)
- Cada usuário é identificado unicamente pelo atributo `cognito_sub` emitido pelo AWS Cognito após o login SSO (Google ou GitHub).
- Notebooks e seus recursos subordinados (fontes, fragmentos vetoriais, conversas, mensagens e fontes ativas) pertencem exclusivamente ao usuário que os criou (`owner_id`).
- Um usuário nunca pode listar, ler, modificar ou deletar recursos pertencentes a outros usuários. Tentativas de acesso não autorizado resultam em `HTTP 404 Not Found` (para prevenir enumeração) ou `HTTP 403 Forbidden`.

### RN-02: Formatos de Fontes Estritamente Permitidos
- O sistema aceita exclusivamente três tipos de fontes de dados:
  1. **`MARKDOWN`**: Arquivos com extensão `.md` ou `.markdown`.
  2. **`DOCX`**: Documentos do Microsoft Word (`.docx`), lidos via Apache POI / Tika.
  3. **`WEB_URL`**: Páginas públicas da web informadas via URL válida (`http://` ou `https://`), com conteúdo extraído de forma limpa via Jsoup.
- Qualquer outro formato de arquivo (ex: `.pdf`, `.txt`, `.exe`, imagens) deve ser rejeitado imediatamente no endpoint de upload com `HTTP 400 Bad Request`.

### RN-03: Ciclo de Vida da Fonte (Async Request-Reply)
- O processamento de fontes nunca bloqueia a requisição HTTP do usuário. O status da fonte obedece à máquina de estados:
  ```
  [PENDING]  -->  [PROCESSING]  -->  [READY]
                               \
                                -->  [FAILED]
  ```
  - **`PENDING`**: O arquivo bruto foi salvo com sucesso no bucket Amazon S3 (ou URL enfileirada) e a fonte foi registrada no banco de dados.
  - **`PROCESSING`**: O worker em background está ativamente executando o parsing textual, o chunking e a geração de embeddings.
  - **`READY`**: Todos os fragmentos foram indexados na tabela `source_chunks` com seus respectivos vetores `pgvector`. A fonte está pronta para ser selecionada e utilizada nas conversas.
  - **`FAILED`**: Ocorreu um erro irrecuperável durante a leitura, scraping ou geração de vetores. O campo `error_message` armazena a causa do erro.

### RN-04: Seleção Granular de Fontes Ativas na Conversa (`conv_active_sources`)
- Cada conversa (`conversations`) pode ter um conjunto customizado de fontes ativas selecionadas pelo usuário na Tela 3.
- As fontes ativas são persistidas na tabela de junção `conv_active_sources` (`conversation_id`, `source_id`).
- Ao executar a busca semântica por similaridade vetorial (`similaritySearch`), a query junta os fragmentos com as fontes ativas da conversa:
  ```sql
  SELECT sc.content FROM source_chunks sc
  JOIN conv_active_sources cas ON cas.source_id = sc.source_id
  JOIN sources s ON s.id = sc.source_id
  WHERE cas.conversation_id = :conversationId AND s.status = 'READY'
  ORDER BY sc.embedding <=> :queryEmbedding
  LIMIT 5;
  ```
- Se nenhuma fonte estiver vinculada em `conv_active_sources` para aquela conversa, o sistema considera automaticamente todas as fontes com status `READY` vinculadas ao notebook correspondente.

### RN-05: Histórico de Conversas e Mensagens
- As conversas dentro de um notebook são representadas pela entidade `conversations`.
- Cada turno da interação é gravado em `conversation_messages` com o papel correspondente (`user` ou `assistant`).
- As mensagens anteriores da conversa são recuperadas ordenadas por `created_at ASC` e alimentadas na memória de contexto do LLM para permitir diálogo contínuo.

### RN-06: Chunking Semântico e Vetorização
- Os textos extraídos são divididos em fragmentos sequenciais utilizando `TokenTextSplitter` com tamanho alvo de ~800 tokens e sobreposição (*overlap*) de 100 tokens para preservar a continuidade de sentido entre trechos adjacentes.
- Cada fragmento gera um vetor float32 de 1536 dimensões compatível com o modelo de embedding do sistema (ex: `text-embedding-3-small`, Titan Embeddings) e indexado via **HNSW** (`vector_cosine_ops`) no PostgreSQL.

---

## 2. Modelo de Dados (ERD)

```
+---------------+      +-------------------+      +---------------------+
|     users     | 1  N |     notebooks     | 1  N |       sources       |
+---------------+------+-------------------+------+---------------------+
| id (PK)       |      | id (PK)           |      | id (PK)             |
| cognito_sub   |      | owner_id (FK)     |      | notebook_id (FK)    |
| email         |      | name              |      | name                |
| name          |      | description       |      | type                |
| created_at    |      | created_at        |      | s3_key              |
+---------------+      | updated_at        |      | url                 |
                       +---------+---------+      | status              |
                                 |                | error_message       |
                                 | 1              | created_at          |
                                 |                +----------+----------+
                                 |                           |
                                 |                           | 1
                                 |                           |
                                 |                           | N
                                 |                +----------v----------+
                                 |                |    source_chunks    |
                                 |                +---------------------+
                                 |                | id (PK)             |
                                 |                | source_id (FK)      |
                                 |                | content             |
                                 |                | embedding (vector)  |
                                 |                | chunk_index         |
                                 |                | created_at          |
                                 |                +---------------------+
                                 |
                                 | N
                       +---------v---------+
                       |   conversations   |
                       +-------------------+
                       | id (PK)           |
                       | notebook_id (FK)  |
                       | created_at        |
                       +---+-----------+---+
                           |           |
                         1 |           | 1
                           |           |
                         N |           | N
        +------------------v--+     +--v--------------------+
        | conv_active_sources |     | conversation_messages |
        +---------------------+     +-----------------------+
        | conversation_id(PK) |     | id (PK)               |
        | source_id (PK, FK)  |     | conversation_id (FK)  |
        +---------------------+     | role (user|assistant) |
                                    | content               |
                                    | created_at            |
                                    +-----------------------+
```

---

## 3. Relacionamentos por Escrito

1. **`users` (1) $\longrightarrow$ (N) `notebooks`**
   - **Descrição:** Um usuário autenticado pode possuir múltiplos notebooks.
   - **Chave:** `notebooks.owner_id` referencia `users.id`.
   - **Regra:** `ON DELETE CASCADE`.

2. **`notebooks` (1) $\longrightarrow$ (N) `sources`**
   - **Descrição:** Um notebook funciona como agrupador lógico de múltiplas fontes documentais (Markdown, DOCX, Web URLs).
   - **Chave:** `sources.notebook_id` referencia `notebooks.id`.
   - **Regra:** `ON DELETE CASCADE`.

3. **`sources` (1) $\longrightarrow$ (N) `source_chunks`**
   - **Descrição:** Cada documento fonte é particionado em múltiplos fragmentos com embeddings vetoriais.
   - **Chave:** `source_chunks.source_id` referencia `sources.id`.
   - **Regra:** `ON DELETE CASCADE`.

4. **`notebooks` (1) $\longrightarrow$ (N) `conversations`**
   - **Descrição:** Um notebook hospeda múltiplas conversas com o assistente de IA.
   - **Chave:** `conversations.notebook_id` referencia `notebooks.id`.
   - **Regra:** `ON DELETE CASCADE`.

5. **`conversations` (1) $\longrightarrow$ (N) `conv_active_sources`**
   - **Descrição:** Uma conversa associa-se às fontes específicas ativadas pelo usuário na Tela 3.
   - **Chave:** `conv_active_sources.conversation_id` referencia `conversations.id`.
   - **Regra:** `ON DELETE CASCADE`.

6. **`sources` (1) $\longrightarrow$ (N) `conv_active_sources`**
   - **Descrição:** Uma fonte pode estar ativa em múltiplas conversas.
   - **Chave:** `conv_active_sources.source_id` referencia `sources.id`.
   - **Regra:** `ON DELETE CASCADE`.

7. **`conversations` (1) $\longrightarrow$ (N) `conversation_messages`**
   - **Descrição:** Uma conversa contém a lista de mensagens trocadas entre o usuário e o assistente.
   - **Chave:** `conversation_messages.conversation_id` referencia `conversations.id`.
   - **Regra:** `ON DELETE CASCADE`.

---

## 4. Dicionário de Tabelas e Atributos

#### Tabela: `users`
Armazena os usuários autenticados via AWS Cognito.

| Coluna | Tipo | Chave | Nulo? | Descrição |
|---|---|---|---|---|
| `id` | `UUID` | **PK** | Não | Identificador primário do usuário. |
| `cognito_sub` | `VARCHAR(255)` | **UK** | Não | Identificador único (`sub`) emitido pelo AWS Cognito. |
| `email` | `VARCHAR(255)` | **UK** | Não | E-mail do usuário obtido via federação Google/GitHub. |
| `name` | `VARCHAR(255)` | | Sim | Nome completo do usuário. |
| `created_at` | `TIMESTAMPTZ` | | Não | Data e hora do primeiro registro. |

#### Tabela: `notebooks`
Unidade lógica de agrupamento de fontes e conversas (Tela 2).

| Coluna | Tipo | Chave | Nulo? | Descrição |
|---|---|---|---|---|
| `id` | `UUID` | **PK** | Não | Identificador primário do notebook. |
| `owner_id` | `UUID` | **FK** | Não | Chave estrangeira referenciando `users(id)`. |
| `name` | `VARCHAR(255)` | | Não | Nome de exibição do notebook (ex: "Notebook 1"). |
| `description` | `TEXT` | | Sim | Descrição opcional do conteúdo do notebook. |
| `created_at` | `TIMESTAMPTZ` | | Não | Data e hora de criação. |
| `updated_at` | `TIMESTAMPTZ` | | Não | Data e hora da última modificação. |

#### Tabela: `sources`
Documentos e links anexados a um notebook (Tela 3 - Painel Esquerdo).

| Coluna | Tipo | Chave | Nulo? | Descrição |
|---|---|---|---|---|
| `id` | `UUID` | **PK** | Não | Identificador primário da fonte. |
| `notebook_id` | `UUID` | **FK** | Não | Chave estrangeira referenciando `notebooks(id)`. |
| `name` | `VARCHAR(255)` | | Não | Nome do arquivo ou título da URL. |
| `type` | `VARCHAR(50)` | | Não | Formato permitido: `MARKDOWN`, `DOCX` ou `WEB_URL`. |
| `s3_key` | `VARCHAR(1024)` | | Sim | Chave do objeto salvo no Amazon S3. |
| `url` | `TEXT` | | Sim | URL pública original (quando `type = WEB_URL`). |
| `status` | `VARCHAR(50)` | | Não | Status assíncrono: `PENDING`, `PROCESSING`, `READY`, `FAILED`. |
| `error_message` | `TEXT` | | Sim | Detalhes do erro caso o processamento falhe. |
| `created_at` | `TIMESTAMPTZ` | | Não | Data e hora de inclusão da fonte. |

#### Tabela: `source_chunks`
Fragmentos de texto e vetores de embedding para busca semântica RAG com `pgvector`.

| Coluna | Tipo | Chave | Nulo? | Descrição |
|---|---|---|---|---|
| `id` | `UUID` | **PK** | Não | Identificador primário do fragmento. |
| `source_id` | `UUID` | **FK** | Não | Chave estrangeira referenciando `sources(id)`. |
| `content` | `TEXT` | | Não | Conteúdo textual limpo do fragmento (~800 tokens). |
| `embedding` | `vector(1536)` | | Sim | Vetor numérico gerado pelo modelo e indexado via HNSW. |
| `chunk_index` | `INT` | | Não | Ordem sequencial do fragmento dentro do documento. |
| `created_at` | `TIMESTAMPTZ` | | Não | Data e hora de indexação do vetor. |

#### Tabela: `conversations`
Linhas de conversa mantidas dentro de um notebook.

| Coluna | Tipo | Chave | Nulo? | Descrição |
|---|---|---|---|---|
| `id` | `UUID` | **PK** | Não | Identificador primário da conversa. |
| `notebook_id` | `UUID` | **FK** | Não | Chave estrangeira referenciando `notebooks(id)`. |
| `created_at` | `TIMESTAMPTZ` | | Não | Data e hora de criação da conversa. |

#### Tabela: `conv_active_sources`
Tabela de junção que define quais fontes estão ativas em uma conversa específica.

| Coluna | Tipo | Chave | Nulo? | Descrição |
|---|---|---|---|---|
| `conversation_id`| `UUID` | **PK, FK** | Não | Chave estrangeira referenciando `conversations(id)`. |
| `source_id` | `UUID` | **PK, FK** | Não | Chave estrangeira referenciando `sources(id)`. |

#### Tabela: `conversation_messages`
Turnos individuais de mensagens dentro de uma conversa.

| Coluna | Tipo | Chave | Nulo? | Descrição |
|---|---|---|---|---|
| `id` | `UUID` | **PK** | Não | Identificador primário da mensagem. |
| `conversation_id`| `UUID` | **FK** | Não | Chave estrangeira referenciando `conversations(id)`. |
| `role` | `VARCHAR(50)` | | Não | Papel do autor: `user` ou `assistant`. |
| `content` | `TEXT` | | Não | Texto completo da mensagem. |
| `created_at` | `TIMESTAMPTZ` | | Não | Data e hora de envio da mensagem. |

---

## 5. DDL do Banco de Dados (PostgreSQL 16 + pgvector)

```sql
-- Extensões necessárias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- 1. Tabela de Usuários (Sincronizada via JWT Claims do Cognito)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cognito_sub VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Tabela de Notebooks (Unidade lógica de agrupamento)
CREATE TABLE notebooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_notebooks_owner_id ON notebooks(owner_id);

-- 3. Tabela de Fontes (Sources anexadas ao Notebook)
CREATE TABLE sources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    notebook_id UUID NOT NULL REFERENCES notebooks(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('MARKDOWN', 'DOCX', 'WEB_URL')),
    s3_key VARCHAR(1024),
    url TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_sources_notebook_id ON sources(notebook_id);
CREATE INDEX idx_sources_status ON sources(status);

-- 4. Tabela de Fragmentos e Vetores (Source Chunks com pgvector)
CREATE TABLE source_chunks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_id UUID NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    embedding vector(1536), -- Vetor float32 gerado pelo modelo de embeddings
    chunk_index INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Índices relacionais e índice vetorial HNSW com Cosine Distance (<=>)
CREATE INDEX idx_chunks_source_id ON source_chunks(source_id);
CREATE INDEX idx_chunks_embedding_hnsw ON source_chunks 
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 5. Tabela de Conversas
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    notebook_id UUID NOT NULL REFERENCES notebooks(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_conversations_notebook_id ON conversations(notebook_id);

-- 6. Tabela de Fontes Ativas na Conversa
CREATE TABLE conv_active_sources (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    source_id UUID NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    PRIMARY KEY (conversation_id, source_id)
);
CREATE INDEX idx_conv_active_sources_source ON conv_active_sources(source_id);

-- 7. Tabela de Mensagens da Conversa
CREATE TABLE conversation_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL CHECK (role IN ('user', 'assistant')),
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_conversation_messages_conv ON conversation_messages(conversation_id, created_at ASC);
```
