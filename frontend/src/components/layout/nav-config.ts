import { ShoppingCart, Package, Users, UserCog, Truck, FileText, FlaskConical, type LucideIcon } from 'lucide-react'

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  shortcut?: string
}

// Single source of truth shared by the desktop Sidebar and the mobile BottomNav.
export const navItems: NavItem[] = [
  { to: '/orders', label: 'Encomendas', icon: ShoppingCart, shortcut: 'F3' },
  { to: '/shortages', label: 'Faltas', icon: Package, shortcut: 'F2' },
  { to: '/prescriptions', label: 'Receitas', icon: FileText, shortcut: 'F12' },
  { to: '/compoundings', label: 'Manipulações', icon: FlaskConical, shortcut: 'F7' },
  { to: '/customers', label: 'Clientes', icon: Users, shortcut: 'F4' },
  { to: '/users', label: 'Usuários', icon: UserCog },
  { to: '/distributors', label: 'Distribuidoras', icon: Truck },
]

// On mobile the four primary operational flows live in the bottom bar; the
// remaining destinations are reachable through the "Mais" sheet.
export const primaryNavItems: NavItem[] = navItems.slice(0, 4)
export const overflowNavItems: NavItem[] = navItems.slice(4)
