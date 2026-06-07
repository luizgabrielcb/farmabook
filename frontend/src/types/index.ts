export type UserRole = 'ADMIN' | 'SELLER'
export type OrderStatus = 'PENDING' | 'ORDERED' | 'RECEIVED' | 'DELIVERED'
export type OrderItemStatus = 'PENDING' | 'ORDERED' | 'RECEIVED' | 'DELIVERED'
export type ShortageStatus = 'PENDING' | 'ORDERED'
export type ShortageType = 'WANIA' | 'FRANCISCO'
export type Category =
  | 'MEDICAMENTOS'
  | 'PERFUMARIA'
  | 'SUPLEMENTOS'
  | 'PRODUTOS_NATURAIS'
  | 'OUTROS'

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export interface User {
  id: string
  name: string
  role: UserRole
  active: boolean
  createdAt: string
  updatedAt?: string
}

export interface Customer {
  id: string
  name: string
  phoneNumber: string | null
  createdAt: string
  updatedAt?: string
}

export interface OrderItem {
  id: string
  product: string
  category: Category
  quantity: number | null
  status: OrderItemStatus
  orderedById: string | null
  orderedByName: string | null
  orderedAt: string | null
  receivedById: string | null
  receivedByName: string | null
  receivedAt: string | null
  deliveredById: string | null
  deliveredByName: string | null
  deliveredAt: string | null
  createdAt: string
  updatedAt: string
}

export interface Order {
  id: string
  customerId: string
  customerName: string
  status: OrderStatus
  notifiedAt: string | null
  createdById: string
  createdByName: string
  createdAt: string
  updatedAt?: string
  items: OrderItem[]
}

export interface Shortage {
  id: string
  product: string
  category: Category
  quantity: number | null
  status: ShortageStatus
  shortageType: ShortageType
  createdById: string
  createdByName: string
  orderedById: string | null
  orderedByName: string | null
  orderedAt: string | null
  createdAt: string
  updatedAt?: string
}

export interface Notification {
  id: string
  orderId: string
  customerId: string
  customerPhone: string
  customerName: string
  message: string
  link: string
  sentAt: string
}
