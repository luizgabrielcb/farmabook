import { api } from '@/lib/api'
import type { CompoundingPharmacy, Page } from '@/types'

export async function listCompoundingPharmacies(page = 0, size = 500): Promise<Page<CompoundingPharmacy>> {
  const { data } = await api.get<Page<CompoundingPharmacy>>('/compounding-pharmacies', {
    params: { page, size, sort: 'name,asc' },
  })
  return data
}

export async function createCompoundingPharmacy(body: { name: string; city: string }): Promise<CompoundingPharmacy> {
  const { data } = await api.post<CompoundingPharmacy>('/compounding-pharmacies', body)
  return data
}

export async function updateCompoundingPharmacy(id: string, body: { name: string; city: string }): Promise<CompoundingPharmacy> {
  const { data } = await api.put<CompoundingPharmacy>(`/compounding-pharmacies/${id}`, body)
  return data
}

export async function deleteCompoundingPharmacy(id: string): Promise<void> {
  await api.delete(`/compounding-pharmacies/${id}`)
}
