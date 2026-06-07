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
  const submitRef = useRef<() => void>(() => {})

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

  // inert em todos os filhos do body exceto o container do PIN
  useEffect(() => {
    if (!open) return
    if (document.activeElement instanceof HTMLElement) document.activeElement.blur()
    const siblings = Array.from(document.body.children).filter(
      (c) => c.id !== PIN_PORTAL_ID,
    ) as HTMLElement[]
    siblings.forEach((el) => el.setAttribute('inert', ''))
    return () => { siblings.forEach((el) => el.removeAttribute('inert')) }
  }, [open])

  // Teclado em capture phase — bloqueia tudo atrás
  useEffect(() => {
    if (!open) return
    function onKey(e: KeyboardEvent) {
      e.stopPropagation()
      e.preventDefault()
      if (e.key >= '0' && e.key <= '9') setPin((p) => (p.length < 4 ? p + e.key : p))
      else if (e.key === 'Backspace' || e.key === 'Delete') setPin((p) => p.slice(0, -1))
      else if (e.key === 'Enter') submitRef.current()
    }
    window.addEventListener('keydown', onKey, true)
    return () => window.removeEventListener('keydown', onKey, true)
  }, [open])

  async function submit() {
    if (!pin || loading) return
    setLoading(true)
    setError('')
    try {
      await validatePin(pin)
      setActivePin(pin)
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

  useEffect(() => { submitRef.current = submit })

  function cancel() {
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
        background: 'white', borderRadius: 8,
        border: '1px solid #e5e7eb',
        boxShadow: '0 20px 60px rgba(0,0,0,0.2)',
        width: 224, padding: 24,
      }}>
        <p style={{ fontSize: 14, fontWeight: 500, color: '#111827', textAlign: 'center', marginBottom: 16 }}>
          Digite a sua senha:
        </p>

        <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginBottom: 16 }}>
          {[0, 1, 2, 3].map((i) => (
            <div key={i} style={{
              width: 36, height: 36, borderRadius: 6,
              border: pin.length > i ? '2px solid #374151' : '2px solid #e5e7eb',
              background: pin.length > i ? '#f9fafb' : 'white',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 18, fontWeight: 500,
            }}>
              {pin[i] ? '•' : ''}
            </div>
          ))}
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
              flex: 1, height: 32, borderRadius: 6,
              border: '1px solid #d1d5db', background: 'white',
              fontSize: 14, color: '#4b5563', cursor: 'pointer',
            }}
            onMouseOver={(e) => { e.currentTarget.style.background = '#f9fafb' }}
            onMouseOut={(e) => { e.currentTarget.style.background = 'white' }}
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={submit}
            disabled={pin.length === 0 || loading}
            style={{
              flex: 1, height: 32, borderRadius: 6, border: 'none',
              background: pin.length === 0 || loading ? '#9ca3af' : '#1f2937',
              fontSize: 14, fontWeight: 500, color: 'white',
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
    async (action: () => void) => {
      try {
        await requestPin()
        action()
      } catch {
        // usuário cancelou
      }
    },
    [requestPin],
  )
}
