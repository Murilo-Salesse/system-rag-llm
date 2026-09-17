## 1. Design Tokens no Tailwind

- [x] 1.1 Atualizar `tailwind.config.js` com `theme.extend` para `colors.primary = '#8e8ea0'`, `borderRadius.design = '5px'`, `transitionDuration.design = '400ms'` e `transitionTimingFunction.design = 'ease'`; verificar que `npm run build` compila sem erros

## 2. Refatoração Visual — LoginPage

- [x] 2.1 Reescrever `LoginPage.tsx`: fundo branco, título 48px `font-bold system-ui`, subtítulo em `text-primary`, botões OAuth com borda `border-primary/30`, `rounded-design`, `duration-design`; verificar que o componente renderiza sem erros no browser

## 3. Refatoração Visual — DashboardPage

- [x] 3.1 Reescrever `DashboardPage.tsx`: header com borda inferior sutil (sem sombra), botão "Novo Notebook" com `bg-primary text-white rounded-design`, cards de notebook com `border-gray-200 rounded-design hover:border-primary` sem `shadow`; verificar renderização no browser

## 4. Refatoração Visual + Layout — WorkspacePage

- [x] 4.1 Aumentar sidebar de fontes para `w-72` (288px), aplicar paleta de cores do `DESIGN.md`, remover todos os `rounded-xl`/`rounded-lg` e substituir por `rounded-design`, eliminar sombras; verificar layout em viewport ≥ 768px
- [x] 4.2 Refatorar área de chat: espaçamento `py-6 px-6` entre mensagens, `line-height: 1.5` em texto, input fixado no rodapé com `py-4 px-6`, botão de envio com `bg-primary rounded-design`; verificar scroll e streaming funcionais

## 5. Módulo Cognito Local

- [x] 5.1 Criar `src/api/cognito.ts` com funções `initiateAuth(username, password)` e `signUp(username, email, password)` usando `fetch` + `Content-Type: application/x-amz-json-1.1` + `X-Amz-Target` corretos para a API Cognito; verificar que TypeScript compila sem erros

## 6. Login e Cadastro Local na LoginPage

- [x] 6.1 Adicionar seção condicional na `LoginPage` (apenas quando `import.meta.env.VITE_COGNITO_CLIENT_ID` está definido): divisor "ambiente local", tabs `[Entrar]` e `[Cadastrar]`, formulário de login (username + senha) chamando `initiateAuth` e armazenando `IdToken`; verificar login bem-sucedido com `admin/123` no Floci
- [x] 6.2 Implementar tab de cadastro: formulário com username, e-mail e senha chamando `signUp` seguido de `initiateAuth` automático; verificar criação de novo usuário no Floci e login automático pós-cadastro
- [x] 6.3 Adicionar tratamento de erro em ambos os formulários: exibir mensagem de erro abaixo do form quando Floci retorna falha; verificar que formulário não redireciona em caso de erro

## 7. Atualização do start_local.sh

- [x] 7.1 Adicionar ao final de `start_local.sh` a escrita de `app/frontend/.env.local` com `VITE_COGNITO_ENDPOINT`, `VITE_COGNITO_POOL_ID` e `VITE_COGNITO_CLIENT_ID`; verificar que o arquivo é criado/sobrescrito após `bash start_local.sh`

## 8. Verificação Final

- [x] 8.1 Executar `npm run build` em `app/frontend/` e verificar que todos os assets compilam sem erros de TypeScript ou bundle
