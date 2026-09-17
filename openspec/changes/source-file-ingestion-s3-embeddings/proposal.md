## Why

Para que o NotebookLM Simplificado possa executar o Retrieval-Augmented Generation (RAG), é essencial permitir que os usuários façam upload de seus documentos de estudo (.md e .docx). O processamento de parsing, chunking e geração de embeddings vetoriais consome tempo e recursos, exigindo uma arquitetura assíncrona orientada a Async Request-Reply (HTTP 202 Accepted) onde os arquivos brutos são guardados no S3, o status é rastreado no PostgreSQL e os vetores são indexados no pgvector.

## What Changes

- Adição da dependência AWS SDK v2 S3 (`software.amazon.awssdk:s3`) e leitor DOCX (`org.apache.poi:poi-ooxml`).
- Configuração de cliente S3 (`S3Client`) apontando para o Floci em desenvolvimento local ou AWS real.
- Criação de DTOs para resposta de ingestão (`SourceUploadResponse`) e consulta de status (`SourceStatusResponse`).
- Endpoint `POST /api/v1/notebooks/{notebookId}/sources/upload` (`multipart/form-data`) aceitando `.md` e `.docx` (até 25MB), validando anti-IDOR e retornando imediatamente `HTTP 202 Accepted` com `statusUrl` e `Location`.
- Endpoint `GET /api/v1/notebooks/{notebookId}/sources/{sourceId}` para polling do status (`PENDING` -> `PROCESSING` -> `READY` ou `FAILED`).
- Pipeline assíncrono (`@Async`):
  1. Upload do arquivo bruto no S3 (`notebooks/{notebookId}/sources/{sourceId}/{fileName}`);
  2. Extração de texto (leitura direta para Markdown, Apache POI para DOCX);
  3. Chunking com `TokenTextSplitter` do Spring AI;
  4. Geração de embeddings vetoriais via `OpenAiEmbeddingModel` (OpenRouter);
  5. Gravação dos chunks e vetores na tabela `source_chunks` vinculados à `sources`.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `source-ingestion`: Detalha os cenários de upload multipart para arquivos Markdown e DOCX, persistência no S3 (Floci), chunking com TokenTextSplitter, geração de embeddings com OpenRouter e gravação em `source_chunks`.

## Impact

- **Affected code:** Adição de `AwsS3Config`, `S3StorageService`, `DocumentParserService`, `SourceIngestionService`, `SourceController` e repositório `SourceChunkRepository`.
- **APIs:** Novos endpoints `POST /api/v1/notebooks/{notebookId}/sources/upload` e `GET /api/v1/notebooks/{notebookId}/sources/{sourceId}`.
- **Dependencies:** `software.amazon.awssdk:s3`, `org.apache.poi:poi-ooxml`.
- **Statelessness & AWS Costs:** 100% stateless via Cognito JWT. Armazenamento de arquivo bruto no S3 e persistência de vetores no PostgreSQL local (Floci/pgvector sem custo adicional em dev).
