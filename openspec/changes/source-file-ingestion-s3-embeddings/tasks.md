## 1. Dependências e Configurações

- [x] 1.1 Adicionar dependências `software.amazon.awssdk:s3` e `org.apache.poi:poi-ooxml` no `pom.xml`. Atualizar `application.yml` com configuração de embeddings (`spring.ai.openai.embedding.options.model: ${LLM_EMBEDDING_MODEL:google/gemini-embedding-2}` e `dimensions: 1536`). Verificar compilação com `mvn test-compile`.
- [x] 1.2 Criar `@Configuration` `AwsS3Config` para instanciar o bean `S3Client` com suporte a endpoint override para o Floci/LocalStack. Criar testes unitários para a configuração.

## 2. DTOs e Repositórios

- [x] 2.1 Criar DTO records `SourceUploadResponse` e `SourceStatusResponse` em `dto` com validações. Criar `SourceChunkRepository` com busca e contagem de chunks por `sourceId`. Verificar compilação.

## 3. Serviços de Parsing e Armazenamento S3

- [x] 3.1 Implementar `S3StorageService` para upload de arquivos brutos no bucket configurado (`notebooklm-sources`). Escrever testes unitários com JUnit 5 + Mockito.
- [x] 3.2 Implementar `DocumentParserService` para extração de texto suportando Markdown (`.md` UTF-8) e DOCX (`.docx` via Apache POI). Escrever testes unitários com JUnit 5 cobrindo parsing de ambos os formatos e rejeição de formatos inválidos.

## 4. Pipeline de Ingestão e Geração de Embeddings

- [x] 4.1 Implementar `SourceIngestionProcessor` com método assíncrono para orquestrar o pipeline: upload S3, atualização de status para `PROCESSING`, parsing do texto, divisão em chunks (`TokenTextSplitter`), geração de embeddings vetoriais de 1536 dimensões via `OpenAiEmbeddingModel`, persistência em `source_chunks` com tipo `PGvector` e finalização com status `READY` (ou `FAILED` em erro). Escrever testes unitários para o processador.
- [x] 4.2 Implementar `SourceService` com criação de fontes em status `PENDING`, validação anti-IDOR do notebook e consulta de status (`getSourceStatus`). Escrever testes unitários para o serviço.

## 5. Endpoints REST e Controller

- [x] 5.1 Implementar `SourceController` com endpoints `POST /api/v1/notebooks/{notebookId}/sources/upload` (`multipart/form-data`) retornando `202 Accepted` e `GET /api/v1/notebooks/{notebookId}/sources/{sourceId}` retornando `200 OK`.
- [x] 5.2 Implementar testes de controller (WebMvcTest) validando autenticação JWT, validação de tipos de arquivo (.md, .docx), rejeição de formatos inválidos (400) e anti-IDOR (404).

## 6. Verificação e Quality Gate

- [x] 6.1 Executar a suíte completa de testes unitários e verificar conformidade executando `quality-gate.sh` garantindo line coverage >= 80% e mutation kill rate >= 80%.
