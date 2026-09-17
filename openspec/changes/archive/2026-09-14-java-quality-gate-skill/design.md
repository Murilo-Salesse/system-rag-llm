## Context

O projeto usa Maven com Spring Boot 4.1.1 e Java 21. O `pom.xml` atual tem apenas o `spring-boot-maven-plugin` na seção `<build><plugins>`. Não existe nenhum plugin de cobertura de código ou teste mutante configurado. Testes existentes são apenas `@SpringBootTest` de carregamento de contexto. Agentes que implementam novas features não têm nenhum gate de qualidade automatizado — testes podem ser omitidos ou triviais sem que o pipeline perceba.

## Goals / Non-Goals

**Goals:**
- Configurar JaCoCo no `pom.xml` com goals `prepare-agent`, `report` e `check` (threshold 100% linha, excluindo classes de config/bootstrap)
- Configurar PITest no `pom.xml` com `pitest-maven` + `pitest-junit5-plugin`, excluindo mutadores triviais e classes de config/bootstrap
- Criar script `quality-gate.sh` em `app/backend-api/local/` que parseia os XMLs gerados e retorna exit code determinístico
- Criar a skill `java-quality-gate` em `.claude/skills/java-quality-gate/SKILL.md` que instrui o agente a usar o pipeline acima
- Adicionar rule em `openspec/config.yaml` na seção `tasks` mandatando uso da skill e em `operations.apply` mandatando execução do gate

**Non-Goals:**
- Testes de integração ou `@SpringBootTest` — PITest só roda em testes unitários puros
- Cobertura de branches (branch coverage) — somente line coverage via JaCoCo
- Integração com CI/CD (GitHub Actions, etc.) — fora de escopo desta spec
- Relatório HTML de cobertura como artefato — apenas XML para consumo do script

## Decisions

### D1 — JaCoCo `check` goal vinculado ao ciclo Maven vs. script externo

**Escolha:** script externo `quality-gate.sh` parseia o XML, não `jacoco:check` no build.

**Rationale:** `jacoco:check` falha o `./mvnw test` inteiro antes de gerar relatório PITest. Com script externo, rodamos `./mvnw test` (gera JaCoCo XML), depois `./mvnw pitest:mutationCoverage` (gera PITest XML), depois `quality-gate.sh` parseia ambos e retorna um único exit code com diagnóstico claro. O agente pode ver os dois resultados antes de decidir o que corrigir.

**Alternativa considerada:** `jacoco:check` + verificação PITest separada. Descartado porque interrompe o pipeline antes de coletar todos os dados.

### D2 — PITest: exclusão de mutadores

**Escolha:** Excluir `VOID_METHOD_CALLS`, `EMPTY_RETURNS` e `NULL_RETURNS` por padrão.

**Rationale:** Esses mutadores geram mutantes em código de infraestrutura Spring (setters de JPA, void callbacks) que são estruturalmente impossíveis de matar com testes unitários puros sem mockar o framework internamente — o que seria testar o framework, não o código de negócio.

**Alternativa considerada:** Excluir apenas `VOID_METHOD_CALLS`. Descartado porque `EMPTY_RETURNS`/`NULL_RETURNS` em métodos `Optional` de repositório mock geram falsos positivos frequentes.

### D3 — Classes excluídas da cobertura e mutação

**Escolha:** Excluir padrões:
- `**/*Application.class` — bootstrap Spring, sem lógica testável
- `**/*Config.class` / `**/*Configuration.class` — beans de infraestrutura
- `**/*Properties.class` — `@ConfigurationProperties`, valores mapeados do YAML

**Rationale:** Essas classes são cola de framework. Escrever testes unitários para elas testa o Spring, não o domínio. A configuração é testada indiretamente pelos testes de integração quando o contexto sobe.

**Alternativa considerada:** Excluir `**/dto/**`. Descartado — records Java imutáveis usados como DTO têm lógica de validação (`@NotNull`, etc.) que vale cobrir.

### D4 — Localização do script `quality-gate.sh`

**Escolha:** `app/backend-api/local/quality-gate.sh`

**Rationale:** Mantém todos os scripts de ambiente local no mesmo diretório (`docker-compose.yml`, `init.sql`, `init-s3.sh`). O script é invocado de dentro do módulo Maven (`app/backend-api/notebooklm/`), então o caminho relativo é `../local/quality-gate.sh`.

### D5 — Localização da skill

**Escolha:** `.claude/skills/java-quality-gate/SKILL.md` (exclusiva deste projeto)

**Rationale:** Skill específica do projeto — depende de convenções de pacote (`github.salessew.notebooklm`) e de paths locais (`app/backend-api/local/quality-gate.sh`). Não é candidata a ser genérica.

### D6 — Integração com OpenSpec via `config.yaml`

**Escolha:** Adicionar rule na seção `tasks` e guidance em `operations.apply`.

**Rationale:** A rule em `tasks` garante que qualquer agente que gere um `tasks.md` via `/opsx-propose` já receba a instrução de incluir tarefas de teste e executar o gate. A guidance em `apply` garante que ao implementar o agente leia e siga a skill.

## Risks / Trade-offs

- **PITest é lento** → Em projetos grandes, `mutationCoverage` pode levar minutos. Mitigação: configurar `<targetClasses>` no plugin para rodar apenas no pacote de negócio, excluindo pacotes de config.
- **100% line coverage pode forçar testes frágeis** → Linhas em catch blocks de exceções checked raramente lançadas podem exigir mocks complexos. Mitigação: revisão pontual; a skill instrui o agente a usar `assertThrows` em vez de mocks de exception para esses casos.
- **`pitest-junit5-plugin` versão deve ser compatível com JUnit 5.x** → A versão do plugin deve ser mantida em sincronia com a versão do JUnit trazida pelo Spring Boot BOM. Mitigação: declarar a versão explicitamente no `pom.xml` e não herdar do BOM.
