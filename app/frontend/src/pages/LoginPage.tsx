import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { initiateAuth, signUp, confirmUser } from '../api/cognito'

const CLIENT_ID = import.meta.env.VITE_COGNITO_CLIENT_ID as string | undefined
const hasLocal = !!CLIENT_ID

type LocalTab = 'login' | 'register'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [tab, setTab] = useState<LocalTab>('login')

  // login form
  const [loginUsername, setLoginUsername] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [loginError, setLoginError] = useState('')
  const [loginLoading, setLoginLoading] = useState(false)

  // register form
  const [regUsername, setRegUsername] = useState('')
  const [regEmail, setRegEmail] = useState('')
  const [regPassword, setRegPassword] = useState('')
  const [regError, setRegError] = useState('')
  const [regLoading, setRegLoading] = useState(false)

  function handleOAuth(provider: 'google' | 'github') {
    alert(
      `OAuth via ${provider} não disponível no ambiente local.\n` +
      `Obtanha o token com start_local.sh ou use o formulário abaixo.`
    )
  }

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault()
    setLoginError('')
    setLoginLoading(true)
    try {
      const idToken = await initiateAuth(loginUsername, loginPassword)
      login(idToken)
      navigate('/', { replace: true })
    } catch (err) {
      setLoginError(err instanceof Error ? err.message : 'Erro ao autenticar')
    } finally {
      setLoginLoading(false)
    }
  }

  async function handleRegister(e: React.FormEvent) {
    e.preventDefault()
    setRegError('')
    setRegLoading(true)
    try {
      console.log('==> [Register] Chamando signUp para:', regUsername)
      const signUpRes = await signUp(regUsername, regEmail, regPassword)
      console.log('==> [Register] signUp retornou:', signUpRes)

      if (!signUpRes.UserConfirmed) {
        console.log('==> [Register] Usuário pendente de confirmação. Auto-confirmando no Floci...')
        await confirmUser(regUsername)
        console.log('==> [Register] Usuário confirmado!')
      }

      console.log('==> [Register] Autenticando com initiateAuth...')
      const idToken = await initiateAuth(regUsername, regPassword)
      console.log('==> [Register] Autenticado com sucesso!')
      login(idToken)
      navigate('/', { replace: true })
    } catch (err) {
      console.error('==> [Register] Erro capturado:', err)
      setRegError(err instanceof Error ? err.message : 'Erro ao cadastrar')
    } finally {
      setRegLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-white">
      <div className="w-full max-w-sm space-y-8">
        {/* Logo / título */}
        <div className="text-center">
          <h1 className="text-[48px] font-bold leading-[1.5] text-black" style={{ fontFamily: 'system-ui, sans-serif' }}>
            NotebookLM
          </h1>
          <p className="mt-2 text-[16px] text-primary leading-[1.5]">
            Seu assistente de pesquisa com IA
          </p>
        </div>

        {/* Botões OAuth */}
        <div className="space-y-3">
          <button
            onClick={() => handleOAuth('google')}
            className="w-full flex items-center justify-center gap-3 px-4 py-3 border border-primary/30 rounded-design bg-white hover:bg-gray-50 transition-colors duration-design ease-design text-[16px] font-medium text-black"
          >
            <svg className="w-5 h-5 shrink-0" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            Entrar com Google
          </button>

          <button
            onClick={() => handleOAuth('github')}
            className="w-full flex items-center justify-center gap-3 px-4 py-3 border border-primary/30 rounded-design bg-white hover:bg-gray-50 transition-colors duration-design ease-design text-[16px] font-medium text-black"
          >
            <svg className="w-5 h-5 shrink-0" fill="currentColor" viewBox="0 0 24 24">
              <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.745 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
            </svg>
            Entrar com GitHub
          </button>
        </div>

        {/* Seção local — condicional ao env */}
        {hasLocal && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="flex-1 h-px bg-primary/20" />
              <span className="text-xs text-primary">ambiente local</span>
              <div className="flex-1 h-px bg-primary/20" />
            </div>

            {/* Tabs */}
            <div className="flex rounded-design border border-primary/20 overflow-hidden">
              <button
                onClick={() => setTab('login')}
                className={`flex-1 py-2 text-sm font-medium transition-colors duration-design ease-design ${
                  tab === 'login'
                    ? 'bg-primary text-white'
                    : 'bg-white text-primary hover:bg-primary/5'
                }`}
              >
                Entrar
              </button>
              <button
                onClick={() => setTab('register')}
                className={`flex-1 py-2 text-sm font-medium transition-colors duration-design ease-design ${
                  tab === 'register'
                    ? 'bg-primary text-white'
                    : 'bg-white text-primary hover:bg-primary/5'
                }`}
              >
                Cadastrar
              </button>
            </div>

            {/* Form de login */}
            {tab === 'login' && (
              <form onSubmit={handleLogin} className="space-y-3">
                <input
                  type="text"
                  value={loginUsername}
                  onChange={e => setLoginUsername(e.target.value)}
                  placeholder="Usuário"
                  autoComplete="username"
                  required
                  className="w-full border border-primary/30 rounded-design px-3 py-2 text-sm text-black placeholder:text-primary/50 focus:outline-none focus:border-primary transition-colors duration-design ease-design"
                />
                <input
                  type="password"
                  value={loginPassword}
                  onChange={e => setLoginPassword(e.target.value)}
                  placeholder="Senha"
                  autoComplete="current-password"
                  required
                  className="w-full border border-primary/30 rounded-design px-3 py-2 text-sm text-black placeholder:text-primary/50 focus:outline-none focus:border-primary transition-colors duration-design ease-design"
                />
                {loginError && (
                  <p className="text-xs text-red-600">{loginError}</p>
                )}
                <button
                  type="submit"
                  disabled={loginLoading}
                  className="w-full py-2.5 bg-primary text-white text-sm font-medium rounded-design hover:bg-primary/90 disabled:opacity-50 transition-colors duration-design ease-design"
                >
                  {loginLoading ? 'Entrando...' : 'Entrar'}
                </button>
              </form>
            )}

            {/* Form de cadastro */}
            {tab === 'register' && (
              <form onSubmit={handleRegister} className="space-y-3">
                <input
                  type="text"
                  value={regUsername}
                  onChange={e => setRegUsername(e.target.value)}
                  placeholder="Usuário"
                  autoComplete="username"
                  required
                  className="w-full border border-primary/30 rounded-design px-3 py-2 text-sm text-black placeholder:text-primary/50 focus:outline-none focus:border-primary transition-colors duration-design ease-design"
                />
                <input
                  type="email"
                  value={regEmail}
                  onChange={e => setRegEmail(e.target.value)}
                  placeholder="E-mail"
                  autoComplete="email"
                  required
                  className="w-full border border-primary/30 rounded-design px-3 py-2 text-sm text-black placeholder:text-primary/50 focus:outline-none focus:border-primary transition-colors duration-design ease-design"
                />
                <input
                  type="password"
                  value={regPassword}
                  onChange={e => setRegPassword(e.target.value)}
                  placeholder="Senha"
                  autoComplete="new-password"
                  required
                  className="w-full border border-primary/30 rounded-design px-3 py-2 text-sm text-black placeholder:text-primary/50 focus:outline-none focus:border-primary transition-colors duration-design ease-design"
                />
                {regError && (
                  <p className="text-xs text-red-600">{regError}</p>
                )}
                <button
                  type="submit"
                  disabled={regLoading}
                  className="w-full py-2.5 bg-primary text-white text-sm font-medium rounded-design hover:bg-primary/90 disabled:opacity-50 transition-colors duration-design ease-design"
                >
                  {regLoading ? 'Cadastrando...' : 'Cadastrar'}
                </button>
              </form>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
