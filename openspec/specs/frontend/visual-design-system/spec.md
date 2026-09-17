## Purpose

Define os tokens visuais canônicos do frontend — cores, tipografia, espaçamento, border-radius e transições — derivados do `DESIGN.md`, garantindo que todas as telas do NotebookLM apliquem a mesma identidade visual sem divergências.

## Requirements

### Requirement: Tokens de Design Aplicados Globalmente

O frontend SHALL utilizar exclusivamente os tokens definidos em `DESIGN.md` para cor primária (`#8e8ea0`), border-radius (`5px`), transições (`400ms ease`) e tipografia (`system-ui, sans-serif`) em todos os componentes de interface.

#### Scenario: Cor primária consistente em botões de ação

- **WHEN** o usuário visualiza qualquer botão de ação primária (login, criar notebook, enviar mensagem)
- **THEN** o botão exibe cor de fundo `#8e8ea0`, texto branco (`#ffffff`) e borda arredondada de exatamente `5px`

#### Scenario: Ausência de sombras em superfícies e modais

- **WHEN** o usuário abre qualquer modal, card ou panel
- **THEN** nenhuma sombra (`box-shadow`) é aplicada; a separação visual é feita por bordas ou cor de fundo

#### Scenario: Transições uniformes

- **WHEN** o usuário interage com qualquer elemento interativo (hover, focus, clique)
- **THEN** a transição visual ocorre em `400ms` com easing `ease`, sem diferença entre elementos

### Requirement: Layout Revisado da WorkspacePage

A `WorkspacePage` SHALL apresentar o painel lateral de fontes com largura mínima de `280px`, hierarquia visual clara (cabeçalho de seção, lista de fontes com status, área de ações de upload), e a área de chat com espaçamento generoso baseado no grid de `8px`.

#### Scenario: Painel de fontes com largura adequada

- **WHEN** o usuário acessa `/notebooks/:id` em viewport ≥ 768px
- **THEN** o painel lateral de fontes ocupa pelo menos `280px` de largura, exibindo nome da fonte, badge de status e checkbox de seleção sem truncamento excessivo

#### Scenario: Chat com área de leitura confortável

- **WHEN** o usuário lê mensagens na área de chat
- **THEN** as mensagens possuem `line-height` de `1.5`, espaçamento vertical entre mensagens ≥ `16px`, e a área de input está fixada na parte inferior com espaçamento interno de `16px`
