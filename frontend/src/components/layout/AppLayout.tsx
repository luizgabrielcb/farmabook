import { useEffect, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { ToastProvider } from '@/context/ToastContext'

const SHORTCUTS: Record<string, string> = {
  F2: '/shortages',
  F3: '/orders',
  F4: '/customers',
  F7: '/compoundings',
  F12: '/prescriptions',
}

export function AppLayout({ children }: { children: ReactNode }) {
  const navigate = useNavigate()

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      const route = SHORTCUTS[e.key]
      if (route) {
        e.preventDefault()
        navigate(route)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [navigate])

  return (
    <ToastProvider>
      <div className="flex h-screen bg-gray-50" style={{ colorScheme: 'light' }}>
        <Sidebar />
        <main className="flex-1 overflow-auto">{children}</main>
      </div>
    </ToastProvider>
  )
}
