## Why

O NotebookLM necessita permitir a interação conversacional em tempo real (Painel Direito da Tela 3). Para proporcionar resposta rápida e experiência fluida ao usuário, o envio de mensagens deve transmitir os tokens da resposta via Server-Sent Events (SSE) à medida que são gerados pelo LLM através do OpenRouter, mantendo o histórico de mensagens multi-turn persistido no PostgreSQL e isolado por usuário contra vulnerabilidades IDOR.

## What Changes

- Implementação do endpoint de streaming em tempo real via Server-Sent Events: `POST /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages/stream` retornando `Flux<ServerSentEvent<Map<String, Object>>>`.
- Integração do `ChatClient` do Spring AI com OpenRouter configurando modelo econômico e de baixa latência (`openai/gpt-4o-mini` ou similar compatível com contrato OpenAI).
- Inclusão do histórico cronológico de mensagens anteriores da conversa (`conversation_messages`) no prompt multi-turn do modelo.
- Persistência imediata da mensagem enviada pelo usuário (`role = user`) e persistência da mensagem final gerada pelo modelo (`role = assistant`) com emissão de evento final `event: done`.
- Atualização e persistência de fontes ativas na tabela `conv_active_sources` caso `activeSourceIds` seja enviado no payload.
- Blindagem anti-IDOR garantindo que o `notebookId` pertença ao usuário autenticado e que a `conversationId` pertença ao `notebookId`, retornando `404 Not Found` em acessos não autorizados.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `chat-sse-rag`: Especifica o comportamento da rota `POST /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages/stream`, fluxo de eventos SSE (`message` e `done`), formato do payload de requisição e persistência do histórico em `conversation_messages` usando Spring AI sem RAG.

## Impact

- **Affected code:** Criação de `AiConfig.java`, DTO `StreamMessageRequest`, serviço de streaming `ChatStreamService` (ou método em `ConversationService`), atualização de `ConversationController` e testes unitários/integração.
- **APIs:** Novo endpoint `POST /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages/stream` com `Accept: text/event-stream`.
- **Dependencies:** Utiliza `spring-ai-starter-model-openai` já presente no `pom.xml`.
- **Statelessness & AWS costs:** 100% stateless via JWT do Cognito. Custo mínimo por uso do modelo mais acessível via OpenRouter.
