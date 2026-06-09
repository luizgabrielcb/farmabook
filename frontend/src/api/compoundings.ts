import { api } from '@/lib/api'
import type { Compounding, Notification, Page } from '@/types'

interface CompoundingInput {
  quantity: number
  customerId: string
  pharmacyId: string
  value?: number | null
  observations?: string | null
}

export async function listCompoundings(page = 0, size = 500): Promise<Page<Compounding>> {
  const { data } = await api.get<Page<Compounding>>('/compoundings', {
    params: { page, size, sort: 'createdAt,desc' },
  })
  return data
}

export async function getCompounding(id: string): Promise<Compounding> {
  const { data } = await api.get<Compounding>(`/compoundings/${id}`)
  return data
}

export async function createCompounding(body: CompoundingInput): Promise<Compounding> {
  const { data } = await api.post<Compounding>('/compoundings', body)
  return data
}

export async function updateCompounding(id: string, body: CompoundingInput): Promise<Compounding> {
  const { data } = await api.put<Compounding>(`/compoundings/${id}`, body)
  return data
}

export async function deleteCompounding(id: string): Promise<void> {
  await api.delete(`/compoundings/${id}`)
}

export async function markCompoundingAsOrdered(id: string): Promise<void> {
  await api.patch(`/compoundings/${id}/mark-as-ordered`)
}

export async function markCompoundingAsReceived(id: string): Promise<void> {
  await api.patch(`/compoundings/${id}/mark-as-received`)
}

export async function markCompoundingAsDelivered(id: string): Promise<void> {
  await api.patch(`/compoundings/${id}/mark-as-delivered`)
}

export async function markCompoundingAsPaid(id: string): Promise<void> {
  await api.patch(`/compoundings/${id}/payment/mark-as-paid`)
}

export async function markCompoundingAsToPay(id: string): Promise<void> {
  await api.patch(`/compoundings/${id}/payment/mark-as-to-pay`)
}

export async function markCompoundingAsMakeNote(id: string): Promise<void> {
  await api.patch(`/compoundings/${id}/payment/mark-as-make-note`)
}

export async function markCompoundingAsNoted(id: string): Promise<void> {
  await api.patch(`/compoundings/${id}/payment/mark-as-noted`)
}

export async function listCompoundingNotifications(compoundingId: string, page = 0, size = 1): Promise<Page<Notification>> {
  const { data } = await api.get<Page<Notification>>(`/notifications/compoundings/${compoundingId}`, {
    params: { page, size },
  })
  return data
}
