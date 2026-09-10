# Contratos de API e Funcionalidades (API.md)

Este documento especifica todos os endpoints REST HTTP e o canal de streaming Server-Sent Events (SSE) da plataforma **NotebookLM Simplificado**, incluindo URLs, métodos, cabeçalhos de segurança, payloads JSON de requisição e resposta, e códigos de status HTTP.

---

## 1. Padrões Globais da API

### 1.1. Base URLs
- **Ambiente de Nuvem (Produção via API Gateway):** `https://api.notebooklm.internal/api/v1`
- **Ambiente Local (Desenvolvimento):** `http://localhost:8080/api/v1`

### 1.2. Autenticação Stateless
Todas as requisições para `/api/v1/**` exigem o cabeçalho HTTP com o token JWT emitido pelo AWS Cognito (após o login SSO com Google ou GitHub na Tela 1):
```http
Authorization: Bearer <cognito_jwt_token>
```
O backend valida a assinatura digital do token diretamente através do endpoint público JWKS do Cognito. Nenhuma sessão em memória ou cookie de sessão é mantido no servidor.

### 1.3. Padrão de Erro Global (RFC 7807)
Em caso de falha, a resposta segue a estrutura:
```json
{
  "timestamp": "2026-09-08T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Formato de arquivo não suportado. Permitidos apenas: .md, .docx",
  "path": "/api/v1/notebooks/7c9e6679-7425-40de-944b-e07fc1f90ae7/sources/upload"
}
```

---

## 2. Módulo de Notebooks (Tela 2)

### 2.1. Criar Notebook
Cria um novo notebook para o usuário autenticado.

- **Método & Rota:** `POST /api/v1/notebooks`
- **Headers:**
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **Request Body:**
```json
{
  "name": "Pesquisa de Mercado IA",
  "description": "Artigos e análises sobre agentes autônomos"
}
```
- **Response `201 Created`:**
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "ownerId": "d98f7e2a-1122-3344-5566-778899aabbcc",
  "name": "Pesquisa de Mercado IA",
  "description": "Artigos e análises sobre agentes autônomos",
  "createdAt": "2026-09-08T12:00:00Z",
  "updatedAt": "2026-09-08T12:00:00Z"
}
```
- **Response `401 Unauthorized`:** Token ausente ou expirado.

---

### 2.2. Listar Notebooks
Retorna todos os notebooks pertencentes ao usuário autenticado (alimenta os cards da Tela 2).

- **Método & Rota:** `GET /api/v1/notebooks`
- **Headers:** `Authorization: Bearer <token>`
- **Response `200 OK`:**
```json
[
  {
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "name": "Notebook 1",
    "description": "Pesquisa inicial sobre RAG",
    "sourceCount": 3,
    "createdAt": "2026-09-08T12:00:00Z",
    "updatedAt": "2026-09-08T12:00:00Z"
  },
  {
    "id": "8d0f7780-8536-41ef-055c-f18fd2f01bf8",
    "name": "Notebook 2",
    "description": "Documentos financeiros",
    "sourceCount": 1,
    "createdAt": "2026-09-08T12:15:00Z",
    "updatedAt": "2026-09-08T12:15:00Z"
  }
]
```

---

### 2.3. Obter Detalhes do Notebook
- **Método & Rota:** `GET /api/v1/notebooks/{notebookId}`
- **Headers:** `Authorization: Bearer <token>`
- **Response `200 OK`:**
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "name": "Notebook 1",
  "description": "Pesquisa inicial sobre RAG",
  "createdAt": "2026-09-08T12:00:00Z",
  "updatedAt": "2026-09-08T12:00:00Z"
}
```
- **Response `404 Not Found`:** Notebook inexistente ou não pertencente ao usuário autenticado.

---

### 2.4. Excluir Notebook
Remove o notebook e realiza a exclusão em cascata de todas as fontes, fragmentos no pgvector e conversas subordinadas.

- **Método & Rota:** `DELETE /api/v1/notebooks/{notebookId}`
- **Headers:** `Authorization: Bearer <token>`
- **Response `204 No Content`**
- **Response `404 Not Found`**

