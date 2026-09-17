## Context

Atualmente o ambiente de desenvolvimento local (`app/backend-api/local/docker-compose.yml`) sobe o PostgreSQL 16 (pgvector) e o Floci (emulador S3/AWS).
Para testar a segurança stateless e os endpoints do sistema com fidelidade à arquitetura definida no `ARCHITECTURE.md` e no `DOMAIN.md`, precisamos que o emulador Floci também gerencie o serviço AWS Cognito User Pool localmente, e que o backend Spring Boot atue como OAuth2 Resource Server consumindo o JWKS provido por essa rota.

## Goals / Non-Goals

**Goals:**
- Configurar script de inicialização (`app/backend-api/local/init-cognito.sh`) montado no Floci (`/etc/floci/init/boot`) para criar User Pool, App Client e o usuário `admin` (senha `123`) idempotentemente.
- Criar script `app/backend-api/local/start_local.sh` que executa `docker compose up -d`, aguarda prontidão dos serviços, autentica `admin` / `123` e exibe o token JWT Bearer gerado no console.
- Integrar `spring-boot-starter-oauth2-resource-server` no backend Spring Boot.
- Configurar `application.yml` e classe `SecurityConfig` com `SessionCreationPolicy.STATELESS`, validação de JWT via JWKS local e proteção de rotas `/api/**`.
- Garantir 100% de conformidade com os testes unitários e com o quality gate (`quality-gate.sh`).

**Non-Goals:**
- Configurar federação externa real (Google/GitHub IdPs) em ambiente local (a federação local é simulada pelo fluxo direto com o User Pool do Floci).
- Criar telas de front-end ou hosted UI no escopo desta alteração.

## Decisions

### 1. Provisionamento via Hook de Boot do Floci (`/etc/floci/init/boot`)
- **Decisão**: Utilizar um script shell `init-cognito.sh` montado no diretório `/etc/floci/init/boot` do container Floci.
- **Rationale**: Assim como já funciona para o bucket S3 com `init-s3.sh`, o Floci executa automaticamente todos os scripts executáveis nessa pasta durante a inicialização. Qualquer desenvolvedor rodando `docker compose up` ou `start_local.sh` terá os recursos criados de forma idempotente sem precisar de containers secundários.
- **Alternativa Considerada**: Criar um container temporário de setup ou rodar comandos AWS CLI no host. Foi descartado pois exigiria AWS CLI instalado na máquina do desenvolvedor.

### 2. Validação via `jwk-set-uri` ao invés de `issuer-uri`
- **Decisão**: Configurar `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` apontando para `http://localhost:4566/${COGNITO_USER_POOL_ID:notebooklm-pool}/.well-known/jwks.json`.
- **Rationale**: A validação via `issuer-uri` força o Spring Security a consultar o `.well-known/openid-configuration` e validar estritamente a claim `iss` com a URL pública exata do issuer. No ambiente local emulado com Floci/LocalStack, o host pode variar (`localhost` vs `floci` em rede de containers). O `jwk-set-uri` valida diretamente a assinatura criptográfica via JWKS sem falhar por divergência de hostname no `iss`.
- **Alternativa Considerada**: Usar `issuer-uri`. Descartado devido à fragilidade em rede Docker vs host localhost.

### 3. Script Wrapper `start_local.sh` com Teste de Token JWT
- **Decisão**: O script `start_local.sh` sobe os containers, aguarda a inicialização, invoca `admin-initiate-auth` contra o Floci e exibe no terminal as variáveis de ambiente recomendadas (`COGNITO_USER_POOL_ID`, `COGNITO_CLIENT_ID`) e o token JWT pronto para cópia (`Bearer eyJ...`).
- **Rationale**: Facilita o onboarding e os testes imediatos de endpoints protegidos no Swagger, Postman ou curl.

## Risks / Trade-offs

- **[Risco] Suporte do Floci ao endpoint JWKS do Cognito**: Emuladores locais podem ter particularidades na geração do JWKS.
  - *Mitigação*: Testar o formato do JWKS e, se necessário, utilizar chaves públicas conhecidas ou configurar decoder de JWT desacoplado e mockável nos testes unitários.
- **[Risco] Testes unitários do Spring Boot falharem por tentar conectar ao JWKS real no startup**:
  - *Mitigação*: Configurar testes com `@MockBean JwtDecoder` ou desabilitar autenticação nos testes de slice MVC existentes através de mocks de segurança, garantindo 100% de passagem sem dependência de container ligado.
