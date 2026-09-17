## 1. Configuração do Spring AI e OpenRouter

- [x] 1.1 Atualizar `application.yml` adicionando a configuração do modelo padrão (`spring.ai.openai.chat.options.model: ${LLM_MODEL:openai/gpt-4o-mini}`) e criar a classe `AiConfig.java` expondo o bean `ChatClient`. Verificar inicialização do contexto do Spring Boot.

## 2. DTOs e Contratos

- [x] 2.1 Criar record imutável `StreamMessageRequest` em `dto` com anotações de validação (`@NotBlank content`, `List<UUID> activeSourceIds`). Configurar exclusão do pacote `dto` no JaCoCo/PITest se necessário e verificar compilação.

## 3. Implementação do Serviço de Streaming

- [x] 3.1 Implementar método de streaming de mensagens no serviço (`ConversationService` ou `ChatStreamService`) recebendo o usuário autenticado, `notebookId`, `conversationId` e `StreamMessageRequest`. O método deve:
  - Validar anti-IDOR (notebook pertencente ao usuário e conversa vinculada ao notebook);
  - Persistir a mensagem do usuário em `conversation_messages` (`role = user`);
  - Se `activeSourceIds` informado, atualizar fontes ativas em `conv_active_sources`;
  - Montar o prompt multi-turn com histórico cronológico de mensagens;
  - Invocar `chatClient.prompt().stream().content()`;
  - Mapear cada chunk para `ServerSentEvent` de tipo `message` com `{"token": chunk}`;
  - Ao completar a stream, persistir a resposta do assistente no banco (`role = assistant`) e emitir evento `done` com `{"messageId": id, "status": "COMPLETED"}`.
- [x] 3.2 Criar testes unitários para o serviço cobrindo fluxo com sucesso, histórico multi-turn, tratamento de erro e IDOR (404 Not Found).

## 4. Endpoint no Controller

- [x] 4.1 Adicionar endpoint `POST /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages/stream` em `ConversationController` com `produces = MediaType.TEXT_EVENT_STREAM_VALUE` e `Accept: text/event-stream`.
- [x] 4.2 Criar testes de controller (WebMvcTest) validando autenticação JWT, status de resposta SSE e validação de payload.

## 5. Verificação e Quality Gate

- [x] 5.1 Executar a suíte de testes com JUnit 5 + Mockito e verificar conformidade executando `quality-gate.sh` garantindo line coverage >= 80% e mutation kill rate >= 80%.
