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

### Requirement: Suporte a USER_PASSWORD_AUTH no App Client

O App Client do Cognito/Floci SHALL ter o fluxo `ALLOW_USER_PASSWORD_AUTH` habilitado (já habilitado pelo `init-cognito.sh` — confirmação formal deste requisito).

#### Scenario: App Client permite autenticação direta
- **WHEN** o frontend chama `InitiateAuth` com `AuthFlow=USER_PASSWORD_AUTH`
- **THEN** o Floci aceita a requisição e retorna `AuthenticationResult` com `IdToken`, `AccessToken` e `RefreshToken`

### Requirement: Token Armazenado é o IdToken

O frontend SHALL armazenar exclusivamente o `IdToken` (não o `AccessToken`) retornado pelo Cognito após qualquer fluxo de autenticação, pois é o token que contém as claims de usuário validadas pelo Spring Security via JWKS.

#### Scenario: Requisição autenticada usa IdToken
- **WHEN** o frontend envia uma requisição ao backend após login local ou OAuth
- **THEN** o header `Authorization: Bearer <token>` carrega o `IdToken` do Cognito, que o Spring Security valida via JWKS do Floci

