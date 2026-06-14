import { api } from '@/lib/api'
import type { Order, OrderItem, Page } from '@/types'
import type { Category } from '@/types'

interface OrderItemInput {
  product: string
  category: Category
  quantity: number | null
  price?: number | null
}

export async function listOrders(page = 0, size = 20): Promise<Page<Order>> {
  const { data } = await api.get<Page<Order>>('/orders', {
    params: { page, size, sort: 'createdAt,desc' },
  })
  return data
}

export async function getOrder(id: string): Promise<Order> {
  const { data } = await api.get<Order>(`/orders/${id}`)
  return data
}

export async function createOrder(body: {
  customerId: string
  items: OrderItemInput[]
  observations?: string | null
  totalPrice?: number | null
}): Promise<Order> {
  const { data } = await api.post<Order>('/orders', body)
  return data
}

export async function updateOrder(
  id: string,
  body: { customerId: string; observations?: string | null; totalPrice?: number | null; paymentStatus?: string | null },
): Promise<Order> {
  const { data } = await api.put<Order>(`/orders/${id}`, body)
  return data
}

export async function deleteOrder(id: string): Promise<void> {
  await api.delete(`/orders/${id}`)
}

export async function markOrderAsOrdered(id: string, distributorId: string): Promise<void> {
  await api.patch(`/orders/${id}/mark-as-ordered`, { distributorId })
}

export async function markOrderAsReceived(id: string): Promise<void> {
  await api.patch(`/orders/${id}/mark-as-received`)
}

export async function markOrderAsDelivered(id: string): Promise<void> {
  await api.patch(`/orders/${id}/mark-as-delivered`)
}

export async function addOrderItem(id: string, item: OrderItemInput): Promise<OrderItem> {
  const { data } = await api.post<OrderItem>(`/orders/${id}/items`, item)
  return data
}

export async function updateOrderItem(
  orderId: string,
  itemId: string,
  item: OrderItemInput,
): Promise<OrderItem> {
  const { data } = await api.put<OrderItem>(`/orders/${orderId}/items/${itemId}`, item)
  return data
}

export async function deleteOrderItem(orderId: string, itemId: string): Promise<void> {
  await api.delete(`/orders/${orderId}/items/${itemId}`)
}

export async function markItemAsOrdered(
  orderId: string,
  itemId: string,
  distributorId: string,
): Promise<void> {
  await api.patch(`/orders/${orderId}/items/${itemId}/mark-as-ordered`, { distributorId })
}

export async function markItemAsReceived(orderId: string, itemId: string): Promise<void> {
  await api.patch(`/orders/${orderId}/items/${itemId}/mark-as-received`)
}

export async function markItemAsDelivered(orderId: string, itemId: string): Promise<void> {
  await api.patch(`/orders/${orderId}/items/${itemId}/mark-as-delivered`)
}

export async function markItemPaymentAsPaid(orderId: string, itemId: string): Promise<void> {
  await api.patch(`/orders/${orderId}/items/${itemId}/payment/mark-as-paid`)
}

export async function markItemPaymentAsMakeNote(orderId: string, itemId: string): Promise<void> {
  await api.patch(`/orders/${orderId}/items/${itemId}/payment/mark-as-make-note`)
}

export async function markItemPaymentAsNoted(orderId: string, itemId: string): Promise<void> {
  await api.patch(`/orders/${orderId}/items/${itemId}/payment/mark-as-noted`)
}

export async function markItemPaymentAsToPay(orderId: string, itemId: string): Promise<void> {
  await api.patch(`/orders/${orderId}/items/${itemId}/payment/mark-as-to-pay`)
}
