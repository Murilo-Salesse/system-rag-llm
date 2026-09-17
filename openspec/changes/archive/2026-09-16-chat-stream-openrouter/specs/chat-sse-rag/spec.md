## MODIFIED Requirements

### Requirement: Streaming em Tempo Real via SSE
A resposta gerada pelo LLM SHALL ser enviada token a token através do protocolo Server-Sent Events (`text/event-stream`) consumindo o modelo configurado via Spring AI.

#### Scenario: Recepção de tokens via SSE
- **WHEN** O cliente envia requisição `POST /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages/stream` com `Accept: text/event-stream`
- **THEN** O servidor transmite eventos do tipo `message` contendo fragmentos de texto gerados pelo modelo no formato `data: {"token": "texto"}` e finaliza com um evento `done` contendo `data: {"messageId": "uuid", "status": "COMPLETED"}`.

#### Scenario: Acesso a conversa ou notebook de outro usuário
- **WHEN** O usuário tenta abrir a stream de mensagens em uma conversa ou notebook que não lhe pertence
- **THEN** O servidor rejeita a requisição imediatamente com `HTTP 404 Not Found`.

### Requirement: Persistência de Histórico de Conversa e Mensagens
O sistema SHALL persistir as mensagens em `conversation_messages` com papéis `user` e `assistant`, vinculadas a `conversations`, mantendo o contexto histórico.

#### Scenario: Continuidade do diálogo com histórico
- **WHEN** O usuário envia uma nova mensagem para uma conversa existente (`conversationId`)
- **THEN** O backend persiste a mensagem do usuário, carrega as mensagens anteriores da conversa em ordem cronológica, inclui o histórico na janela de contexto do LLM e, ao término do streaming, persiste a resposta do assistente no banco.
