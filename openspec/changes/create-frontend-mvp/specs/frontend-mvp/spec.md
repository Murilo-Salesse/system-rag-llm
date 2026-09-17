## Purpose

Interface de usuário web reativa e minimalista implementando as 3 telas funcionais (Login, Dashboard de Notebooks e Workspace com Fontes e Chat SSE).

## ADDED Requirements

### Requirement: Autenticação e Roteamento Protegido (Tela 1)
O frontend DEVE apresentar uma tela de login centralizada com os botões "Login Google" e "Login GitHub", gerenciar o Bearer Token JWT no estado da aplicação e proteger as rotas internas, redirecionando usuários não autenticados para a Tela 1.

#### Scenario: Exibição da Tela de Login
- **WHEN** O usuário acessa a raiz ou rota não autenticada sem token armazenado
- **THEN** A aplicação exibe a Tela 1 centralizada com o título "NotebookLM" e os botões "Login Google" e "Login GitHub".

#### Scenario: Roteamento protegido com token presente
- **WHEN** O usuário possui um Bearer Token válido configurado
- **THEN** A aplicação permite acesso ao Dashboard de Notebooks e anexa o cabeçalho `Authorization: Bearer <token>` em todas as requisições HTTP e SSE.

### Requirement: Dashboard de Notebooks (Tela 2)
O frontend DEVE listar todos os notebooks pertencentes ao usuário em formato de cards simples, permitir a criação de novos notebooks e viabilizar a navegação direta para o workspace de cada notebook.

#### Scenario: Listagem de Notebooks
- **WHEN** O usuário acessa o Dashboard (Tela 2)
- **THEN** A aplicação consome `GET /api/v1/notebooks` e renderiza cada notebook em um card contendo nome, descrição e o botão "Abrir".

#### Scenario: Criação de Notebook
- **WHEN** O usuário clica no botão "Criar", preenche o nome e descrição e submete o formulário
- **THEN** A aplicação envia `POST /api/v1/notebooks`, adiciona o novo card na lista e fecha o formulário de criação.

#### Scenario: Abertura do Notebook
- **WHEN** O usuário clica no botão "Abrir" de um card de notebook
- **THEN** A aplicação navega para a rota `/notebooks/:id` abrindo a Tela 3.

### Requirement: Workspace com Fontes e Chat SSE (Tela 3)
O frontend DEVE fornecer uma visão dividida em dois painéis (Split View): painel esquerdo para gestão de fontes com seleção granular, e painel direito para histórico de conversa e chat com streaming Server-Sent Events (SSE).

#### Scenario: Gestão e Seleção Granular de Fontes
- **WHEN** O usuário visualiza o painel esquerdo do workspace
- **THEN** A aplicação lista as fontes com seus status (`PENDING`, `PROCESSING`, `READY`) e fornece checkboxes para marcar quais fontes participam da busca semântica, sincronizando via `PUT .../active-sources`.

#### Scenario: Ingestão de Arquivo e Web URL
- **WHEN** O usuário faz upload de um arquivo (`.md` ou `.docx`) ou submete uma Web URL
- **THEN** A aplicação envia a requisição correspondente com status `HTTP 202 Accepted`, exibe a nova fonte com status pendente e realiza polling até a prontidão (`READY`).

#### Scenario: Streaming de Chat em Tempo Real via SSE
- **WHEN** O usuário digita uma mensagem no prompt inferior e clica em enviar `[ > ]`
- **THEN** A aplicação exibe a mensagem do usuário no histórico, conecta-se ao endpoint `POST .../messages/stream`, renderiza os tokens textuais em tempo real na tela e finaliza a mensagem no evento `done`.