---

## 3. Módulo de Ingestão de Fontes (Tela 3 - Painel Esquerdo)

### 3.1. Upload Assíncrono de Arquivo (Markdown / DOCX)
Inicia a ingestão de um documento local nos formatos estritamente permitidos: **Markdown** (`.md`) ou Word **DOCX** (`.docx`).

- **Método & Rota:** `POST /api/v1/notebooks/{notebookId}/sources/upload`
- **Headers:**
  - `Content-Type: multipart/form-data`
  - `Authorization: Bearer <token>`
- **Form Data:**
  - `file`: Arquivo binário (`.md` ou `.docx`) com limite máximo de 25MB.
  - `name` (opcional): Nome customizado de exibição da fonte.
- **Response `202 Accepted` (Async Request-Reply):**
  - **Header retornado:** `Location: /api/v1/notebooks/7c9e6679-7425-40de-944b-e07fc1f90ae7/sources/a1b2c3d4-e5f6-7890-abcd-ef1234567890`
```json
{
  "sourceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "notebookId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "name": "relatorio-financeiro.docx",
  "type": "DOCX",
  "status": "PENDING",
  "message": "Upload aceito com sucesso. Processamento assíncrono em andamento.",
  "statusUrl": "/api/v1/notebooks/7c9e6679-7425-40de-944b-e07fc1f90ae7/sources/a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```
- **Response `400 Bad Request`:** Extensão não suportada (ex: `.pdf`) ou arquivo corrompido/vazio.

---

### 3.2. Ingestão Assíncrona de Web URL
Inicia a extração assíncrona de uma página web pública.

- **Método & Rota:** `POST /api/v1/notebooks/{notebookId}/sources/url`
- **Headers:**
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **Request Body:**
```json
{
  "url": "https://aws.amazon.com/bedrock/",
  "name": "AWS Bedrock Overview"
}
```
- **Response `202 Accepted`:**
```json
{
  "sourceId": "f9e8d7c6-b5a4-3210-fedc-ba9876543210",
  "notebookId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "name": "AWS Bedrock Overview",
  "type": "WEB_URL",
  "status": "PENDING",
  "message": "Extração web enfileirada. Processamento assíncrono em andamento.",
  "statusUrl": "/api/v1/notebooks/7c9e6679-7425-40de-944b-e07fc1f90ae7/sources/f9e8d7c6-b5a4-3210-fedc-ba9876543210"
}
```
- **Response `400 Bad Request`:** URL inválida ou inacessível.

---

### 3.3. Listar Fontes do Notebook
Lista todas as fontes cadastradas no notebook com os respectivos status de prontidão (usado para montar a lista de checkboxes na Tela 3).

- **Método & Rota:** `GET /api/v1/notebooks/{notebookId}/sources`
- **Headers:** `Authorization: Bearer <token>`
- **Response `200 OK`:**
```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "document.docx",
    "type": "DOCX",
    "status": "READY",
    "createdAt": "2026-09-08T12:05:00Z"
  },
  {
    "id": "f9e8d7c6-b5a4-3210-fedc-ba9876543210",
    "name": "AWS Bedrock Overview",
    "type": "WEB_URL",
    "status": "PROCESSING",
    "createdAt": "2026-09-08T12:06:00Z"
  }
]
```

---

### 3.4. Consultar Status da Fonte (Polling)
Endpoint utilizado pelo frontend para acompanhar a transição de status (`PENDING` $\rightarrow$ `PROCESSING` $\rightarrow$ `READY` ou `FAILED`).

