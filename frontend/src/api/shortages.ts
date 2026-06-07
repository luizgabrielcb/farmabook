import { api } from '@/lib/api'
import type { Shortage, Page, Category, ShortageType } from '@/types'

interface ShortageInput {
  product: string
  category: Category
  quantity: number | null
  shortageType: ShortageType
}

export async function listShortages(page = 0, size = 20): Promise<Page<Shortage>> {
  const { data } = await api.get<Page<Shortage>>('/shortages', {
    params: { page, size, sort: 'createdAt,desc' },
  })
  return data
}

export async function getShortage(id: string): Promise<Shortage> {
  const { data } = await api.get<Shortage>(`/shortages/${id}`)
  return data
}

export async function createShortage(body: ShortageInput): Promise<Shortage> {
  const { data } = await api.post<Shortage>('/shortages', body)
  return data
}

export async function updateShortage(id: string, body: ShortageInput): Promise<Shortage> {
  const { data } = await api.put<Shortage>(`/shortages/${id}`, body)
  return data
}

export async function deleteShortage(id: string): Promise<void> {
  await api.delete(`/shortages/${id}`)
}

export async function markShortageAsOrdered(id: string): Promise<void> {
  await api.patch(`/shortages/${id}/mark-as-ordered`)
}
