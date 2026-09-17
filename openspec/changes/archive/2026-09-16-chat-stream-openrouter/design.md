## Context

Ver `proposal.md` para motivação e escopo.
O backend utiliza Java 21, Spring Boot 3 e Spring AI com o starter `spring-ai-starter-model-openai`.
A integração atual conecta-se ao endpoint compatível com OpenAI no OpenRouter (`https://openrouter.ai/api/v1`).
As tabelas `conversations`, `conv_active_sources` e `conversation_messages` já existem no PostgreSQL e possuem entidades JPA mapeadas.

## Goals / Non-Goals

**Goals:**
- Prover endpoint `POST /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages/stream` com streaming reativo SSE (`Flux<ServerSentEvent<Map<String, Object>>>`).
- Integrar com OpenRouter via `ChatClient` fluent API do Spring AI.
- Utilizar modelo simples e econômico (default: `openai/gpt-4o-mini` configurável via `LLM_MODEL`).
- Persistir turnos da conversa (`role = user` antes da chamada, `role = assistant` ao finalizar a stream).
- Manter histórico multi-turn na requisição para o LLM.
- Proteger contra IDOR (validar propriedade do notebook e pertencimento da conversa).

**Non-Goals:**
- Busca semântica de RAG com `pgvector` nesta fase (será adicionada na etapa de ingestão/RAG).
- Function calling ou execução de tools pelo LLM.

## Decisions

### 1. Spring AI `ChatClient` e Configuração de Bean
- **Decisão:** Criar classe `@Configuration` `AiConfig` expondo um bean `ChatClient` configurado a partir de `ChatClient.Builder`.
- **Alternativa considerada:** Instanciar diretamente `OpenAiChatModel` manualmente. A fluent API `ChatClient` é a convenção recomendada no Spring AI 1.0+ e desacopla detalhes de baixo nível.

### 2. Streaming via `Flux<ServerSentEvent<Map<String, Object>>>`
- **Decisão:** O controller retornará um `Flux<ServerSentEvent<Map<String, Object>>>` com `produces = MediaType.TEXT_EVENT_STREAM_VALUE`.
  - Eventos de token intermediários: `event("message").data(Map.of("token", chunk))`
  - Evento final de conclusão: `event("done").data(Map.of("messageId", assistantMsgId, "status", "COMPLETED"))`
- **Alternativa considerada:** `SseEmitter`. O `Flux` reativo integra nativamente com `chatClient.prompt().stream().content()` de forma mais concisa e funcional.

### 3. Persistência de Turnos e Acúmulo de Resposta
- **Decisão:**
  1. A mensagem do usuário é persistida em `conversation_messages` com `role = MessageRole.USER`.
  2. As mensagens anteriores da conversa são recuperadas (`findByConversationIdOrderByCreatedAtAsc`) e passadas como mensagens do prompt (`SystemMessage` inicial de instruções simples + histórico `UserMessage`/`AssistantMessage`).
  3. À medida que os chunks fluem, um acumulador (`StringBuilder`) armazena os fragmentos.
  4. No operador reativo de finalização (`doOnComplete` / `concatWith`), a mensagem final do assistente é persistida via transação dedicada e o evento `done` é emitido.
- **Alternativa considerada:** Salvar a resposta no banco apenas de forma síncrona antes do envio. Inviável porque bloquearia o streaming SSE inicial.

### 4. Modelo Econômico no `application.yml`
- **Decisão:** Adicionar propriedade configurável `spring.ai.openai.chat.options.model: ${LLM_MODEL:openai/gpt-4o-mini}`.

## Risks / Trade-offs

- [Desconexão prematura do cliente SSE] → Se o cliente fechar a aba no meio da stream, o operador `doFinally` ou manipulador de cancelamento assegura que tokens parciais não causem inconsistência.
- [Latência da primeira resposta do OpenRouter] → Mitigada pelo streaming imediato do primeiro token assim que emitido pelo modelo.
