## Why

O frontend MVP implementado em `create-frontend-mvp` usa o Tailwind "padrão" — azuis, bordas arredondadas grandes e sombras — em contraste com o `DESIGN.md` do projeto que define uma estética OpenAI: muted purple-gray (`#8e8ea0`), radius de 5px, zero sombras e sistema de 8px. Além disso, para desenvolver localmente, o único caminho era colar o Bearer token manualmente no `localStorage`, fricção desnecessária que atrasa o ciclo de desenvolvimento.

## What Changes

- **Repaginação visual completa** de todas as telas (`LoginPage`, `DashboardPage`, `WorkspacePage`) alinhada ao `DESIGN.md`: paleta `#8e8ea0`, radius `5px`, sem sombras, transições de 400ms, tipografia `system-ui`.
- **Revisão de layout** da `WorkspacePage`: sidebar de fontes mais larga e com melhor hierarquia visual; área de chat com espaçamento mais respirado.
- **Login local via Cognito** (`USER_PASSWORD_AUTH`): formulário de username/senha que chama diretamente o Floci na porta 4566, visível apenas quando `VITE_COGNITO_CLIENT_ID` está definido.
- **Cadastro local via Cognito** (`SignUp`): formulário de registro (username, e-mail, senha) que cria novos usuários no Floci — também condicional ao env.
- **`start_local.sh` atualizado**: grava `app/frontend/.env.local` com `VITE_COGNITO_POOL_ID`, `VITE_COGNITO_CLIENT_ID` e `VITE_COGNITO_ENDPOINT` após provisionar o Floci, eliminando a necessidade de copiar IDs manualmente.
- **Armazenamento do `IdToken`**: garantir explicitamente que o token armazenado é o `IdToken` (contém claims do usuário validadas pelo Spring Security via JWKS).

## Capabilities

### New Capabilities

- `frontend/visual-design-system`: Tokens de design codificados como utilitários Tailwind customizados (cor primária, radius, transições) para garantir consistência entre telas.
- `frontend/local-auth`: Autenticação e cadastro direto no Cognito/Floci via `InitiateAuth` e `SignUp`, condicionais à presença de variáveis de ambiente Vite.

### Modified Capabilities

- `auth-cognito`: Adiciona fluxo de autenticação local com username/senha (`USER_PASSWORD_AUTH`) e registro (`SignUp`) como extensão do suporte ao Cognito, restrito ao perfil de desenvolvimento local.

## Impact

- **`app/frontend/src/`**: Refactor visual de todos os componentes existentes; novos componentes `LocalAuthSection.tsx` e `RegisterPage.tsx` (ou tabs na `LoginPage`).
- **`app/frontend/tailwind.config.js`**: Adicionar tokens do `DESIGN.md` como `theme.extend` (cores, radius, transição).
- **`app/frontend/src/api/cognito.ts`**: Novo módulo para chamadas diretas ao Cognito/Floci (`InitiateAuth`, `SignUp`, `ConfirmSignUp`).
- **`app/backend-api/local/start_local.sh`**: Escrita de `.env.local` com IDs do Cognito após provisionamento.
- **Sem impacto no backend**: Nenhuma rota nova, nenhuma entidade nova. O JWT emitido pelo Floci é validado da mesma forma.
- **Sem impacto em banco de dados**: Nenhuma migration necessária.
- **Latência**: Zero impacto — chamadas ao Floci são locais (localhost:4566).
- **AWS Costs**: Zero impacto — mudança restrita ao frontend e ao ambiente local.
- **Statelessness**: Mantida — o frontend ainda envia `Authorization: Bearer <IdToken>` em todas as chamadas ao backend.
