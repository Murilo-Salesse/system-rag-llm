## Why

A pasta `infra/` deve ser reservada estritamente para módulos e recursos de IaC (Terraform) para a nuvem AWS. Recursos de bootstrap local, incluindo containers Docker e scripts auxiliares para desenvolvimento, devem pertencer ao escopo da aplicação backend em `app/backend-api/local/`. Além disso, o emulador S3 (Floci) precisa inicializar com o bucket `notebooklm-sources` pré-criado para permitir testes imediatos, deixando o código-fonte Java para a próxima especificação.

## What Changes

- Remove `infra/docker-compose.yml` de `infra/`, preservando a pasta para futuros recursos Terraform.
- Cria o diretório `app/backend-api/local/` contendo:
  - `docker-compose.yml`: orquestração do PostgreSQL (pgvector) e Floci (`latest-compat`).
  - `init-s3.sh`: hook de inicialização montado em `/etc/floci/init/boot` para criar o bucket S3 `notebooklm-sources` no boot do container.
- O código-fonte Java permanece intocado, sendo escopo exclusivo da próxima especificação.

## Capabilities

### New Capabilities
Nenhuma. Mudança estrutural de organização de arquivos de desenvolvimento local e bootstrap do emulador S3.

### Modified Capabilities
Nenhuma.

## Impact

- `infra/`: Limpa de arquivos de bootstrap local para receber código Terraform.
- `app/backend-api/local/`: Centraliza o ambiente de desenvolvimento local e inicialização automática do bucket S3.
