## Purpose
Permitir a ingestão assíncrona (Async Request-Reply) de fontes documentais nos formatos Markdown, DOCX e Web URL, persistindo arquivos brutos no Amazon S3 e gerando chunks indexados no PostgreSQL com pgvector.

## Requirements

### Requirement: Formatos de Fontes Permitidos
O sistema deve aceitar estritamente três formatos de fontes: arquivos Markdown (`.md`), documentos Word (`.docx`) e URLs da Web públicas. Qualquer outro formato deve ser rejeitado.

#### Scenario: Upload de arquivo não suportado
- **WHEN** O usuário tenta enviar um arquivo com extensão `.pdf` ou `.exe`
- **THEN** O sistema rejeita o upload retornando `HTTP 400 Bad Request` com mensagem de formatos permitidos (`MARKDOWN`, `DOCX`, `WEB_URL`).

### Requirement: Padrão Async Request-Reply na Ingestão
A ingestão de fontes deve responder imediatamente sem bloquear a conexão HTTP, delegando o processamento pesado (parsing, chunking e embeddings) para execução assíncrona em background.

#### Scenario: Início de ingestão via upload de arquivo
- **WHEN** O usuário envia `POST /api/v1/notebooks/{id}/sources/upload` com um arquivo `.md` ou `.docx`
- **THEN** O backend armazena o arquivo no bucket S3, insere o registro da fonte no PostgreSQL com status `PENDING`, dispara o worker assíncrono e responde imediatamente com `HTTP 202 Accepted` contendo `sourceId`, status `PENDING` e header `Location`.

#### Scenario: Início de ingestão via Web URL
- **WHEN** O usuário envia `POST /api/v1/notebooks/{id}/sources/url` com uma URL válida
- **THEN** O backend cria a fonte com status `PENDING`, dispara o worker assíncrono para efetuar o scraping e responde imediatamente com `HTTP 202 Accepted`.

### Requirement: Pipeline Assíncrono de Chunking e Embedding
O worker em background deve extrair o texto limpo, particionar o conteúdo em chunks com sobreposição e gerar embeddings vetoriais via Spring AI persistindo-os no PostgreSQL + pgvector.

#### Scenario: Processamento concluído com sucesso
- **WHEN** O worker finaliza o parsing (Jsoup para URL, Apache POI para DOCX, parser MD para Markdown), executa `TokenTextSplitter` e gera embeddings vetoriais de 1536 dimensões
- **THEN** Os chunks são persistidos na tabela `document_chunks` e a fonte tem seu status atualizado para `READY` com o número total de chunks.

#### Scenario: Falha durante o processamento
- **WHEN** Ocorre falha na extração de texto (ex: URL inacessível com HTTP 404 ou DOCX corrompido)
- **THEN** O status da fonte é atualizado para `FAILED` com a descrição do erro no campo `error_message`.

### Requirement: Consulta de Status da Fonte (Polling)
O cliente deve ser capaz de consultar o status de prontidão da fonte para atualizar a interface (Tela 3).

#### Scenario: Polling de fonte em processamento
- **WHEN** O frontend dispara `GET /api/v1/notebooks/{id}/sources/{sourceId}`
- **THEN** O sistema retorna `HTTP 200 OK` com o status atual (`PENDING`, `PROCESSING`, `READY` ou `FAILED`) e métricas.
