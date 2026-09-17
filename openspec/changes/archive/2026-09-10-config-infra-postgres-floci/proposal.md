## Why

O projeto precisa de um ambiente local funcional e desacoplado de nuvem, compatível com o 12-Factor App, fornecendo PostgreSQL com suporte a vetores (pgvector) e armazenamento S3 local antes de iniciar a implementação do backend.

## What Changes

- Adiciona `infra/docker-compose.yml` contendo os serviços `postgresql` (imagem `pgvector/pgvector:pg16`) e `floci` (emulador de AWS S3).
- Adiciona `.env.example` na raiz com as variáveis de ambiente necessárias para execução local e cloud.
- Substitui `application.properties` por `application.yml` em `app/backend-api/notebooklm/src/main/resources/` parametrizando conexão com PostgreSQL e S3 via variáveis de ambiente com fallbacks locais.

## Capabilities

### New Capabilities
Nenhuma. Mudança estrutural de configuração e infraestrutura local.

### Modified Capabilities
Nenhuma.

## Impact

- `infra/docker-compose.yml`: Novo arquivo de orquestração local.
- `.env.example`: Novo arquivo modelo de configuração.
- `app/backend-api/notebooklm/src/main/resources/application.yml`: Substitui `application.properties`.
