## Purpose
Fornecer autenticação stateless e Single Sign-On (SSO) com Google e GitHub via AWS Cognito User Pool para acesso seguro às APIs do NotebookLM.

## Requirements

### Requirement: Autenticação Federada Google e GitHub
O sistema deve integrar com AWS Cognito User Pool configurado com provedores de identidade federados (Google e GitHub) utilizando o fluxo OAuth2 Authorization Code com PKCE.

#### Scenario: Login bem-sucedido via Google ou GitHub
- **WHEN** O usuário clica em "Login Google" ou "Login GitHub" na Tela 1
- **THEN** O usuário é redirecionado para o Hosted UI do AWS Cognito, autentica-se no provedor selecionado e recebe os tokens JWT (ID Token e Access Token).

### Requirement: Validação Stateless de JWT
O backend Spring Boot deve atuar exclusivamente como OAuth2 Resource Server stateless, validando as assinaturas dos tokens JWT através das chaves públicas JWKS expostas pelo AWS Cognito.

#### Scenario: Requisição autenticada com Bearer Token válido
- **WHEN** O cliente envia uma requisição com header `Authorization: Bearer <valid_jwt>`
- **THEN** O Spring Security valida a assinatura e expiração do token via JWKS, extrai o identificador único do usuário (`sub`) e permite acesso aos endpoints protegidos sem criar sessão HTTP em memória.

#### Scenario: Requisição com token ausente ou expirado
- **WHEN** O cliente envia uma requisição sem token ou com token JWT inválido/expirado
- **THEN** O sistema retorna imediatamente `HTTP 401 Unauthorized`.
