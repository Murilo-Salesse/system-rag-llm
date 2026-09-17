#!/usr/bin/env bash
# login.sh — Autentica o usuário no Cognito local (Floci) e emite tokens JWT
set -euo pipefail

ENDPOINT="${AWS_ENDPOINT_URL:-http://localhost:4566}"
REGION="${AWS_REGION:-us-east-1}"
USERNAME="${1:-admin}"
PASSWORD="${2:-123}"

echo "==> [login.sh] Consultando Cognito local em $ENDPOINT..."

# 1. Obtém dinamicamente o User Pool ID e o Client ID se não definidos
POOL_ID="${COGNITO_USER_POOL_ID:-$(docker exec notebooklm-floci aws --region "$REGION" --endpoint-url http://localhost:4566 cognito-idp list-user-pools --max-results 10 --query "UserPools[?Name=='notebooklm-pool'].Id | [0]" --output text 2>/dev/null || true)}"

if [[ -z "$POOL_ID" || "$POOL_ID" == "None" ]]; then
  echo "❌ User Pool 'notebooklm-pool' não encontrado no Floci."
  echo "   Execute 'docker exec notebooklm-floci sh /etc/floci/init/boot/init-cognito.sh' para inicializar."
  exit 1
fi

CLIENT_ID="${COGNITO_CLIENT_ID:-$(docker exec notebooklm-floci aws --region "$REGION" --endpoint-url http://localhost:4566 cognito-idp list-user-pool-clients --user-pool-id "$POOL_ID" --query "UserPoolClients[?ClientName=='notebooklm-client'].ClientId | [0]" --output text 2>/dev/null || true)}"

if [[ -z "$CLIENT_ID" || "$CLIENT_ID" == "None" ]]; then
  echo "❌ Client ID para 'notebooklm-client' não encontrado no User Pool $POOL_ID."
  exit 1
fi

echo "==> Autenticando usuário '$USERNAME' (Pool: $POOL_ID, Client: $CLIENT_ID)..."

# 2. Executa initiate-auth via AWS CLI dentro do container ou localmente
AUTH_JSON=$(docker exec notebooklm-floci aws --region "$REGION" --endpoint-url http://localhost:4566 cognito-idp initiate-auth \
  --client-id "$CLIENT_ID" \
  --auth-flow USER_PASSWORD_AUTH \
  --auth-parameters USERNAME="$USERNAME",PASSWORD="$PASSWORD" 2>/dev/null || true)

if [[ -z "$AUTH_JSON" ]]; then
  echo "❌ Falha na autenticação. Verifique se o usuário e senha estão corretos."
  exit 1
fi

# 3. Extrai os tokens
ACCESS_TOKEN=$(echo "$AUTH_JSON" | python3 -c 'import sys, json; print(json.load(sys.stdin)["AuthenticationResult"]["AccessToken"])')
ID_TOKEN=$(echo "$AUTH_JSON" | python3 -c 'import sys, json; print(json.load(sys.stdin)["AuthenticationResult"]["IdToken"])')

echo ""
echo "✅ Login realizado com sucesso!"
echo ""
echo "━━━ User Pool & JWKS Info ━━━"
echo "Pool ID:      $POOL_ID"
echo "Client ID:    $CLIENT_ID"
echo "JWKS URI:     $ENDPOINT/$POOL_ID/.well-known/jwks.json"
echo ""
echo "━━━ ID Token (Use para autenticar no backend com claims de usuário) ━━━"
echo "$ID_TOKEN"
echo ""
echo "━━━ Access Token ━━━"
echo "$ACCESS_TOKEN"
echo ""
echo "━━━ Exemplo de Uso com cURL ━━━"
echo "curl -H \"Authorization: Bearer $ID_TOKEN\" http://localhost:8080/api/v1/notebooks"
