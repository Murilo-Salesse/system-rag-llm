#!/bin/sh
set -e

REGION="${AWS_REGION:-us-east-1}"
ENDPOINT="http://localhost:4566"
POOL_NAME="notebooklm-pool"
CLIENT_NAME="notebooklm-client"
ADMIN_USERNAME="admin"
ADMIN_EMAIL="admin@notebooklm.local"
ADMIN_PASSWORD="123"

echo "==> [Cognito Boot Hook] Initializing AWS Cognito in Floci..."

# 1. Verifica ou cria o User Pool
EXISTING_POOL_ID=$(aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp list-user-pools --max-results 20 --query "UserPools[?Name=='$POOL_NAME'].Id | [0]" --output text 2>/dev/null || true)

if [ -z "$EXISTING_POOL_ID" ] || [ "$EXISTING_POOL_ID" = "None" ]; then
    echo "Creating User Pool: $POOL_NAME"
    POOL_ID=$(aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp create-user-pool \
        --pool-name "$POOL_NAME" \
        --query "UserPool.Id" --output text)
    echo "User Pool created with ID: $POOL_ID"
else
    POOL_ID="$EXISTING_POOL_ID"
    echo "User Pool already exists with ID: $POOL_ID"
fi

# 2. Verifica ou cria o App Client
EXISTING_CLIENT_ID=$(aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp list-user-pool-clients --user-pool-id "$POOL_ID" --query "UserPoolClients[?ClientName=='$CLIENT_NAME'].ClientId | [0]" --output text 2>/dev/null || true)

if [ -z "$EXISTING_CLIENT_ID" ] || [ "$EXISTING_CLIENT_ID" = "None" ]; then
    echo "Creating User Pool Client: $CLIENT_NAME"
    CLIENT_ID=$(aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp create-user-pool-client \
        --user-pool-id "$POOL_ID" \
        --client-name "$CLIENT_NAME" \
        --explicit-auth-flows "ALLOW_ADMIN_USER_PASSWORD_AUTH" "ALLOW_USER_PASSWORD_AUTH" "ALLOW_REFRESH_TOKEN_AUTH" \
        --query "UserPoolClient.ClientId" --output text)
    echo "User Pool Client created with ID: $CLIENT_ID"
else
    CLIENT_ID="$EXISTING_CLIENT_ID"
    echo "User Pool Client already exists with ID: $CLIENT_ID"
fi

# 3. Cria o usuário admin se não existir
USER_EXISTS=$(aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp admin-get-user --user-pool-id "$POOL_ID" --username "$ADMIN_USERNAME" 2>/dev/null || true)

if [ -z "$USER_EXISTS" ]; then
    echo "Creating user: $ADMIN_USERNAME ($ADMIN_EMAIL)"
    aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp admin-create-user \
        --user-pool-id "$POOL_ID" \
        --username "$ADMIN_USERNAME" \
        --user-attributes Name=email,Value="$ADMIN_EMAIL" Name=email_verified,Value="true" \
        --message-action SUPPRESS >/dev/null

    echo "Setting permanent password for $ADMIN_USERNAME..."
    aws --region "$REGION" --endpoint-url "$ENDPOINT" cognito-idp admin-set-user-password \
        --user-pool-id "$POOL_ID" \
        --username "$ADMIN_USERNAME" \
        --password "$ADMIN_PASSWORD" \
        --permanent >/dev/null
    echo "User $ADMIN_USERNAME created and password set."
else
    echo "User $ADMIN_USERNAME already exists."
fi

# Salva arquivo de metadados em /app/data para persistência/consulta se montado
if [ -d "/app/data" ]; then
    cat <<EOF > /app/data/cognito.env
COGNITO_USER_POOL_ID=$POOL_ID
COGNITO_CLIENT_ID=$CLIENT_ID
COGNITO_JWK_SET_URI=http://localhost:4566/$POOL_ID/.well-known/jwks.json
EOF
fi

echo "==> [Cognito Boot Hook] Cognito setup complete. Pool ID: $POOL_ID, Client ID: $CLIENT_ID"
