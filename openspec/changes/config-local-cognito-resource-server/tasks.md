## 1. Infraestrutura Local e Scripts de Automação

- [x] 1.1 Criar o script `app/backend-api/local/init-cognito.sh` montado no Floci para criar User Pool (`notebooklm-pool`), Client App (`notebooklm-client`) e usuário `admin` (senha `123`) com status confirmado de forma idempotente, e verificar a execução no container
- [x] 1.2 Atualizar `app/backend-api/local/docker-compose.yml` montando o volume `./init-cognito.sh:/etc/floci/init/boot/init-cognito.sh:ro` e verificar a sintaxe com `docker compose -f app/backend-api/local/docker-compose.yml config`
- [x] 1.3 Criar o script executável `app/backend-api/local/start_local.sh` que sobe o compose com `docker compose up -d`, aguarda a prontidão dos serviços, autentica o usuário `admin` e imprime os IDs e o Bearer Token JWT de teste no terminal

## 2. Configuração do Backend Spring Boot (OAuth2 Resource Server)

- [x] 2.1 Adicionar a dependência `spring-boot-starter-oauth2-resource-server` ao `app/backend-api/notebooklm/pom.xml` e verificar compilação com `./mvnw clean compile`
- [x] 2.2 Configurar `app/backend-api/notebooklm/src/main/resources/application.yml` com as propriedades de `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` apontando para o Floci local com fallback por variável de ambiente
- [x] 2.3 Implementar a classe de configuração `SecurityConfig` em `github.salessew.notebooklm.config` aplicando `SessionCreationPolicy.STATELESS`, desabilitando CSRF, configurando resource server JWT e liberando endpoints públicos

## 3. Testes Unitários e Quality Gate

- [x] 3.1 Implementar testes unitários JUnit 5 + Mockito para o `SecurityConfig` cobrindo regras de acesso e comportamento stateless
- [x] 3.2 Executar a skill `java-quality-gate` executando `quality-gate.sh` e garantir aprovação com exit code 0 antes de concluir a mudança
