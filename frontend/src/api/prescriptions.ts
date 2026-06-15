import { api } from '@/lib/api'
import type { Page, Prescription, PrescriptionItem } from '@/types'

interface PrescriptionItemInput {
  product: string
  quantity: number
  batch: string
  expiry: string
}

export async function listPrescriptions(page = 0, size = 500): Promise<Page<Prescription>> {
  const { data } = await api.get<Page<Prescription>>('/prescriptions', {
    params: { page, size, sort: 'createdAt,desc' },
  })
  return data
}

export async function getPrescription(id: string): Promise<Prescription> {
  const { data } = await api.get<Prescription>(`/prescriptions/${id}`)
  return data
}

export async function createPrescription(body: {
  customerId: string
  items: PrescriptionItemInput[]
  observations?: string | null
}): Promise<Prescription> {
  const { data } = await api.post<Prescription>('/prescriptions', body)
  return data
}

export async function updatePrescription(
  id: string,
  body: { customerId: string; observations?: string | null },
): Promise<Prescription> {
  const { data } = await api.put<Prescription>(`/prescriptions/${id}`, body)
  return data
}

export async function deletePrescription(id: string): Promise<void> {
  await api.delete(`/prescriptions/${id}`)
}

export async function markAllAsReceived(id: string): Promise<void> {
  await api.patch(`/prescriptions/${id}/mark-as-received`)
}

export async function markItemAsReceived(prescriptionId: string, itemId: string): Promise<void> {
  await api.patch(`/prescriptions/${prescriptionId}/items/${itemId}/mark-as-received`)
}

export async function addPrescriptionItem(
  id: string,
  item: PrescriptionItemInput,
): Promise<PrescriptionItem> {
  const { data } = await api.post<PrescriptionItem>(`/prescriptions/${id}/items`, item)
  return data
}

export async function updatePrescriptionItem(
  prescriptionId: string,
  itemId: string,
  item: PrescriptionItemInput,
): Promise<PrescriptionItem> {
  const { data } = await api.put<PrescriptionItem>(
    `/prescriptions/${prescriptionId}/items/${itemId}`,
    item,
  )
  return data
}

export async function deletePrescriptionItem(
  prescriptionId: string,
  itemId: string,
): Promise<void> {
  await api.delete(`/prescriptions/${prescriptionId}/items/${itemId}`)
}
