import { api } from '@/lib/api'
import type { ShortageOrder, ShortageOrderListItem, Page, ShortageType, Category } from '@/types'

interface ShortageOrderItemInput {
  product: string
  category: Category
  quantity: number | null
  costPrice: number | null
}

interface CreateShortageOrderInput {
  shortageType: ShortageType
  distributorId: string
  observations: string | null
  items: ShortageOrderItemInput[]
}

export async function listShortageOrders(
  shortageType: ShortageType,
  distributorId?: string,
  page = 0,
  size = 20,
): Promise<Page<ShortageOrderListItem>> {
  const { data } = await api.get<Page<ShortageOrderListItem>>('/shortage-orders', {
    params: { shortageType, distributorId, page, size, sort: 'createdAt,desc' },
  })
  return data
}

export async function getShortageOrder(id: string): Promise<ShortageOrder> {
  const { data } = await api.get<ShortageOrder>(`/shortage-orders/${id}`)
  return data
}

export async function createShortageOrder(body: CreateShortageOrderInput): Promise<ShortageOrder> {
  const { data } = await api.post<ShortageOrder>('/shortage-orders', body)
  return data
}

interface UpdateShortageOrderInput {
  distributorId: string
  observations: string | null
}

export async function updateShortageOrder(id: string, body: UpdateShortageOrderInput): Promise<ShortageOrder> {
  const { data } = await api.put<ShortageOrder>(`/shortage-orders/${id}`, body)
  return data
}

export async function deleteShortageOrder(id: string): Promise<void> {
  await api.delete(`/shortage-orders/${id}`)
}

export async function markShortageOrderAsOrdered(id: string): Promise<void> {
  await api.patch(`/shortage-orders/${id}/mark-as-ordered`)
}

export async function addShortageOrderItem(id: string, body: ShortageOrderItemInput): Promise<ShortageOrder> {
  const { data } = await api.post<ShortageOrder>(`/shortage-orders/${id}/items`, body)
  return data
}
