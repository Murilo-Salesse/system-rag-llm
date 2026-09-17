## Purpose

Delta para o fluxo de autenticação `auth-cognito`: adiciona suporte a `USER_PASSWORD_AUTH` e `SignUp` via Cognito como extensão de desenvolvimento local, mantendo o comportamento de produção inalterado.

## ADDED Requirements

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
