## ADDED Requirements

### Requirement: Provisionamento Local do Cognito e Ambiente de Testes
O ambiente local (Floci) DEVE provisionar automaticamente um User Pool dedicado com Client App e um usuário administrador padrão (`admin`, senha `123`, status confirmado) no boot dos containers para testes locais reproduzíveis e sem dependência de internet.

#### Scenario: Inicialização automática do Cognito no Floci
- **WHEN** Os containers locais sobem via Docker Compose ou script de boot
- **THEN** O Floci inicializa o User Pool `notebooklm-pool`, registra o Client `notebooklm-client` e cria o usuário `admin` com senha `123` e status confirmado de forma idempotente.

#### Scenario: Script de conveniência start_local.sh com geração de token de teste
- **WHEN** O desenvolvedor executa `start_local.sh`
- **THEN** O script garante que os containers estão saudáveis, autentica o usuário `admin` com senha `123` contra o Floci local e imprime o token JWT Bearer no console pronto para uso.

## MODIFIED Requirements

### Requirement: Validação Stateless de JWT
O backend Spring Boot deve atuar exclusivamente como OAuth2 Resource Server stateless, validando as assinaturas dos tokens JWT através das chaves públicas JWKS expostas pelo AWS Cognito (ou pelo emulador Floci em ambiente de desenvolvimento).

#### Scenario: Requisição autenticada com Bearer Token válido
- **WHEN** O cliente envia uma requisição com header `Authorization: Bearer <valid_jwt>`
- **THEN** O Spring Security valida a assinatura e expiração do token via JWKS configurado (`jwk-set-uri`), extrai o identificador único do usuário (`sub`) e permite acesso aos endpoints protegidos sem criar sessão HTTP em memória (`SessionCreationPolicy.STATELESS`).

#### Scenario: Requisição com token ausente ou expirado
- **WHEN** O cliente envia uma requisição sem token ou com token JWT inválido/expirado
- **THEN** O sistema retorna imediatamente `HTTP 401 Unauthorized`.
