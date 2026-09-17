#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
REGION="${AWS_REGION:-us-east-1}"
ENDPOINT="http://localhost:4566"
POOL_NAME="notebooklm-pool"
CLIENT_NAME="notebooklm-client"
ADMIN_USERNAME="admin"
ADMIN_PASSWORD="123"

echo "================================================================"
echo "    Iniciando Ambiente Local (PostgreSQL + S3/Cognito no Floci) "
echo "================================================================"

# 1. Sobe os containers via Docker Compose
docker compose -f "$COMPOSE_FILE" up -d

echo "Aguardando serviços inicializarem..."

# Aguarda PostgreSQL responder
until docker compose -f "$COMPOSE_FILE" exec -T postgresql pg_isready -U postgres -d notebooklm >/dev/null 2>&1; do
    printf "."
    sleep 1
done
echo -e "\n✓ PostgreSQL pronto!"

# Aguarda Floci responder na porta 4566
MAX_RETRIES=30
RETRY_COUNT=0
until curl -s "$ENDPOINT" >/dev/null 2>&1 || [ "$RETRY_COUNT" -ge "$MAX_RETRIES" ]; do
    printf "."
    sleep 1
    RETRY_COUNT=$((RETRY_COUNT + 1))
done

if [ "$RETRY_COUNT" -ge "$MAX_RETRIES" ]; then
    echo -e "\n✗ Timeout aguardando o Floci responder na porta 4566."
    exit 1
fi
echo -e "\n✓ Floci pronto!"

# Dá tempo para os scripts de boot /etc/floci/init/boot executarem
echo "Aguardando execução dos hooks de inicialização do Floci..."
sleep 3

# 2. Obtém os IDs do User Pool e do App Client
POOL_ID=$(docker compose -f "$COMPOSE_FILE" exec -T floci aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp list-user-pools --max-results 10 --query "UserPools[?Name=='$POOL_NAME'].Id | [0]" --output text 2>/dev/null || true)
CLIENT_ID=$(docker compose -f "$COMPOSE_FILE" exec -T floci aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp list-user-pool-clients --user-pool-id "$POOL_ID" --query "UserPoolClients[?ClientName=='$CLIENT_NAME'].ClientId | [0]" --output text 2>/dev/null || true)

if [ -z "$POOL_ID" ] || [ "$POOL_ID" = "None" ]; then
    echo "Executando inicialização do Cognito manualmente caso o hook ainda não tenha disparado..."
    docker compose -f "$COMPOSE_FILE" exec -T floci /bin/sh /etc/floci/init/boot/init-cognito.sh
    POOL_ID=$(docker compose -f "$COMPOSE_FILE" exec -T floci aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp list-user-pools --max-results 10 --query "UserPools[?Name=='$POOL_NAME'].Id | [0]" --output text 2>/dev/null || true)
    CLIENT_ID=$(docker compose -f "$COMPOSE_FILE" exec -T floci aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp list-user-pool-clients --user-pool-id "$POOL_ID" --query "UserPoolClients[?ClientName=='$CLIENT_NAME'].ClientId | [0]" --output text 2>/dev/null || true)
fi

echo "================================================================"
echo "✓ User Pool ID: $POOL_ID"
echo "✓ Client ID:    $CLIENT_ID"
echo "✓ JWKS URI:     $ENDPOINT/$POOL_ID/.well-known/jwks.json"
echo "================================================================"

# 3. Autentica o usuário admin para gerar o token JWT de teste
echo "Autenticando usuário '$ADMIN_USERNAME' para obter token JWT..."

AUTH_OUTPUT=$(docker compose -f "$COMPOSE_FILE" exec -T floci aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp admin-initiate-auth \
    --user-pool-id "$POOL_ID" \
    --client-id "$CLIENT_ID" \
    --auth-flow ADMIN_NO_SRP_AUTH \
    --auth-parameters USERNAME="$ADMIN_USERNAME",PASSWORD="$ADMIN_PASSWORD" 2>/dev/null || true)

ID_TOKEN=$(echo "$AUTH_OUTPUT" | grep -o '"IdToken": *"[^"]*"' | sed 's/"IdToken": *"//;s/"//' || true)
ACCESS_TOKEN=$(echo "$AUTH_OUTPUT" | grep -o '"AccessToken": *"[^"]*"' | sed 's/"AccessToken": *"//;s/"//' || true)

echo "================================================================"
echo "    Credenciais e Tokens de Teste (Admin)"
echo "================================================================"
echo "Usuário: $ADMIN_USERNAME"
echo "Senha:   $ADMIN_PASSWORD"
echo ""
if [ -n "$ID_TOKEN" ]; then
    echo "ID_TOKEN (Use como Bearer no header Authorization):"
    echo "Bearer $ID_TOKEN"
    echo ""
    echo "ACCESS_TOKEN:"
    echo "Bearer $ACCESS_TOKEN"
else
    echo "Nota: Não foi possível obter o token automaticamente via CLI."
    echo "Detalhes do Auth Output:"
    echo "$AUTH_OUTPUT"
fi
echo "================================================================"
echo "Ambiente local pronto!"

# 4. Grava app/frontend/.env.local com os IDs do Cognito
FRONTEND_ENV="$SCRIPT_DIR/../../frontend/.env.local"
cat > "$FRONTEND_ENV" <<EOF
VITE_COGNITO_ENDPOINT=/_cognito
VITE_COGNITO_POOL_ID=${POOL_ID}
VITE_COGNITO_CLIENT_ID=${CLIENT_ID}
EOF
echo "✓ app/frontend/.env.local atualizado com IDs do Cognito."
echo "  Reinicie o 'npm run dev' se já estiver rodando."
echo "================================================================"
