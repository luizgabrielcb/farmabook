import {
  createContext,
  useContext,
  useState,
  useRef,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react'
import { createPortal } from 'react-dom'
import { validatePin } from '@/api/auth'
import { setActivePin } from '@/lib/api'
import { pinState } from '@/lib/pinState'

interface PinContextValue {
  requestPin: () => Promise<void>
}

const PinContext = createContext<PinContextValue | null>(null)

interface Pending {
  resolve: () => void
  reject: () => void
}

const PIN_PORTAL_ID = 'pin-portal'

export function PinProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)
  const [pin, setPin] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const pendingRef = useRef<Pending | null>(null)
  const inputRef = useRef<HTMLInputElement | null>(null)

  const containerRef = useRef<HTMLDivElement | null>(null)
  if (!containerRef.current) {
    const el = document.createElement('div')
    el.id = PIN_PORTAL_ID
    containerRef.current = el
  }

  useEffect(() => {
    const el = containerRef.current!
    document.body.appendChild(el)
    return () => { if (document.body.contains(el)) document.body.removeChild(el) }
  }, [])

  useEffect(() => { pinState.open = open }, [open])

  // inert em todos os filhos do body exceto o container do PIN; foca o input
  // do PIN para abrir o teclado do dispositivo
  useEffect(() => {
    if (!open) return
    const siblings = Array.from(document.body.children).filter(
      (c) => c.id !== PIN_PORTAL_ID,
    ) as HTMLElement[]
    siblings.forEach((el) => el.setAttribute('inert', ''))
    inputRef.current?.focus()
    return () => { siblings.forEach((el) => el.removeAttribute('inert')) }
  }, [open])

  async function submit() {
    if (!pin || loading) return
    setLoading(true)
    setError('')
    try {
      await validatePin(pin)
      setActivePin(pin)
      inputRef.current?.blur()
      setOpen(false)
      setPin('')
      pendingRef.current?.resolve()
      pendingRef.current = null
    } catch {
      setError('PIN inválido. Tente novamente.')
      setPin('')
    } finally {
      setLoading(false)
    }
  }

  function cancel() {
    inputRef.current?.blur()
    setOpen(false)
    setPin('')
    setError('')
    pendingRef.current?.reject()
    pendingRef.current = null
  }

  const requestPin = useCallback((): Promise<void> => {
    return new Promise((resolve, reject) => {
      pendingRef.current = { resolve, reject }
      setPin('')
      setError('')
      setOpen(true)
    })
  }, [])

  const dialog = open ? (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 9999,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'rgba(0,0,0,0.5)',
    }}>
      <div style={{
        background: 'white', borderRadius: 16,
        border: '1px solid #e3e8ed',
        boxShadow: '0 24px 48px -12px rgba(20,26,32,0.18)',
        width: 'min(300px, calc(100vw - 40px))', padding: 20,
      }}>
        <p style={{ fontSize: 14, fontWeight: 500, color: '#111827', textAlign: 'center', marginBottom: 16 }}>
          Digite a sua senha:
        </p>

        <div
          style={{ position: 'relative', marginBottom: 16, cursor: 'text' }}
          onClick={() => inputRef.current?.focus()}
        >
          {/* Input real e transparente: tocar abre o teclado do celular */}
          <input
            ref={inputRef}
            value={pin}
            onChange={(e) => setPin(e.target.value.replace(/\D/g, '').slice(0, 4))}
            onKeyDown={(e) => { if (e.key === 'Enter') submit() }}
            type="tel"
            inputMode="numeric"
            autoComplete="off"
            autoFocus
            aria-label="Senha"
            style={{
              position: 'absolute', inset: 0, width: '100%', height: '100%',
              opacity: 0, border: 'none', background: 'transparent',
              fontSize: 16, // evita zoom automático no iOS
            }}
          />
          <div style={{ display: 'flex', justifyContent: 'center', gap: 8, pointerEvents: 'none' }}>
            {[0, 1, 2, 3].map((i) => (
              <div key={i} style={{
                width: 36, height: 44, borderRadius: 8,
                border: pin.length > i ? '2px solid #0d8a7e' : '2px solid #e3e8ed',
                background: pin.length > i ? '#f0fdfa' : 'white',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 18, fontWeight: 500, color: '#0e6e66',
              }}>
                {pin[i] ? '•' : ''}
              </div>
            ))}
          </div>
        </div>

        {error && (
          <p style={{ fontSize: 12, color: '#dc2626', textAlign: 'center', marginBottom: 12 }}>
            {error}
          </p>
        )}

        <div style={{ display: 'flex', gap: 8 }}>
          <button
            type="button"
            onClick={cancel}
            style={{
              flex: 1, height: 44, borderRadius: 8,
              border: '1px solid #d8dee5', background: 'white',
              fontSize: 14, color: '#4f5a66', cursor: 'pointer',
            }}
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={submit}
            disabled={pin.length === 0 || loading}
            style={{
              flex: 1, height: 44, borderRadius: 8, border: 'none',
              background: pin.length === 0 || loading ? '#97a2ad' : '#0d8a7e',
              fontSize: 14, fontWeight: 600, color: 'white',
              cursor: pin.length === 0 || loading ? 'not-allowed' : 'pointer',
            }}
          >
            {loading ? '...' : 'OK'}
          </button>
        </div>
      </div>
    </div>
  ) : null

  return (
    <PinContext.Provider value={{ requestPin }}>
      {children}
      {createPortal(dialog, containerRef.current)}
    </PinContext.Provider>
  )
}

export function usePin() {
  const ctx = useContext(PinContext)
  if (!ctx) throw new Error('usePin must be used inside PinProvider')
  return ctx
}

export function useWithPin() {
  const { requestPin } = usePin()
  return useCallback(
    async (action: () => void | Promise<void>) => {
      try {
        await requestPin()
        await action()
      } catch {
        // usuário cancelou
      }
    },
    [requestPin],
  )
}
