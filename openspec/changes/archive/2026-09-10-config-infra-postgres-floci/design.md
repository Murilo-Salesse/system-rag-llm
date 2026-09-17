## Context

Configuração de infraestrutura local e desacoplamento de ambiente via 12-Factor App (Fator III - Config e Fator IV - Backing Services).

## Goals / Non-Goals

**Goals:**
- Prover ambiente local com Docker Compose contendo PostgreSQL 16 + pgvector e Floci (emulador S3 leve).
- Parametrizar a aplicação Spring Boot para ler variáveis de ambiente puras em produção e fornecer fallbacks seguros para execução local.

**Non-Goals:**
- Não criar migrations Flyway nesta etapa.
- Não implementar código de regras de negócio ou controladores.

## Decisions

1. **Docker Compose em `infra/docker-compose.yml`**: Mantém a raiz limpa e centraliza arquivos de infraestrutura.
2. **Floci para S3**: Alternativa ultrarrápida e sem autenticação para emulação local de AWS S3 na porta 4566.
3. **`application.yml` declarativo**: Usa expressões `${VAR:default}` permitindo injeção direta de variáveis pelo ECS Task Definition em produção.

## Risks / Trade-offs

- [Extensão pgvector não inicializada] → A imagem `pgvector/pgvector:pg16` já contém o binário pronto; basta `CREATE EXTENSION IF NOT EXISTS vector;` quando as migrations forem adicionadas.
