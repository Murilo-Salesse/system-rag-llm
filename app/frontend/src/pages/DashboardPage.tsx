import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { PlusIcon, BookOpenIcon, LogOutIcon } from 'lucide-react'

interface Notebook {
  id: string
  name: string
  title?: string
  description?: string
  createdAt: string
}

export function DashboardPage() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [notebooks, setNotebooks] = useState<Notebook[]>([])
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [name, setName] = useState('')
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    api.get<any>('/api/v1/notebooks')
      .then(res => {
        // Se a resposta for uma página Spring (Page<T>), a lista está em res.content
        const list = Array.isArray(res) ? res : (Array.isArray(res?.content) ? res.content : [])
        setNotebooks(list)
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  async function handleCreate() {
    if (!name.trim()) return
    setCreating(true)
    try {
      const nb = await api.post<Notebook>('/api/v1/notebooks', { name: name.trim() })
      setNotebooks(prev => [nb, ...prev])
      setShowModal(false)
      setName('')
    } catch (e) {
      console.error('Erro ao criar notebook:', e)
      alert(`Falha ao criar notebook: ${e instanceof Error ? e.message : 'Erro desconhecido'}`)
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="min-h-screen bg-white">
      {/* Header — borda sutil, sem sombra */}
      <header className="border-b border-primary/20 px-8 py-4 flex items-center justify-between">
        <h1 className="text-[16px] font-semibold text-black" style={{ fontFamily: 'system-ui, sans-serif' }}>
          NotebookLM
        </h1>
        <button
          onClick={logout}
          className="flex items-center gap-2 text-sm text-primary hover:text-black transition-colors duration-design ease-design"
        >
          <LogOutIcon className="w-4 h-4" />
          Sair
        </button>
      </header>

      {/* Content */}
      <main className="max-w-5xl mx-auto px-8 py-8">
        <div className="flex items-center justify-between mb-8">
          <h2 className="text-[32px] font-semibold text-black leading-[1.5]" style={{ fontFamily: 'system-ui, sans-serif' }}>
            Seus Notebooks
          </h2>
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-design text-sm font-medium hover:bg-primary/90 transition-colors duration-design ease-design"
          >
            <PlusIcon className="w-4 h-4" />
            Novo Notebook
          </button>
        </div>

        {loading ? (
          <p className="text-primary text-sm">Carregando...</p>
        ) : notebooks.length === 0 ? (
          <div className="text-center py-24">
            <BookOpenIcon className="w-10 h-10 mx-auto mb-4 text-primary/40" />
            <p className="text-sm text-primary">Nenhum notebook ainda. Crie o primeiro!</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {notebooks.map(nb => (
              <button
                key={nb.id}
                onClick={() => navigate(`/notebooks/${nb.id}`)}
                className="text-left border border-primary/20 rounded-design p-6 hover:border-primary transition-colors duration-design ease-design bg-white"
              >
                <BookOpenIcon className="w-5 h-5 text-primary mb-4" />
                <p className="font-medium text-black text-sm truncate">{nb.name || nb.title}</p>
                <p className="text-xs text-primary mt-2">
                  {new Date(nb.createdAt).toLocaleDateString('pt-BR')}
                </p>
              </button>
            ))}
          </div>
        )}
      </main>

      {/* Modal criar notebook — sem sombra, borda sutil */}
      {showModal && (
        <div className="fixed inset-0 bg-black/20 flex items-center justify-center z-50">
          <div className="bg-white border border-primary/20 rounded-design w-full max-w-md p-8 space-y-6">
            <h3 className="text-[16px] font-semibold text-black">Novo Notebook</h3>
            <input
              autoFocus
              type="text"
              value={name}
              onChange={e => setName(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleCreate()}
              placeholder="Nome do notebook"
              className="w-full border border-primary/30 rounded-design px-3 py-2 text-sm text-black placeholder:text-primary/50 focus:outline-none focus:border-primary transition-colors duration-design ease-design"
            />
            <div className="flex justify-end gap-3">
              <button
                onClick={() => { setShowModal(false); setName('') }}
                className="px-4 py-2 text-sm text-primary hover:text-black transition-colors duration-design ease-design"
              >
                Cancelar
              </button>
              <button
                onClick={handleCreate}
                disabled={creating || !name.trim()}
                className="px-4 py-2 bg-primary text-white text-sm rounded-design hover:bg-primary/90 disabled:opacity-50 transition-colors duration-design ease-design"
              >
                {creating ? 'Criando...' : 'Criar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
