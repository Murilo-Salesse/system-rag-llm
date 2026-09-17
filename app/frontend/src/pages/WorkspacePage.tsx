import { useState, useEffect, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import {
  ArrowLeftIcon,
  UploadIcon,
  LinkIcon,
  SendIcon,
  Loader2Icon,
  FileTextIcon,
  CheckIcon,
} from 'lucide-react'

interface Source {
  id: string
  name: string
  status: 'PENDING' | 'PROCESSING' | 'DONE' | 'ERROR'
}

interface Message {
  id: string
  role: 'USER' | 'ASSISTANT'
  content: string
}

interface Conversation {
  id: string
  title: string
}

const STATUS_LABEL: Record<Source['status'], { label: string; color: string }> = {
  DONE:       { label: '',            color: 'text-primary/40' },
  PENDING:    { label: 'pendente',    color: 'text-yellow-500' },
  PROCESSING: { label: 'processando', color: 'text-primary' },
  ERROR:      { label: 'erro',        color: 'text-red-500' },
}

export function WorkspacePage() {
  const { id: notebookId } = useParams<{ id: string }>()
  const navigate = useNavigate()

  // Sources
  const [sources, setSources] = useState<Source[]>([])
  const [activeSources, setActiveSources] = useState<Set<string>>(new Set())
  const [urlInput, setUrlInput] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  // Conversation
  const [conversation, setConversation] = useState<Conversation | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const stopStreamRef = useRef<(() => void) | null>(null)

  // Load sources
  useEffect(() => {
    if (!notebookId) return
    api.get<Source[]>(`/api/v1/notebooks/${notebookId}/sources`)
      .then(list => {
        if (Array.isArray(list)) {
          setSources(list)
          setActiveSources(new Set(list.filter(s => s.status === 'DONE').map(s => s.id)))
        }
      })
      .catch(err => {
        console.warn('Sources ainda não configuradas no backend:', err)
        setSources([])
      })
  }, [notebookId])

  // Load or create default conversation
  useEffect(() => {
    if (!notebookId) return
    api.get<Conversation[]>(`/api/v1/notebooks/${notebookId}/conversations`)
      .then(async list => {
        if (list.length > 0) {
          setConversation(list[0])
          const msgs = await api.get<Message[]>(
            `/api/v1/notebooks/${notebookId}/conversations/${list[0].id}/messages`
          )
          setMessages(msgs)
        } else {
          const conv = await api.post<Conversation>(
            `/api/v1/notebooks/${notebookId}/conversations`,
            { title: 'Conversa 1' }
          )
          setConversation(conv)
        }
      })
      .catch(console.error)
  }, [notebookId])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  function toggleSource(id: string) {
    setActiveSources(prev => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  }


  async function handleFileUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file || !notebookId) return
    const form = new FormData()
    form.append('file', file)
    try {
      const src = await fetch(`/api/v1/notebooks/${notebookId}/sources/upload`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${localStorage.getItem('auth_token') ?? ''}` },
        body: form,
      }).then(r => r.json()) as Source
      setSources(prev => [...prev, src])
    } catch (e) {
      console.error(e)
    }
    e.target.value = ''
  }

  async function handleUrlAdd() {
    if (!urlInput.trim() || !notebookId) return
    try {
      const src = await api.post<Source>(`/api/v1/notebooks/${notebookId}/sources/url`, {
        url: urlInput.trim(),
      })
      setSources(prev => [...prev, src])
      setUrlInput('')
    } catch (e) {
      console.error(e)
    }
  }

  const handleSend = useCallback(async () => {
    if (!input.trim() || streaming || !conversation || !notebookId) return

    const userMsg: Message = { id: crypto.randomUUID(), role: 'USER', content: input.trim() }
    setMessages(prev => [...prev, userMsg])
    setInput('')

    const assistantId = crypto.randomUUID()
    setMessages(prev => [...prev, { id: assistantId, role: 'ASSISTANT', content: '' }])
    setStreaming(true)

    const stop = api.stream(
      `/api/v1/notebooks/${notebookId}/conversations/${conversation.id}/messages/stream`,
      { content: userMsg.content, activeSourceIds: Array.from(activeSources) },
      (token) => {
        setMessages(prev =>
          prev.map(m => m.id === assistantId ? { ...m, content: m.content + token } : m)
        )
      },
      () => setStreaming(false)
    )
    stopStreamRef.current = stop
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [input, streaming, conversation, notebookId, activeSources])

  return (
    <div className="h-screen flex flex-col bg-white" style={{ fontFamily: 'system-ui, sans-serif' }}>
      {/* Top bar */}
      <header className="flex items-center gap-4 px-6 py-4 border-b border-primary/20">
        <button
          onClick={() => navigate('/')}
          className="text-primary hover:text-black transition-colors duration-design ease-design"
        >
          <ArrowLeftIcon className="w-4 h-4" />
        </button>
        <span className="text-sm font-medium text-black">Notebook</span>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* ── Left panel — Sources (w-72 = 288px) ── */}
        <aside className="w-72 shrink-0 border-r border-primary/20 flex flex-col bg-white">
          {/* Cabeçalho da sidebar */}
          <div className="px-6 py-4 border-b border-primary/10">
            <p className="text-xs font-semibold text-primary uppercase tracking-widest">
              Fontes
            </p>
          </div>

          {/* Lista de fontes */}
          <div className="flex-1 overflow-y-auto py-2">
            {sources.length === 0 && (
              <p className="text-xs text-primary/50 text-center mt-8 px-6">
                Nenhuma fonte adicionada
              </p>
            )}
            {sources.map(src => {
              const { label, color } = STATUS_LABEL[src.status]
              const active = activeSources.has(src.id)
              return (
                <label
                  key={src.id}
                  className="flex items-center gap-3 px-6 py-3 hover:bg-primary/5 cursor-pointer transition-colors duration-design ease-design"
                >
                  {/* Checkbox customizado */}
                  <span
                    className={`w-4 h-4 shrink-0 border rounded-design flex items-center justify-center transition-colors duration-design ease-design ${
                      active && src.status === 'DONE'
                        ? 'bg-primary border-primary'
                        : 'border-primary/30 bg-white'
                    }`}
                    onClick={() => src.status === 'DONE' && toggleSource(src.id)}
                  >
                    {active && src.status === 'DONE' && (
                      <CheckIcon className="w-2.5 h-2.5 text-white" strokeWidth={3} />
                    )}
                  </span>
                  <FileTextIcon className="w-4 h-4 text-primary/50 shrink-0" />
                  <span className="text-sm text-black truncate flex-1 leading-[1.5]">
                    {src.name}
                  </span>
                  {label && (
                    <span className={`text-xs shrink-0 ${color}`}>{label}</span>
                  )}
                </label>
              )
            })}
          </div>

          {/* Ações de adicionar fonte */}
          <div className="px-6 py-4 border-t border-primary/10 space-y-3">
            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              onChange={handleFileUpload}
              accept=".md,.markdown,.docx"
            />
            <button
              onClick={() => fileInputRef.current?.click()}
              className="w-full flex items-center gap-2 px-3 py-2 text-xs border border-primary/30 rounded-design hover:bg-primary/5 text-primary transition-colors duration-design ease-design"
            >
              <UploadIcon className="w-3.5 h-3.5 shrink-0" />
              Upload arquivo (.md, .docx)
            </button>

            <div className="flex gap-2">
              <input
                type="url"
                value={urlInput}
                onChange={e => setUrlInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleUrlAdd()}
                placeholder="https://..."
                className="flex-1 text-xs border border-primary/30 rounded-design px-3 py-2 text-black placeholder:text-primary/40 focus:outline-none focus:border-primary transition-colors duration-design ease-design min-w-0"
              />
              <button
                onClick={handleUrlAdd}
                className="px-2.5 border border-primary/30 rounded-design hover:bg-primary/5 transition-colors duration-design ease-design"
              >
                <LinkIcon className="w-3.5 h-3.5 text-primary" />
              </button>
            </div>
          </div>
        </aside>

        {/* ── Right panel — Chat ── */}
        <main className="flex-1 flex flex-col overflow-hidden">
          {/* Messages */}
          <div className="flex-1 overflow-y-auto px-8 py-6 space-y-4">
            {messages.length === 0 && (
              <div className="h-full flex items-center justify-center">
                <p className="text-sm text-primary/60 text-center max-w-xs leading-[1.5]">
                  Selecione fontes no painel à esquerda e comece a conversar.
                </p>
              </div>
            )}
            {messages.map(msg => (
              <div
                key={msg.id}
                className={`flex ${msg.role === 'USER' ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[72%] rounded-design px-4 py-3 text-sm leading-[1.5] whitespace-pre-wrap ${
                    msg.role === 'USER'
                      ? 'bg-primary text-white'
                      : 'border border-primary/20 text-black bg-white'
                  }`}
                >
                  {msg.content}
                  {msg.role === 'ASSISTANT' && msg.content === '' && streaming && (
                    <Loader2Icon className="w-4 h-4 animate-spin text-primary/50 mt-1" />
                  )}
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} />
          </div>

          {/* Input area */}
          <div className="px-8 py-6 border-t border-primary/20 bg-white">
            <div className="flex gap-3 items-end">
              <textarea
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault()
                    handleSend()
                  }
                }}
                placeholder="Pergunte algo sobre suas fontes..."
                rows={1}
                className="flex-1 resize-none border border-primary/30 rounded-design px-4 py-3 text-sm text-black placeholder:text-primary/50 focus:outline-none focus:border-primary transition-colors duration-design ease-design leading-[1.5] min-h-[44px] max-h-32"
              />
              <button
                onClick={handleSend}
                disabled={!input.trim() || streaming}
                className="p-3 bg-primary text-white rounded-design hover:bg-primary/90 disabled:opacity-40 transition-colors duration-design ease-design"
              >
                {streaming ? (
                  <Loader2Icon className="w-4 h-4 animate-spin" />
                ) : (
                  <SendIcon className="w-4 h-4" />
                )}
              </button>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
