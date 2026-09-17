## Why

O projeto adota uma abordagem local-first para desenvolvimento e testes, permitindo executar toda a stack de infraestrutura e backend sem dependência direta de contas ativas na AWS. Atualmente, o Floci emula apenas o Amazon S3, sem um User Pool local do AWS Cognito configurado nem usuário de teste (`admin`), e o backend Spring Boot ainda não possui as dependências e configurações de Spring Security OAuth2 Resource Server para validar tokens Bearer JWT via JWKS local. Esta mudança viabiliza a execução local completa e reprodutível da autenticação e proteção de endpoints.

## What Changes

- **Provisionamento Automático no Floci (Hook de Boot)**: Adiciona script de inicialização `app/backend-api/local/init-cognito.sh` montado em `/etc/floci/init/boot` para provisionar automaticamente o User Pool (`notebooklm-pool`), Client App (`notebooklm-client`), usuário administrador (`admin`, senha `123`) com status permanente e exportar identificadores necessários.
- **Docker Compose Local**: Atualiza `app/backend-api/local/docker-compose.yml` montando `init-cognito.sh` e expondo variáveis de ambiente de Cognito.
- **Script de Início e Teste (`start_local.sh`)**: Cria `app/backend-api/local/start_local.sh` que sobe os containers com `docker compose up -d`, aguarda a inicialização do Floci e PostgreSQL, executa autenticação com `admin` / `123` e exibe um token JWT pronto para uso em testes.
- **Backend Spring Security OAuth2 Resource Server**:
  - Adiciona `spring-boot-starter-oauth2-resource-server` ao `pom.xml`.
  - Configura `application.yml` apontando o `jwk-set-uri` para a rota well-known do Cognito no Floci (`http://localhost:4566/{userPoolId}/.well-known/jwks.json`).
  - Cria classe de configuração de segurança (`SecurityConfig`) impondo arquitetura 100% stateless (`SessionCreationPolicy.STATELESS`), desabilitando CSRF e protegendo as rotas de API com Bearer JWT.

## Capabilities

### New Capabilities
<!-- Nenhuma nova capability de negócio; estamos estendendo/detalhando a capability existente de auth-cognito -->

### Modified Capabilities
- `auth-cognito`: Atualiza os cenários e requisitos para contemplar o suporte ao endpoint JWKS do Cognito local (Floci) em ambiente de desenvolvimento e o provisionamento do admin inicial.

## Impact

- **Affected Code**: `app/backend-api/notebooklm/pom.xml`, `app/backend-api/notebooklm/src/main/resources/application.yml`, nova classe de segurança em `github.salessew.notebooklm.config.SecurityConfig`.
- **Local Infrastructure**: `app/backend-api/local/docker-compose.yml`, `app/backend-api/local/init-cognito.sh`, `app/backend-api/local/start_local.sh`.
- **Dependencies**: Inclusão de `org.springframework.boot:spring-boot-starter-oauth2-resource-server`.
- **Statelessness & Performance**: 100% stateless, sem sessões HTTP; validação local de chave pública via JWKS em cache pelo Spring Security com latência sub-milisegundo.
