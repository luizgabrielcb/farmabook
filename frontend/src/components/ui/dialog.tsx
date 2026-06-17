import { useEffect, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { cn } from '@/lib/utils'
import { pinState } from '@/lib/pinState'

interface DialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: string
  children: ReactNode
  className?: string
}

export function Dialog({ open, onOpenChange, title, description, children, className }: DialogProps) {
  useEffect(() => {
    if (!open) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape' && !pinState.open) onOpenChange(false)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onOpenChange])

  if (!open) return null

  return createPortal(
    <div
      style={{
        position: 'fixed', inset: 0, zIndex: 50,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '16px',
      }}
    >
      {/* backdrop */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'rgba(20,26,32,0.45)',
          backdropFilter: 'blur(2px)',
        }}
        onClick={() => !pinState.open && onOpenChange(false)}
      />
      {/* conteúdo */}
      <div
        className={cn(
          'relative bg-white rounded-2xl shadow-[0_24px_48px_-12px_rgba(20,26,32,0.18)] border border-gray-150 w-full max-w-md p-6 focus:outline-none max-h-[90vh] overflow-y-auto',
          className,
        )}
        style={{ animation: 'fb-dialog-in 180ms cubic-bezier(0.22,1,0.36,1)' }}
      >
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-base font-semibold text-gray-900">{title}</h2>
            {description && (
              <p className="text-sm text-gray-500 mt-0.5">{description}</p>
            )}
          </div>
          <button
            onClick={() => !pinState.open && onOpenChange(false)}
            className="text-gray-400 hover:text-gray-600 transition-colors cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>
        {children}
      </div>
    </div>,
    document.body,
  )
}
