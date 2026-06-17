import { useEffect, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { BottomNav } from './BottomNav'
import { ToastProvider } from '@/context/ToastContext'
import { pinState } from '@/lib/pinState'

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
      if (pinState.open) return
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
        {/* pb leaves room for the fixed mobile bottom nav (56px + safe area) */}
        <main className="flex-1 overflow-auto pb-[calc(3.5rem+env(safe-area-inset-bottom))] md:pb-0">
          {children}
        </main>
        <BottomNav />
      </div>
    </ToastProvider>
  )
}
