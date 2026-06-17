import { useEffect, useRef, useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { MoreHorizontal } from 'lucide-react'
import { cn } from '@/lib/utils'
import { primaryNavItems, overflowNavItems } from './nav-config'

const itemClass = (isActive: boolean) =>
  cn(
    // h-full inside a 56px bar keeps the tappable area well above 44px
    'flex flex-col items-center justify-center gap-0.5 h-full min-w-0 flex-1 text-[10px] font-medium transition-colors',
    isActive ? 'text-brand-700' : 'text-gray-500',
  )

export function BottomNav() {
  const [moreOpen, setMoreOpen] = useState(false)
  const location = useLocation()
  const moreRef = useRef<HTMLDivElement>(null)

  const overflowActive = overflowNavItems.some((i) => location.pathname.startsWith(i.to))

  // Close the "Mais" sheet on navigation or outside tap.
  useEffect(() => setMoreOpen(false), [location.pathname])
  useEffect(() => {
    if (!moreOpen) return
    function onPointer(e: PointerEvent) {
      if (moreRef.current && !moreRef.current.contains(e.target as Node)) setMoreOpen(false)
    }
    window.addEventListener('pointerdown', onPointer)
    return () => window.removeEventListener('pointerdown', onPointer)
  }, [moreOpen])

  return (
    <div className="md:hidden" ref={moreRef}>
      {/* "Mais" sheet */}
      {moreOpen && (
        <>
          <div className="fixed inset-0 z-40 bg-black/20" onClick={() => setMoreOpen(false)} />
          <div className="fixed bottom-14 inset-x-0 z-50 bg-white border-t border-gray-150 shadow-[0_-8px_24px_-12px_rgba(20,26,32,0.18)] p-2 grid grid-cols-3 gap-1">
            {overflowNavItems.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  cn(
                    'flex flex-col items-center justify-center gap-1 py-3 rounded-lg text-xs font-medium',
                    isActive ? 'bg-brand-50 text-brand-700' : 'text-gray-600 active:bg-gray-100',
                  )
                }
              >
                <Icon size={20} />
                <span className="truncate max-w-full px-1">{label}</span>
              </NavLink>
            ))}
          </div>
        </>
      )}

      <nav className="fixed bottom-0 inset-x-0 z-50 h-14 bg-white border-t border-gray-150 flex items-stretch pb-[env(safe-area-inset-bottom)]">
        {primaryNavItems.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} className={({ isActive }) => itemClass(isActive)}>
            <Icon size={21} />
            <span className="truncate max-w-full px-0.5">{label}</span>
          </NavLink>
        ))}
        <button
          type="button"
          onClick={() => setMoreOpen((v) => !v)}
          className={cn(itemClass(overflowActive || moreOpen), 'cursor-pointer')}
        >
          <MoreHorizontal size={21} />
          <span>Mais</span>
        </button>
      </nav>
    </div>
  )
}
