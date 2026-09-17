## 1. Infraestrutura Docker

- [x] 1.1 Criar `infra/docker-compose.yml` com os serviços `postgresql` (pgvector:pg16) e `floci` (S3) e verificar sintaxe com `docker compose -f infra/docker-compose.yml config`

## 2. Configuração de Variáveis de Ambiente

- [x] 2.1 Criar `.env.example` na raiz do projeto com as variáveis necessárias para desenvolvimento local e documentação de produção e verificar existência do arquivo

## 3. Parametrização Spring Boot

- [x] 3.1 Remover `app/backend-api/notebooklm/src/main/resources/application.properties` e criar `application.yml` parametrizado com fallbacks locais e verificar compilação/sintaxe
