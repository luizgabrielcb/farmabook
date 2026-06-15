export type UserRole = 'ADMIN' | 'SELLER'
export type PrescriptionStatus = 'PENDING' | 'FINISHED'
export type PrescriptionItemStatus = 'PENDING' | 'RECEIVED'
export type OrderStatus = 'PENDING' | 'ORDERED' | 'RECEIVED' | 'DELIVERED'
export type OrderItemStatus = 'PENDING' | 'ORDERED' | 'RECEIVED' | 'DELIVERED'
export type OrderPaymentStatus = 'TO_PAY' | 'MAKE_NOTE' | 'PAID' | 'NOTED'
export type ShortageStatus = 'PENDING' | 'ORDERED'
export type ShortageType = 'WANIA' | 'FRANCISCO'
export type ShortageOrderStatus = 'PENDING' | 'ORDERED'
export type CompoundingStatus = 'PENDING' | 'ORDERED' | 'RECEIVED' | 'DELIVERED'
export type PaymentStatus = 'TO_PAY' | 'PAID' | 'MAKE_NOTE' | 'NOTED'
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
  distributorId: string | null
  distributorName: string | null
  price: number | null
  paymentStatus: OrderPaymentStatus
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
  observations: string | null
  paymentStatus: OrderPaymentStatus
  totalPrice: number | null
  items: OrderItem[]
}

export interface Distributor {
  id: string
  name: string
  createdAt: string
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
  shortageOrderId: string | null
  costPrice: number | null
  createdAt: string
  updatedAt?: string
}

export interface ShortageOrderListItem {
  id: string
  shortageType: ShortageType
  distributorId: string
  distributorName: string
  status: ShortageOrderStatus
  createdById: string
  createdByName: string
  orderedById: string | null
  orderedByName: string | null
  orderedAt: string | null
  observations: string | null
  createdAt: string
  updatedAt: string
}

export interface ShortageOrder {
  id: string
  shortageType: ShortageType
  distributorId: string
  distributorName: string
  status: ShortageOrderStatus
  createdById: string
  createdByName: string
  orderedById: string | null
  orderedByName: string | null
  orderedAt: string | null
  observations: string | null
  shortages: Shortage[]
  createdAt: string
  updatedAt: string
}

export interface PrescriptionItem {
  id: string
  product: string
  quantity: number
  batch: string
  expiry: string
  status: PrescriptionItemStatus
  receivedById: string | null
  receivedByName: string | null
  receivedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface Prescription {
  id: string
  customerId: string
  customerName: string
  status: PrescriptionStatus
  createdById: string
  createdByName: string
  observations: string | null
  items: PrescriptionItem[]
  createdAt: string
  updatedAt: string
}

export interface Notification {
  id: string
  orderId: string | null
  compoundingId: string | null
  customerId: string
  customerPhone: string
  customerName: string
  message: string
  link: string
  sentAt: string
}

export interface CompoundingPharmacy {
  id: string
  name: string
  city: string
  createdAt: string
  updatedAt?: string
}

export interface Compounding {
  id: string
  quantity: number
  customerId: string
  customerName: string
  pharmacyId: string
  pharmacyName: string
  pharmacyCity: string
  value: number | null
  observations: string | null
  status: CompoundingStatus
  paymentStatus: PaymentStatus
  notifiedAt: string | null
  createdById: string
  createdByName: string
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
