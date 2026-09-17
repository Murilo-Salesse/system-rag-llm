const ENDPOINT = import.meta.env.VITE_COGNITO_ENDPOINT || '/_cognito'
const CLIENT_ID = import.meta.env.VITE_COGNITO_CLIENT_ID as string

async function cognitoPost<T>(target: string, body: Record<string, unknown>): Promise<T> {
  console.log(`[Cognito] POST target: ${target}`, body)
  const res = await fetch(ENDPOINT, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-amz-json-1.1',
      'X-Amz-Target': target,
    },
    body: JSON.stringify(body),
  })
  const data = await res.json()
  console.log(`[Cognito] Response status ${res.status}:`, data)
  if (!res.ok) {
    throw new Error(data.message ?? data.__type ?? `Cognito error ${res.status}`)
  }
  return data as T
}

interface InitiateAuthResponse {
  AuthenticationResult: {
    IdToken: string
    AccessToken: string
    RefreshToken: string
  }
}

/** Autenticar com username + password. Retorna o IdToken. */
export async function initiateAuth(username: string, password: string): Promise<string> {
  const data = await cognitoPost<InitiateAuthResponse>(
    'AWSCognitoIdentityProviderService.InitiateAuth',
    {
      AuthFlow: 'USER_PASSWORD_AUTH',
      ClientId: CLIENT_ID,
      AuthParameters: {
        USERNAME: username,
        PASSWORD: password,
      },
    }
  )
  return data.AuthenticationResult.IdToken
}

interface SignUpResponse {
  UserConfirmed: boolean
  UserSub: string
}

const POOL_ID = import.meta.env.VITE_COGNITO_POOL_ID as string

/** Auto-confirma o usuário no Floci local */
export async function confirmUser(username: string): Promise<void> {
  await cognitoPost(
    'AWSCognitoIdentityProviderService.AdminConfirmSignUp',
    {
      UserPoolId: POOL_ID,
      Username: username,
    }
  )
}

/** Cadastrar novo usuário no Cognito/Floci. */
export async function signUp(username: string, email: string, password: string): Promise<SignUpResponse> {
  return cognitoPost<SignUpResponse>(
    'AWSCognitoIdentityProviderService.SignUp',
    {
      ClientId: CLIENT_ID,
      Username: username,
      Password: password,
      UserAttributes: [
        { Name: 'email', Value: email },
      ],
    }
  )
}
