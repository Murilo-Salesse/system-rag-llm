## Context

Separação de responsabilidades:
- `infra/`: Exclusivo para Infraestrutura como Código (IaC) com Terraform para provisionamento de recursos na AWS.
- `app/backend-api/local/`: Contém arquivos de suporte ao desenvolvimento local da API backend (Docker Compose, scripts de seed/bootstrap, etc.).
- O código-fonte Java será implementado na próxima especificação.

## Goals / Non-Goals

**Goals:**
- Mover a infraestrutura local para `app/backend-api/local/docker-compose.yml`.
- Configurar inicialização automática do Floci com o bucket `notebooklm-sources` via hook `/etc/floci/init/boot`.
- Deixar `infra/` limpa de arquivos locais para receber templates Terraform.

**Non-Goals:**
- Não criar código Terraform nesta mudança.
- Não implementar código-fonte Java nesta etapa.

## Decisions

1. **Floci Startup Hook**: Usar a imagem `floci/floci:latest-compat` e montar `app/backend-api/local/init-s3.sh` em `/etc/floci/init/boot`. O Floci executa scripts dessa pasta no startup, garantindo o bucket criado sem necessidade de containers extras de setup.
2. **Localização do Compose**: `app/backend-api/local/docker-compose.yml` permite subir a infraestrutura de apoio ao backend diretamente no contexto do backend (`docker compose -f app/backend-api/local/docker-compose.yml up -d`).
3. **Reserva de `infra/`**: Destinada exclusivamente a arquivos `.tf`, variáveis e módulos Terraform.

## Risks / Trade-offs

- [Permissão de execução do script de init] → `init-s3.sh` deve ter permissão `+x` no repositório.
