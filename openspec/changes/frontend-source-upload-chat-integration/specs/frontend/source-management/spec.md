## Purpose

Gerenciamento e acompanhamento reativo de fontes documentais no frontend, incluindo upload de arquivos locais, feedback de processamento assíncrono via polling e seleção granular para o contexto de chat RAG.

## ADDED Requirements

### Requirement: Upload de Arquivos de Fontes com Validação e Feedback

O frontend SHALL permitir a seleção e upload de arquivos nos formatos `.md`, `.markdown` e `.docx` limitados a 25MB, enviando-os via requisição multipart com token JWT Bearer, exibindo feedback imediato de envio e tratando rejeições locais e do servidor.

#### Scenario: Upload de arquivo válido com sucesso
- **WHEN** o usuário seleciona um arquivo válido `.md` ou `.docx` menor ou igual a 25MB
- **THEN** o frontend dispara `POST /api/v1/notebooks/{notebookId}/sources/upload`, recebe `HTTP 202 Accepted` com `SourceUploadResponse`, adiciona a fonte à lista com status `PENDING` e inicia o monitoramento de status.

#### Scenario: Validação local de extensão não suportada
- **WHEN** o usuário tenta selecionar um arquivo de extensão não suportada (ex: `.pdf`, `.txt`, `.png`)
- **THEN** o frontend impede o envio, exibe mensagem de erro informativa e não realiza chamada HTTP.

#### Scenario: Validação local de tamanho excedido
- **WHEN** o usuário tenta enviar um arquivo com tamanho superior a 25MB
- **THEN** o frontend impede o envio e exibe alerta de que o tamanho excede o limite máximo permitido de 25MB.

### Requirement: Acompanhamento de Status de Fontes via Polling

O frontend SHALL consultar periodicamente (`polling` a cada 2 a 3 segundos) o status de cada fonte em estado transitório (`PENDING` ou `PROCESSING`) através de `GET /api/v1/notebooks/{notebookId}/sources/{sourceId}`, cessando o polling assim que o status alcançar um estado final (`READY` ou `FAILED`).

#### Scenario: Atualização de status para READY
- **WHEN** a consulta de status retorna status `READY`
- **THEN** o frontend atualiza o indicador visual da fonte para pronto, habilita o checkbox de seleção para o chat e marca a fonte como ativa por padrão.

#### Scenario: Atualização de status para FAILED
- **WHEN** a consulta de status retorna status `FAILED` com mensagem de erro
- **THEN** o frontend atualiza o indicador visual da fonte para erro, desabilita a seleção e exibe a mensagem de erro detalhada para o usuário.

### Requirement: Listagem e Seleção Granular de Fontes Ativas no Chat

A tela de Workspace SHALL carregar todas as fontes do notebook via `GET /api/v1/notebooks/{notebookId}/sources` ao inicializar, permitindo ao usuário marcar ou desmarcar fontes com status `READY` para serem enviadas como `activeSourceIds` no payload de streaming do chat.

#### Scenario: Carregamento inicial de fontes existentes
- **WHEN** o usuário abre a tela do Workspace (`/notebooks/:id`)
- **THEN** o frontend busca `GET /api/v1/notebooks/{notebookId}/sources`, preenche a lista na barra lateral e retoma o polling para quaisquer fontes que ainda estejam em `PENDING` ou `PROCESSING`.

#### Scenario: Envio de mensagem com fontes selecionadas
- **WHEN** o usuário envia uma mensagem no chat com fontes marcadas
- **THEN** a chamada de streaming SSE inclui os IDs das fontes ativas no campo `activeSourceIds` do corpo da requisição.
