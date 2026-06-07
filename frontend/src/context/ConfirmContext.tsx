import {
  createContext,
  useContext,
  useState,
  useRef,
  useCallback,
  type ReactNode,
} from 'react'
import { createPortal } from 'react-dom'

interface ConfirmContextValue {
  requestConfirm: (message: string) => Promise<void>
}

const ConfirmContext = createContext<ConfirmContextValue | null>(null)

interface Pending {
  resolve: () => void
  reject: () => void
}

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<{ message: string } | null>(null)
  const pendingRef = useRef<Pending | null>(null)

  const requestConfirm = useCallback((message: string): Promise<void> => {
    return new Promise((resolve, reject) => {
      pendingRef.current = { resolve, reject }
      setState({ message })
    })
  }, [])

  function handleConfirm() {
    setState(null)
    pendingRef.current?.resolve()
    pendingRef.current = null
  }

  function handleCancel() {
    setState(null)
    pendingRef.current?.reject()
    pendingRef.current = null
  }

  const dialog = state
    ? createPortal(
        <div
          className="fixed inset-0 flex items-center justify-center"
          style={{ zIndex: 9998 }}
          onMouseDown={(e) => e.stopPropagation()}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="absolute inset-0 bg-black/30" />
          <div
            className="relative bg-white rounded-lg border border-gray-200 shadow-xl w-72 p-5"
            onMouseDown={(e) => e.stopPropagation()}
            onClick={(e) => e.stopPropagation()}
          >
            <p className="text-sm text-gray-800 mb-5">{state.message}</p>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); handleCancel() }}
                className="h-8 px-4 rounded border border-gray-300 text-sm text-gray-600 hover:bg-gray-50 cursor-pointer transition-colors"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); handleConfirm() }}
                className="h-8 px-4 rounded bg-red-600 text-white text-sm font-medium hover:bg-red-700 cursor-pointer transition-colors"
              >
                Confirmar
              </button>
            </div>
          </div>
        </div>,
        document.body,
      )
    : null

  return (
    <ConfirmContext.Provider value={{ requestConfirm }}>
      {children}
      {dialog}
    </ConfirmContext.Provider>
  )
}

export function useConfirm() {
  const ctx = useContext(ConfirmContext)
  if (!ctx) throw new Error('useConfirm must be used inside ConfirmProvider')
  return useCallback(
    async (message: string): Promise<boolean> => {
      try {
        await ctx.requestConfirm(message)
        return true
      } catch {
        return false
      }
    },
    [ctx],
  )
}
