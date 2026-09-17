## 1. Bootstrap e Configuração do Floci

- [x] 1.1 Criar `app/backend-api/local/init-s3.sh` com comando de criação do bucket `notebooklm-sources` e permissão de execução
- [x] 1.2 Criar `app/backend-api/local/docker-compose.yml` com `postgresql` (pgvector:pg16) e `floci` (`latest-compat`) montando `init-s3.sh` em `/etc/floci/init/boot`
- [x] 1.3 Validar sintaxe do compose com `docker compose -f app/backend-api/local/docker-compose.yml config`

## 2. Limpeza da Pasta de Infra

- [x] 2.1 Remover `infra/docker-compose.yml` e verificar que a pasta `infra/` permanece reservada para Terraform
