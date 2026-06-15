import { NavLink } from 'react-router-dom'
import { ShoppingCart, Users, UserCog, Package, Bell, Truck } from 'lucide-react'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/orders', label: 'Encomendas', icon: ShoppingCart, shortcut: 'F3' },
  { to: '/customers', label: 'Clientes', icon: Users, shortcut: 'F4' },
  { to: '/shortages', label: 'Faltas', icon: Package, shortcut: 'F2' },
  { to: '/distributors', label: 'Distribuidoras', icon: Truck },
  { to: '/users', label: 'Usuários', icon: UserCog },
  { to: '/notifications', label: 'Notificações', icon: Bell },
]

export function Sidebar() {
  return (
    <aside className="w-52 shrink-0 bg-white border-r border-gray-200 flex flex-col h-screen sticky top-0">
      <div className="px-4 py-4 border-b border-gray-200">
        <span className="text-sm font-semibold text-gray-900 tracking-tight">FarmaBook</span>
      </div>

      <nav className="flex-1 px-2 py-3 space-y-0.5">
        {navItems.map(({ to, label, icon: Icon, shortcut }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-2.5 px-2.5 py-2 rounded text-sm transition-colors',
                isActive
                  ? 'bg-gray-100 text-gray-900 font-medium'
                  : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900',
              )
            }
          >
            <Icon size={15} />
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
