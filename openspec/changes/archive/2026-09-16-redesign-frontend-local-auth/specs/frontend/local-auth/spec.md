## Purpose

Permite que desenvolvedores autentiquem e se registrem diretamente no Cognito/Floci local usando username e senha, sem depender de provedores OAuth externos ou manipulação manual do `localStorage`.

## ADDED Requirements

### Requirement: Login Local com Username e Senha

Quando a variável de ambiente `VITE_COGNITO_CLIENT_ID` estiver definida, a `LoginPage` SHALL exibir um formulário adicional de login local com campos de username e senha que autentica o usuário diretamente no Floci via `USER_PASSWORD_AUTH`, armazenando o `IdToken` retornado no `localStorage`.

#### Scenario: Login bem-sucedido com credenciais locais

- **WHEN** o usuário preenche username e senha válidos e clica em "Entrar"
- **THEN** o frontend chama `InitiateAuth` no Floci (`http://localhost:4566`), recebe o `IdToken`, armazena em `localStorage['auth_token']` e redireciona para o Dashboard

#### Scenario: Falha de autenticação com credenciais inválidas

- **WHEN** o usuário preenche credenciais incorretas e clica em "Entrar"
- **THEN** o frontend exibe mensagem de erro legível abaixo do formulário e não redireciona

#### Scenario: Seção local oculta em produção

- **WHEN** a variável `VITE_COGNITO_CLIENT_ID` não está definida no ambiente de build
- **THEN** a seção de login local não é renderizada — apenas os botões de OAuth social são exibidos

### Requirement: Cadastro Local via SignUp no Cognito

Quando a variável `VITE_COGNITO_CLIENT_ID` estiver definida, o frontend SHALL oferecer um fluxo de cadastro onde o usuário informa username, e-mail e senha para criar uma nova conta no Floci via API `SignUp` do Cognito, com confirmação automática (sem código de verificação) para facilitar o desenvolvimento.

#### Scenario: Cadastro bem-sucedido de novo usuário

- **WHEN** o usuário preenche username, e-mail e senha válidos no formulário de cadastro e confirma
- **THEN** o frontend chama `SignUp` no Floci, o Floci cria o usuário com confirmação automática (`UserConfirmed: true`), e o frontend em seguida autentica o usuário via `InitiateAuth` e redireciona para o Dashboard

#### Scenario: Cadastro com username já existente

- **WHEN** o usuário tenta cadastrar um username já registrado no Floci
- **THEN** o frontend exibe a mensagem de erro do Cognito ("User already exists") e mantém o formulário preenchido

### Requirement: Provisionamento Automático de Variáveis de Ambiente

O script `start_local.sh` SHALL escrever automaticamente o arquivo `app/frontend/.env.local` com os valores de `VITE_COGNITO_POOL_ID`, `VITE_COGNITO_CLIENT_ID` e `VITE_COGNITO_ENDPOINT` imediatamente após provisionar os recursos no Floci, sem necessidade de intervenção manual do desenvolvedor.

#### Scenario: `.env.local` criado após start_local.sh

- **WHEN** o desenvolvedor executa `bash start_local.sh`
- **THEN** o arquivo `app/frontend/.env.local` é criado (ou sobrescrito) com os três vars preenchidos com os valores reais do Floci

#### Scenario: Idempotência do script

- **WHEN** o desenvolvedor executa `bash start_local.sh` mais de uma vez
- **THEN** o `.env.local` é sobrescrito com os valores atuais e nenhum recurso duplicado é criado no Floci
