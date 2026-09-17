---
name: java-quality-gate
description: >
  Instrui o agente a implementar testes unitários JUnit 5 + Mockito e verificar
  cobertura de forma determinística com o script quality-gate.sh. Garante no mínimo 80%
  de line coverage (JaCoCo) e no mínimo 80% de mutation kill rate (PITest) antes de
  marcar qualquer tarefa de implementação como concluída. Use ao implementar
  qualquer classe de serviço, domínio ou utilitário do projeto NotebookLM.
license: MIT
---

# java-quality-gate

Toda tarefa de implementação **DEVE** terminar com o quality gate passando.
Nenhuma tarefa pode ser marcada `[x]` enquanto `quality-gate.sh` retornar exit code ≠ 0.

---

## Pipeline obrigatório

Para cada tarefa de implementação:

```
1. Escreva/atualize a classe de produção
2. Escreva/atualize os testes unitários JUnit 5 + Mockito
3. ./mvnw test                                           # gera target/site/jacoco/jacoco.xml
4. ./mvnw org.pitest:pitest-maven:mutationCoverage       # gera target/pit-reports/mutations.xml
5. ../local/quality-gate.sh                              # ou ./app/backend-api/local/quality-gate.sh
6. Se exit code ≠ 0 → corrija e volte ao passo 2
7. Se exit code = 0 → marque a tarefa [x]
```

---

## Identificar classes-alvo de teste

**Incluir** (requerem testes unitários):
- `*Service`, `*ServiceImpl` — lógica de negócio
- `*DomainService`, `*DomainModel` — regras de domínio
- `*Util`, `*Helper`, `*Validator` — utilitários
- `*Mapper`, `*Converter` — transformação de dados
- `*Repository` (métodos com lógica custom) — acesso a dados com lógica
- Qualquer classe com lógica de branch (if/else, switch, try/catch de negócio)

**Excluir** (não requerem testes unitários puros, excluídos dos plugins):
- `*Application` — bootstrap Spring, sem lógica testável
- `*Config`, `*Configuration` — beans de infraestrutura
- `*Properties` — `@ConfigurationProperties`, valores mapeados de YAML
- Records usados apenas como DTO sem lógica de validação

---

## Estrutura dos testes JUnit 5 + Mockito

### Template base

```java
package github.salessew.notebooklm.<dominio>;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NomeDaClasseTest {

    @Mock
    DependenciaA dependenciaA;   // uma por dependência injetada

    @Mock
    DependenciaB dependenciaB;

    @InjectMocks
    NomeDaClasse sut;            // System Under Test

    @Test
    void deve_descrever_comportamento_esperado() {
        // Given
        when(dependenciaA.metodo(any())).thenReturn(valor);

        // When
        var resultado = sut.metodoTestado(entrada);

        // Then
        assertThat(resultado).isEqualTo(esperado);
        verify(dependenciaA).metodo(entrada);
    }

    @Test
    void deve_lancar_excecao_quando_entrada_invalida() {
        // Given
        when(dependenciaA.metodo(null)).thenThrow(new IllegalArgumentException("inválido"));

        // When / Then
        assertThatThrownBy(() -> sut.metodoTestado(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("inválido");
    }
}
```

### Regras obrigatórias

- `@ExtendWith(MockitoExtension.class)` em todas as classes de teste unitário
- `@Mock` para cada dependência do construtor/campo — **nunca** `@Autowired` em testes unitários
- `@InjectMocks` na classe sendo testada — sem `new` manual se houver dependências
- **Um `@Test` por comportamento** — não misture múltiplos comportamentos em um método
- Nomes de teste em português descritivo: `deve_retornar_lista_vazia_quando_notebook_sem_fontes()`
- Use `assertThat` (AssertJ) em vez de `assertEquals` — mais legível no output de falha
- Cubra **todos** os branches: caminho feliz, edge cases, exceções, valores nulos/vazios

### Cobrindo branches difíceis

**Checked exceptions:**
```java
@Test
void deve_lancar_quando_io_falha() throws IOException {
    when(dep.read()).thenThrow(new IOException("falha"));
    assertThatThrownBy(() -> sut.processar())
        .isInstanceOf(RuntimeException.class)
        .hasCauseInstanceOf(IOException.class);
}
```

**Optional vazio:**
```java
@Test
void deve_retornar_vazio_quando_nao_encontrado() {
    when(repo.findById(99L)).thenReturn(Optional.empty());
    assertThat(sut.buscar(99L)).isEmpty();
}
```

**Listas/coleções vazias:**
```java
@Test
void deve_retornar_lista_vazia_quando_sem_resultados() {
    when(repo.findAll()).thenReturn(List.of());
    assertThat(sut.listar()).isEmpty();
}
```

---

## Configuração dos plugins (referência)

Os plugins já estão configurados no `pom.xml`. Exclusões aplicadas:

**JaCoCo** — exclui das métricas:
- `**/*Application.class`
- `**/*Config.class`
- `**/*Configuration.class`
- `**/*Properties.class`

**PITest** — mutadores excluídos (geram falsos positivos em código Spring):
- `VOID_METHOD_CALLS`
- `EMPTY_RETURNS`
- `NULL_RETURNS`

---

## Interpretando falhas do quality-gate.sh

### Falha JaCoCo

```
❌ Cobertura de linhas < 100%:
   NotebookService: 12/15 linhas (80.0%)
```

→ Abra `target/site/jacoco/index.html` para ver quais linhas não estão cobertas.
→ Adicione testes para os branches faltantes.

### Falha PITest

```
❌ 2 mutante(s) sobrevivente(s):
   NotebookService:45 [ConditionalsBoundary]
   SourceProcessor:92 [NegateConditionals]
```

→ Um mutante sobrevivente significa que uma mudança sutil no código (`<` → `<=`, `!` removido)
   não é detectada por nenhum teste.
→ Adicione um teste que **falhe** quando esse branch mudar.

Exemplo: mutante `ConditionalsBoundary` em `if (count > 0)`:
```java
@Test
void deve_processar_quando_count_exatamente_um() {
    // Testa count = 1 (boundary), não apenas count > 1
    assertThat(sut.processar(1)).isTrue();
}

@Test
void nao_deve_processar_quando_count_zero() {
    assertThat(sut.processar(0)).isFalse();
}
```

---

## Checklist de conclusão de tarefa

Antes de marcar `[x]` em qualquer tarefa de implementação:

- [ ] Classe de produção implementada
- [ ] Testes unitários escritos para todos os branches relevantes
- [ ] `./mvnw test` passa sem erros de compilação ou falhas
- [ ] `./mvnw org.pitest:pitest-maven:mutationCoverage` executa sem erro
- [ ] `../local/quality-gate.sh` retorna exit code 0
- [ ] Nenhum `//TODO`, `//FIXME` ou método vazio deixado na classe de produção
