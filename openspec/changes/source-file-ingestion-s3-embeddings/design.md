## Context

Ver `proposal.md` para motivação e escopo.
O backend possui entidades `Source` e `SourceChunk` mapeadas com JPA e extensões `pgvector` prontas no PostgreSQL.
O container `notebooklm-floci` expõe emulador local do S3 na porta 4566 com bucket `notebooklm-sources`.
A comunicação com o modelo de IA é configurada via OpenRouter utilizando Spring AI.

## Goals / Non-Goals

**Goals:**
- Prover endpoint `POST /api/v1/notebooks/{notebookId}/sources/upload` com suporte a multipart `.md` e `.docx` retornando `202 Accepted`.
- Prover endpoint `GET /api/v1/notebooks/{notebookId}/sources/{sourceId}` para acompanhamento de status.
- Integrar com AWS SDK v2 (`S3Client`) apontando para endpoint customizado configurável via `application.yml`.
- Upload seguro do arquivo original no bucket S3.
- Parsing limpo do texto do arquivo: UTF-8 para `.md` e Apache POI para `.docx`.
- Chunking semântico via `TokenTextSplitter` do Spring AI.
- Geração de embeddings vetoriais via `OpenAiEmbeddingModel` do Spring AI.
- Persistência dos chunks em `source_chunks` com tipo `PGvector` (1536 dimensões) e atualização de status em `sources`.
- Blindagem anti-IDOR completa em todas as operações de leitura e gravação.

**Non-Goals:**
- Ingestão via Web URL (`POST .../sources/url`) (será abordada em outra especificação dedicada a web scraping com Jsoup).
- Exclusão física complexa ou reconciliação de jobs zumbis (fora do escopo inicial).

## Decisions

### 1. Cliente AWS S3 com AWS SDK v2
- **Decisão:** Adicionar dependência `software.amazon.awssdk:s3` no `pom.xml`.
  - Criar `@Configuration` `AwsS3Config` expondo bean `S3Client`.
  - Se `aws.s3.endpoint` estiver configurado, utilizar `.endpointOverride(URI.create(endpoint))` e `.forcePathStyle(true)` (essencial para emuladores S3 como Floci/LocalStack).
  - Configurar credenciais estáticas de desenvolvimento (`test`/`test`) ou default credentials provider.

### 2. Extração de Texto com Apache POI
- **Decisão:** Adicionar `org.apache.poi:poi-ooxml` no `pom.xml` para leitura de arquivos `.docx` através de `XWPFDocument` e `XWPFParagraph`.
- **Alternativa considerada:** Apache Tika completo. O Apache POI é mais leve, direto e específico para DOCX, evitando dependências transitivas massivas de OCR e outros formatos não suportados.

### 3. Modelo e Dimensões de Embeddings
- **Decisão:** Utilizar `OpenAiEmbeddingModel` provido pelo starter Spring AI apontando para o OpenRouter configurado no `application.yml`:
  `spring.ai.openai.embedding.options.model: ${LLM_EMBEDDING_MODEL:google/gemini-embedding-2}`
  com dimensões configuradas para 1536 (`spring.ai.openai.embedding.options.dimensions: 1536`), perfeitamente compatível com o schema `vector(1536)` da tabela `source_chunks` no PostgreSQL.

### 4. Padrão de Execução Assíncrona
- **Decisão:** Criar serviço `SourceIngestionProcessor` anotado com `@Async` (ou executado em executor de background) que recebe os bytes/arquivo, ID da fonte e executa a esteira sequencialmente:
  1. Status -> `PROCESSING`;
  2. PutObject S3;
  3. Extrai texto;
  4. Divide em tokens via `TokenTextSplitter`;
  5. Gera embeddings em lote ou por chunk;
  6. Grava `SourceChunk` no banco;
  7. Status -> `READY`.
  Em caso de erro inesperado, captura a exceção e atualiza a fonte para `status = FAILED` e `errorMessage = e.getMessage()`.

## Risks / Trade-offs

- [Timeout ou instabilidade na API externa de Embeddings] → A falha não trava a thread HTTP do cliente (pois o retorno 202 já ocorreu). O registro é marcado como `FAILED` e pode ser consultado via polling.
- [Arquivo DOCX malformado] → Capturado no parsing e marcado como `FAILED` com mensagem explicativa amigável.
