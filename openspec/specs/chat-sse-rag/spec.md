## Purpose
Prover interação conversacional com streaming em tempo real via Server-Sent Events (SSE), filtragem por fontes ativas na conversa (`conv_active_sources`), persistência de histórico de mensagens (`conversation_messages`) e integração com LLM compatível com contrato OpenAI.

## Requirements

### Requirement: Seleção Granular de Fontes Ativas na Conversa
O usuário deve poder definir quais fontes anexadas ao notebook estão ativas para responder a perguntas em uma conversa específica (`conversations`), persistindo-as em `conv_active_sources`.

#### Scenario: Pergunta com fontes ativas associadas
- **WHEN** O cliente envia `POST /api/v1/notebooks/{id}/conversations/{conversationId}/messages/stream` com `activeSourceIds: ["uuid-1", "uuid-2"]`
- **THEN** O sistema associa as fontes na tabela `conv_active_sources` e a busca semântica por similaridade de cossenos no `pgvector` restringe os chunks exclusivamente às fontes ativas com status `READY`.

#### Scenario: Pergunta sem fontes ativas definidas
- **WHEN** A conversa não possui fontes associadas em `conv_active_sources`
- **THEN** O sistema seleciona automaticamente como contexto do RAG todas as fontes do notebook com status `READY`.

### Requirement: Streaming em Tempo Real via SSE
A resposta gerada pelo LLM deve ser enviada token a token através do protocolo Server-Sent Events (`text/event-stream`).

#### Scenario: Recepção de tokens via SSE
- **WHEN** O cliente estabelece a conexão SSE com `Accept: text/event-stream`
- **THEN** O servidor transmite eventos do tipo `message` contendo fragmentos de texto gerados pelo modelo e finaliza com um evento `done` contendo o ID da mensagem gerada.

### Requirement: Persistência de Histórico de Conversa e Mensagens
O sistema deve persistir as mensagens em `conversation_messages` com papéis `user` e `assistant`, vinculadas a `conversations`, mantendo o contexto histórico.

#### Scenario: Continuidade do diálogo com histórico
- **WHEN** O usuário envia uma nova mensagem para uma conversa existente (`conversationId`)
- **THEN** O backend carrega as mensagens anteriores da conversa em ordem cronológica e as inclui na janela de contexto do LLM junto aos chunks recuperados pelo RAG.

### Requirement: Provedor de LLM Transparente e Invisível
O backend deve utilizar o contrato compatível com OpenAI (suportando OpenRouter, AWS Bedrock Converse ou OpenAI) configurado na infraestrutura do sistema, sem expor opções de modelo ou provedor na interface do usuário.

#### Scenario: Chamada ao LLM agnóstico
- **WHEN** O serviço de chat invoca o client de IA
- **THEN** A chamada é executada contra o provedor configurado no sistema, sendo totalmente transparente para o usuário final.
