## MODIFIED Requirements

### Requirement: Padrão Async Request-Reply na Ingestão
A ingestão de fontes SHALL responder imediatamente sem bloquear a conexão HTTP, delegando o processamento pesado (parsing, chunking e embeddings) para execução assíncrona em background.

#### Scenario: Início de ingestão via upload de arquivo
- **WHEN** O usuário autenticado envia `POST /api/v1/notebooks/{notebookId}/sources/upload` com um arquivo `.md` ou `.docx` (<= 25MB) para um notebook de sua propriedade
- **THEN** O backend armazena o arquivo no bucket S3, insere o registro da fonte no PostgreSQL com status `PENDING`, dispara o worker assíncrono e responde imediatamente com `HTTP 202 Accepted` contendo `sourceId`, status `PENDING` e header `Location`.

#### Scenario: Início de ingestão via Web URL
- **WHEN** O usuário envia `POST /api/v1/notebooks/{id}/sources/url` com uma URL válida
- **THEN** O backend cria a fonte com status `PENDING`, dispara o worker assíncrono para efetuar o scraping e responde imediatamente com `HTTP 202 Accepted`.

#### Scenario: Tentativa de upload em notebook de outro usuário
- **WHEN** O usuário tenta fazer upload de fonte em um notebook que não lhe pertence
- **THEN** O sistema rejeita com `HTTP 404 Not Found`.

#### Scenario: Upload de arquivo não suportado
- **WHEN** O usuário envia um arquivo com extensão não permitida (diferente de `.md` e `.docx`) ou vazio
- **THEN** O sistema rejeita imediatamente com `HTTP 400 Bad Request`.

### Requirement: Pipeline Assíncrono de Chunking e Embedding
O worker em background SHALL salvar o arquivo no bucket S3, atualizar o status para `PROCESSING`, extrair o texto limpo, particionar o conteúdo em chunks com `TokenTextSplitter`, gerar embeddings vetoriais de 1536 dimensões via OpenRouter e persistir os registros na tabela `source_chunks`.

#### Scenario: Processamento concluído com sucesso
- **WHEN** O worker finaliza o parsing (Jsoup para URL, Apache POI para DOCX, parser MD para Markdown), executa `TokenTextSplitter` e gera embeddings vetoriais de 1536 dimensões
- **THEN** Os chunks são persistidos na tabela `source_chunks` e a fonte tem seu status atualizado para `READY` com o número total de chunks.

#### Scenario: Falha durante o processamento
- **WHEN** Ocorre falha na extração de texto (ex: URL inacessível com HTTP 404 ou DOCX corrompido)
- **THEN** O status da fonte é atualizado para `FAILED` com a descrição do erro no campo `error_message`.
