import { NavLink } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { navItems } from './nav-config'

export function Sidebar() {
  return (
    <aside className="hidden md:flex w-60 shrink-0 bg-white border-r border-gray-150 flex-col h-screen sticky top-0">
      <div className="px-4 py-4 flex items-center gap-2.5 border-b border-gray-150">
        <span className="grid place-items-center w-8 h-8 rounded-lg bg-gradient-to-br from-brand-500 to-brand-700 shadow-[0_2px_6px_-1px_rgba(13,138,126,0.45)]">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="white" aria-hidden="true">
            <rect x="8.5" y="3.5" width="7" height="17" rx="2" />
            <rect x="3.5" y="8.5" width="17" height="7" rx="2" />
          </svg>
        </span>
        <span className="text-base font-extrabold tracking-tight text-gray-900">
          Farma<span className="text-brand-700">Book</span>
        </span>
      </div>

      <nav className="flex-1 px-2.5 py-3 space-y-0.5 overflow-y-auto">
        {navItems.map(({ to, label, icon: Icon, shortcut }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-2.5 px-2.5 py-2 rounded-md text-sm transition-colors',
                isActive
                  ? 'bg-brand-50 text-brand-700 font-semibold'
                  : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900',
              )
            }
          >
            <Icon size={16} />
            <span className="flex-1">{label}</span>
            {shortcut && (
              <span className="text-[10px] text-gray-400 font-mono">{shortcut}</span>
            )}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