- **Método & Rota:** `GET /api/v1/notebooks/{notebookId}/sources/{sourceId}`
- **Headers:** `Authorization: Bearer <token>`
- **Response `200 OK`:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "notebookId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "name": "document.docx",
  "type": "DOCX",
  "status": "READY",
  "errorMessage": null,
  "createdAt": "2026-09-08T12:05:00Z"
}
```

---

### 3.5. Excluir Fonte
Remove o arquivo do S3, a fonte do banco e todos os fragmentos vetoriais associados em `source_chunks`.

- **Método & Rota:** `DELETE /api/v1/notebooks/{notebookId}/sources/{sourceId}`
- **Headers:** `Authorization: Bearer <token>`
- **Response `204 No Content`**

---

## 4. Módulo de Conversas e Streaming SSE (Tela 3 - Painel Direito)

### 4.1. Criar Conversa
Cria uma nova conversa vinculada a um notebook.

- **Método & Rota:** `POST /api/v1/notebooks/{notebookId}/conversations`
- **Headers:**
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **Response `201 Created`:**
```json
{
  "id": "11223344-5566-7788-99aa-bbccddeeff00",
  "notebookId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "createdAt": "2026-09-08T12:10:00Z"
}
```

---

### 4.2. Listar Conversas do Notebook
Retorna todas as conversas existentes no notebook.

- **Método & Rota:** `GET /api/v1/notebooks/{notebookId}/conversations`
- **Headers:** `Authorization: Bearer <token>`
- **Response `200 OK`:**
```json
[
  {
    "id": "11223344-5566-7788-99aa-bbccddeeff00",
    "notebookId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "createdAt": "2026-09-08T12:10:00Z"
  }
]
```

---

### 4.3. Atualizar Fontes Ativas da Conversa (`conv_active_sources`)
Define ou atualiza quais fontes estão ativas para delimitar a busca no RAG desta conversa.

- **Método & Rota:** `PUT /api/v1/notebooks/{notebookId}/conversations/{conversationId}/active-sources`
- **Headers:**
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **Request Body:**
```json
{
  "sourceIds": [
    "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  ]
}
```
- **Response `200 OK`:**
```json
{
  "conversationId": "11223344-5566-7788-99aa-bbccddeeff00",
  "activeSourceIds": [
    "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  ]
}
```

---

### 4.4. Obter Histórico de Mensagens da Conversa
Retorna o histórico cronológico de turnos de mensagens da conversa.

- **Método & Rota:** `GET /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages`
- **Headers:** `Authorization: Bearer <token>`
- **Response `200 OK`:**
```json
[
  {
    "id": "c1a2b3c4-0001-1111-2222-333344445555",
    "conversationId": "11223344-5566-7788-99aa-bbccddeeff00",
    "role": "user",
    "content": "Ola chat, etc",
    "createdAt": "2026-09-08T12:10:05Z"
  },
  {
    "id": "c1a2b3c4-0002-1111-2222-333344445555",
    "conversationId": "11223344-5566-7788-99aa-bbccddeeff00",
    "role": "assistant",
    "content": "Olá! Com base nas fontes selecionadas, aqui está o resumo...",
    "createdAt": "2026-09-08T12:10:10Z"
  }
]
```

---

### 4.5. Streaming de Chat via Server-Sent Events (SSE)
Envia uma nova mensagem do usuário para a conversa, atualiza opcionalmente as fontes ativas e transmite os tokens da resposta em tempo real via fluxo SSE.

- **Método & Rota:** `POST /api/v1/notebooks/{notebookId}/conversations/{conversationId}/messages/stream`
- **Headers:**
  - `Content-Type: application/json`
  - `Accept: text/event-stream`
  - `Authorization: Bearer <token>`
- **Request Body:**
```json
{
  "content": "Qual o principal resultado apresentado na seção 2?",
  "activeSourceIds": [
    "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  ]
}
```
*Nota: Se `activeSourceIds` for omitido, o backend consulta a tabela `conv_active_sources` da conversa. Se esta estiver vazia, ativa todas as fontes do notebook com status `READY`.*

- **Response `200 OK` (`Content-Type: text/event-stream;charset=UTF-8`):**
```text
event: message
data: {"token": "De"}

event: message
data: {"token": " acordo"}

event: message
data: {"token": " com"}

event: message
data: {"token": " a"}

event: message
data: {"token": " seção"}

event: message
data: {"token": " 2..."}

event: done
data: {"messageId": "c1a2b3c4-0003-1111-2222-333344445555", "status": "COMPLETED"}
```
