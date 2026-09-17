## Context

O frontend MVP (`create-frontend-mvp`) foi construído com Tailwind padrão: `blue-600` como cor de ação, `rounded-xl`/`rounded-lg` como radius, `shadow-xl` nos modais e `bg-gray-50` como background. O `DESIGN.md` define um sistema diferente — baseado no design OpenAI — que usa `#8e8ea0`, radius fixo de `5px`, zero sombras e transições uniformes de `400ms ease`.

O Floci já suporta `USER_PASSWORD_AUTH` e `SignUp` via API Cognito emulada na porta `4566`. O `start_local.sh` já lê `POOL_ID` e `CLIENT_ID` mas não os escreve em `.env.local`.

## Goals / Non-Goals

**Goals:**
- Alinhar 100% do CSS do frontend com os tokens do `DESIGN.md`
- Revisar o layout da `WorkspacePage` (sidebar mais larga, chat mais respirado)
- Entregar formulário de login local e cadastro local via Floci/Cognito, condicionais ao env
- Automatizar o `.env.local` via `start_local.sh`

**Non-Goals:**
- Não criar backend endpoints novos
- Não implementar OAuth real (Google/GitHub) — os botões permanecem cosméticos até prod
- Não implementar refresh token ou logout remoto nesta change
- Não alterar banco de dados ou migrations

## Decisions

### D1 — Tokens como `theme.extend` no Tailwind

Adicionar ao `tailwind.config.js`:
```js
theme: {
  extend: {
    colors: { primary: '#8e8ea0' },
    borderRadius: { design: '5px' },
    transitionDuration: { design: '400ms' },
    transitionTimingFunction: { design: 'ease' },
  }
}
```
Isso permite usar `bg-primary`, `rounded-design`, `duration-design` em vez de valores arbitrários `bg-[#8e8ea0]`, garantindo consistência e refactorabilidade.

**Alternativa considerada**: CSS variables globais em `index.css`. Rejeitada porque o Tailwind JIT já faz a mesma função com melhor DX (autocomplete, purge).

### D2 — Módulo `src/api/cognito.ts` para chamadas Floci

Um módulo separado encapsula `initAuth(username, password)` e `signUp(username, email, password)`, chamando a URL configurada por `VITE_COGNITO_ENDPOINT` (padrão `http://localhost:4566`).

A API do Cognito usa `Content-Type: application/x-amz-json-1.1` com headers `X-Amz-Target`. O módulo isola esse protocolo do restante do app.

**Alternativa considerada**: Usar o AWS SDK JS v3 (`@aws-sdk/client-cognito-identity-provider`). Rejeitada pelo princípio Ponytail — são ~300kB extras para um `fetch` com 3 headers fixos.

### D3 — Tabs na LoginPage (Social | Local)

A `LoginPage` exibirá **tabs visuais** "Entrar" e "Cadastrar" quando `VITE_COGNITO_CLIENT_ID` estiver definido. Quando não estiver, exibe apenas os botões OAuth.

```
[sem VITE_COGNITO_CLIENT_ID]         [com VITE_COGNITO_CLIENT_ID]
─────────────────────────────        ─────────────────────────────
  [G] Google                           [G] Google
  [🐙] GitHub                          [🐙] GitHub
                                        ── local ──
                                       [Entrar] [Cadastrar]  ← tabs
                                        username / senha
```

**Alternativa considerada**: Página separada `/register`. Rejeitada — duplicaria o layout e aumentaria a complexidade de roteamento.

### D4 — `start_local.sh` escreve `.env.local` com `tee`

Após extrair `POOL_ID` e `CLIENT_ID` (já feito hoje), o script executa:
```bash
cat > "$(dirname "$0")/../../frontend/.env.local" <<EOF
VITE_COGNITO_ENDPOINT=http://localhost:4566
VITE_COGNITO_POOL_ID=${POOL_ID}
VITE_COGNITO_CLIENT_ID=${CLIENT_ID}
EOF
```
O `.env.local` é ignorado pelo git (já deve estar no `.gitignore` do frontend).

### D5 — `SignUp` sem código de confirmação (Floci auto-confirma)

O Floci auto-confirma usuários criados via `SignUp` sem enviar e-mail real (comportamento padrão do LocalStack). Após `SignUp`, o frontend imediatamente chama `InitiateAuth` para obter o `IdToken` e fazer login automático.

Em produção real, o Cognito enviaria e-mail de confirmação — mas isso está fora do escopo desta change.

## Risks / Trade-offs

- **[Risco] `.env.local` não existe na primeira execução do `npm run dev`** → O Vite simplesmente não injeta as vars e a seção local fica oculta — comportamento correto e seguro.
- **[Risco] IDs do Floci mudam entre restarts** → O `start_local.sh` sobrescreve `.env.local` a cada execução, então um `npm run dev` após o script sempre tem os valores corretos.
- **[Trade-off] Tabs na LoginPage adicionam estado local** → O componente gerencia `activeTab: 'login' | 'register'` com `useState` — trivial, sem Context nem reducer.

## Migration Plan

1. Atualizar `tailwind.config.js` com tokens do DESIGN.md
2. Refatorar `LoginPage`, `DashboardPage`, `WorkspacePage` com as novas classes
3. Criar `src/api/cognito.ts`
4. Adicionar tabs/forms de login e cadastro na `LoginPage`
5. Atualizar `start_local.sh` para escrever `.env.local`
6. Verificar `npm run build` sem erros
