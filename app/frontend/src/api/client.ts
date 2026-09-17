const TOKEN_KEY = 'auth_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

function authHeaders(): Record<string, string> {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
      ...(init.headers as Record<string, string> | undefined),
    },
  })
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText)
    throw new Error(`${res.status}: ${text}`)
  }
  // 204 No Content
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),

  /** SSE streaming via fetch (POST with Bearer token) */
  stream: (path: string, body: unknown, onToken: (t: string) => void, onDone: () => void) => {
    const controller = new AbortController()
    fetch(path, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...authHeaders(),
      },
      body: JSON.stringify(body),
      signal: controller.signal,
    }).then(async (res) => {
      if (!res.ok || !res.body) {
        onDone()
        return
      }
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split(/\r?\n/)
        buffer = lines.pop() ?? ''
        for (const rawLine of lines) {
          const line = rawLine.trim()
          if (!line) continue
          if (line.startsWith('event: done') || line === 'event:done') {
            onDone()
            return
          }
          if (line.startsWith('data:')) {
            const dataStr = line.slice(line.indexOf(':') + 1).trim()
            try {
              const payload = JSON.parse(dataStr)
              if (payload.token) {
                onToken(payload.token)
              }
            } catch {
              // se vier como texto puro
              if (dataStr) onToken(dataStr)
            }
          }
        }
      }
      onDone()
    }).catch(() => onDone())
    return () => controller.abort()
  },
}
