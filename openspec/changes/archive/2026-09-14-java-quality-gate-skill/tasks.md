## 1. Configurar JaCoCo no pom.xml

- [x] 1.1 Adicionar plugin `jacoco-maven-plugin` na seção `<build><plugins>` do `pom.xml` com goals `prepare-agent` (phase `initialize`) e `report` (phase `test`) vinculados ao ciclo padrão; verificar que `./mvnw test` gera `target/site/jacoco/jacoco.xml`
- [x] 1.2 Configurar exclusões no plugin JaCoCo para os padrões `**/*Application.class`, `**/*Config.class`, `**/*Configuration.class`, `**/*Properties.class`; verificar que classes de config não aparecem no relatório XML

## 2. Configurar PITest no pom.xml

- [x] 2.1 Adicionar plugin `pitest-maven` e dependência `pitest-junit5-plugin` no `pom.xml` com `<targetClasses>github.salessew.notebooklm.**</targetClasses>` e `<excludedClasses>` cobrindo os mesmos padrões do JaCoCo; verificar que `./mvnw org.pitest:pitest-maven:mutationCoverage` executa sem erro de configuração
- [x] 2.2 Configurar exclusão dos mutadores `VOID_METHOD_CALLS`, `EMPTY_RETURNS`, `NULL_RETURNS` no plugin PITest e verificar que o relatório XML é gerado em `target/pit-reports/*/mutations.xml`

## 3. Criar script quality-gate.sh

- [x] 3.1 Criar `app/backend-api/local/quality-gate.sh` com permissão de execução (`chmod +x`); o script deve: (a) verificar se `target/site/jacoco/jacoco.xml` existe — exit 1 se ausente; (b) parsear o XML com `python3` ou `awk` e extrair covered/total lines; (c) calcular percentual; (d) exit 1 com diagnóstico se < 100%
- [x] 3.2 Adicionar ao `quality-gate.sh` verificação do relatório PITest: (a) localizar `target/pit-reports/*/mutations.xml` via glob; (b) exit 1 se não encontrado; (c) parsear com `python3` ou `awk` contando `<mutation detected='false'>`; (d) exit 1 com lista das classes/linhas de mutantes sobreviventes se count > 0
- [x] 3.3 Verificar o script end-to-end: executar `./mvnw test` a partir de `app/backend-api/notebooklm/`, depois `./mvnw org.pitest:pitest-maven:mutationCoverage`, depois `../local/quality-gate.sh` — o teste de contexto existente deve resultar em exit 0 ou os relatórios gerados devem ser legíveis pelo script

## 4. Criar skill java-quality-gate

- [x] 4.1 Criar `.claude/skills/java-quality-gate/SKILL.md` com frontmatter YAML (`name`, `description`) e instruções completas para o agente: (a) identificar classes de produção alvo (service, domain, util — excluir `*Config`, `*Application`, `*Properties`); (b) escrever testes JUnit 5 + Mockito para cada classe; (c) usar `@ExtendWith(MockitoExtension.class)` e `@Mock`/`@InjectMocks`; (d) cobrir todos os branches de negócio relevantes; (e) executar `quality-gate.sh` como etapa final; verificar que o arquivo existe e tem conteúdo ≥ 50 linhas
- [x] 4.2 Copiar o mesmo `SKILL.md` para `.agent/skills/java-quality-gate/SKILL.md` (compatibilidade com AGY/Antigravity); verificar que ambos os arquivos têm conteúdo idêntico

## 5. Atualizar openspec/config.yaml

- [x] 5.1 Adicionar rule na seção `tasks` do `openspec/config.yaml`: `"Cada tarefa de implementação DEVE incluir sub-tarefa de escrever testes unitários JUnit 5 + Mockito e executar a skill java-quality-gate via quality-gate.sh antes de marcar a tarefa como concluída"`; verificar que `openspec validate` passa após a edição
- [x] 5.2 Adicionar guidance na seção `operations.apply` do `openspec/config.yaml`: `"Invocar a skill java-quality-gate (.claude/skills/java-quality-gate/SKILL.md) ao implementar qualquer classe de serviço, domínio ou utilitário; executar quality-gate.sh (app/backend-api/local/quality-gate.sh) e garantir exit 0 antes de concluir"`; verificar que `openspec validate` passa
