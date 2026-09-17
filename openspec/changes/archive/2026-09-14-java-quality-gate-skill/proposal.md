## Why

O projeto não tem nenhum mecanismo automatizado de qualidade de testes. Testes são escritos ad-hoc sem garantia de cobertura de linhas nem de eficácia contra mutações de código. Sem um quality gate determinístico, testes podem passar 100% e ainda assim não cobrir comportamentos críticos do domínio RAG.

## What Changes

- **Nova skill `java-quality-gate`** instalada em `.claude/skills/java-quality-gate/SKILL.md`: instrui o agente a escrever testes JUnit 5 + Mockito e executar um pipeline determinístico de verificação de qualidade.
- **Script `quality-gate.sh`** em `app/backend-api/local/`: executa JaCoCo (line coverage ≥ 100%) e PITest (mutation kill rate ≥ 100%), parseia os XMLs e falha com `exit 1` se qualquer threshold não for atingido.
- **Plugin JaCoCo** adicionado ao `pom.xml` com goals `prepare-agent`, `report` e `check` ligados ao ciclo de vida de teste.
- **Plugin PITest** (`pitest-maven` + `pitest-junit5-plugin`) adicionado ao `pom.xml` configurado para rodar sobre classes do pacote `github.salessew.notebooklm` excluindo DTOs e configs triviais.
- **Rule `tasks`** adicionada ao `openspec/config.yaml` mandatando que toda tarefa de implementação inclua testes unitários verificados pelo `quality-gate.sh`.

## Capabilities

### New Capabilities

- `quality-gate`: Definição do contrato de qualidade de testes — linha 100%, mutação 100%, executado deterministicamente por script

### Modified Capabilities

_(nenhuma — sem mudança de comportamento do sistema em produção)_

## Impact

- **pom.xml**: Adição dos plugins JaCoCo e PITest. Não afeta artefato de produção (`<scope>test</scope>` / `<phase>test</phase>`).
- **Sem impacto em statelessness, AWS ou latência**: qualidade de testes é ferramenta de desenvolvimento local e CI.
- **Sem migrations DDL**: não há alteração de schema.
- **Sem mudança de contrato REST**: não há novos endpoints.
- **`openspec/config.yaml`**: nova rule em `tasks` e `operations.apply` referenciando a skill.
