## Purpose

Define o contrato de qualidade de testes unitários do projeto: todo código de produção do pacote `github.salessew.notebooklm` DEVE ter 100% de cobertura de linhas verificada pelo JaCoCo e 100% de mutações eliminadas verificadas pelo PITest, com verificação executada deterministicamente por script.

## ADDED Requirements

### Requirement: Cobertura de linhas 100% verificada automaticamente
O sistema de build SHALL produzir um relatório JaCoCo após cada execução de testes e o quality gate script MUST falhar com exit code não-zero se a cobertura de linhas for inferior a 100% em qualquer classe do pacote `github.salessew.notebooklm`, exceto classes explicitamente excluídas na configuração do plugin.

#### Scenario: Cobertura total atingida
- **WHEN** `./mvnw test` é executado e todos os testes passam com 100% de linhas cobertas
- **THEN** o script `quality-gate.sh` termina com exit code 0

#### Scenario: Cobertura insuficiente detectada
- **WHEN** `./mvnw test` é executado e alguma linha de produção não é exercitada por nenhum teste
- **THEN** o script `quality-gate.sh` termina com exit code 1 e imprime o nome da classe e a porcentagem de cobertura atingida

#### Scenario: Relatório XML ausente
- **WHEN** `quality-gate.sh` é executado antes de `./mvnw test` ou após uma falha de compilação
- **THEN** o script termina com exit code 1 e imprime mensagem indicando que o relatório JaCoCo não foi encontrado

### Requirement: Kill rate de mutações 100% verificado automaticamente
O quality gate script MUST executar PITest e falhar com exit code não-zero se qualquer mutante sobreviver nas classes do pacote `github.salessew.notebooklm`, exceto classes e mutadores explicitamente excluídos na configuração do plugin.

#### Scenario: Todas as mutações eliminadas
- **WHEN** `./mvnw test org.pitest:pitest-maven:mutationCoverage` é executado e nenhum mutante sobrevive
- **THEN** o script `quality-gate.sh` termina com exit code 0

#### Scenario: Mutante sobrevivente detectado
- **WHEN** PITest identifica ao menos um mutante sobrevivente
- **THEN** o script `quality-gate.sh` termina com exit code 1 e imprime o nome da classe, linha e tipo do mutante sobrevivente

#### Scenario: Relatório XML de mutações ausente
- **WHEN** `quality-gate.sh` é executado sem relatório PITest gerado
- **THEN** o script termina com exit code 1 e imprime mensagem indicando que o relatório PITest não foi encontrado

### Requirement: Skill instrui o agente a escrever testes antes de implementar
A skill `java-quality-gate` MUST instruir o agente a escrever testes unitários JUnit 5 + Mockito para cada classe de serviço, domínio e utilitário antes de considerar uma tarefa de implementação concluída. O agente SHALL executar `quality-gate.sh` como etapa final de cada tarefa de implementação e iterar até o gate passar.

#### Scenario: Tarefa de implementação com gate passando
- **WHEN** o agente conclui a implementação de uma classe e executa `quality-gate.sh`
- **THEN** o script retorna exit code 0 e o agente marca a tarefa como concluída

#### Scenario: Tarefa de implementação com gate falhando
- **WHEN** `quality-gate.sh` retorna exit code 1
- **THEN** o agente escreve ou corrige testes e reexecuta o script antes de concluir a tarefa

### Requirement: Exclusões de classes configuradas explicitamente
O sistema SHALL excluir das verificações de cobertura e mutação somente as classes declaradas explicitamente na configuração dos plugins — nunca excluir silenciosamente. Classes de configuração Spring (`*Config`, `*Configuration`, `*Properties`) e a classe principal de bootstrap (`*Application`) MUST ser excluídas. Qualquer outra exclusão MUST ser declarada com comentário justificando.

#### Scenario: Classe de configuração Spring excluída corretamente
- **WHEN** JaCoCo e PITest são executados
- **THEN** classes anotadas com `@Configuration`, `@SpringBootApplication` e sufixo `Config`/`Properties` são ignoradas nos relatórios e não causam falha no gate

#### Scenario: Tentativa de exclusão não declarada
- **WHEN** uma exclusão de classe não está presente na configuração do plugin
- **THEN** JaCoCo e PITest incluem a classe na verificação e o gate falha se a cobertura for insuficiente
