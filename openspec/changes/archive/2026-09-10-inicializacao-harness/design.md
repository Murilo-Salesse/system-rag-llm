## Context
O sistema é um clone simplificado do NotebookLM que permite aos usuários carregar documentos e páginas web e interagir com um assistente de IA através de RAG (Retrieval-Augmented Generation). O sistema deve operar na nuvem AWS, com arquitetura 100% stateless, integrando API Gateway, Load Balancer, containers Spring Boot, Amazon S3 para arquivos originais e PostgreSQL com a extensão pgvector para dados relacionais e busca semântica.

## Goals / Non-Goals

**Goals:**
- Prover autenticação federada com Google e GitHub via AWS Cognito User Pool, validando tokens JWT de forma stateless no backend.
- Permitir ao usuário criar e gerenciar notebooks (Tela 2) e isolar os dados estritamente por usuário.
- Suportar ingestão não-bloqueante (Async Request-Reply: HTTP 202 Accepted) para arquivos Markdown (`.md`), Word (`.docx`) e Web URLs.
- Armazenar arquivos brutos no Amazon S3 e fragmentos de texto (chunks) indexados no PostgreSQL com pgvector (índice HNSW).
- Permitir seleção granular de quais fontes anexadas estão ativas para responder a uma pergunta no chat (Tela 3).
- Persistir histórico completo de conversas em sessões de chat.
- Transmitir respostas geradas pelo LLM em tempo real via Server-Sent Events (SSE).
- Tornar o provedor de LLM (AWS Bedrock, OpenRouter ou OpenAI) totalmente transparente e invisível para o usuário final.

**Non-Goals:**
- Implementação de autenticação com login e senha local/legado (apenas SSO federado via Cognito).
- Suporte a formatos além de Markdown, DOCX e Web URL (como PDFs escaneados ou imagens OCR).
- Sessões stateful baseadas em cookies ou sessões de servlet mantidas em memória.
- Interface para o usuário escolher modelos ou parâmetros de IA na tela.

## Decisions
- **ADR-001 (Cognito Stateless):** Adoção de Spring Security OAuth2 Resource Server validando JWT via JWKS do Cognito.
- **ADR-002 (Async Request-Reply):** Upload de arquivos e URLs retorna HTTP 202 Accepted e processa extração, chunking e embeddings de forma assíncrona.
- **ADR-003 (PostgreSQL + pgvector):** Unificação de dados relacionais e vetoriais em uma única base, eliminando a complexidade de bancos vetoriais dedicados separados.
- **ADR-004 (Contrato OpenAI para Provedores de LLM):** Compatibilidade com APIs estilo OpenAI permite alternar entre OpenRouter, Bedrock Converse e OpenAI apenas mudando variáveis de ambiente.
- **ADR-005 (Streaming com Server-Sent Events):** Uso de SSE (`text/event-stream`) por ser unidirecional, mais simples que WebSockets e totalmente compatível com proxies HTTP e API Gateway.
- **ADR-006 (Seleção Dinâmica de Sources):** Query vetorial filtra `notebook_id = :id AND source_id IN (:activeSourceIds)`.

## Risks / Trade-offs
- **Timeout no API Gateway:** Adoção do padrão Async Request-Reply mitiga o limite de 29s do API Gateway para arquivos grandes ou URLs lentas.
- **Dimensionalidade Vetorial:** Fixada em 1536 dimensões (`vector(1536)`), padrão de modelos populares como `text-embedding-3-small`. Mudanças de modelo de embedding requerem nova migration no banco.
