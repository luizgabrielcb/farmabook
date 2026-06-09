import { api } from '@/lib/api'
import type { Distributor, Page } from '@/types'

export async function listDistributors(page = 0, size = 500): Promise<Page<Distributor>> {
  const { data } = await api.get<Page<Distributor>>('/distributors', {
    params: { page, size, sort: 'name,asc' },
  })
  return data
}

export async function createDistributor(body: { name: string }): Promise<Distributor> {
  const { data } = await api.post<Distributor>('/distributors', body)
  return data
}

export async function updateDistributor(id: string, body: { name: string }): Promise<Distributor> {
  const { data } = await api.put<Distributor>(`/distributors/${id}`, body)
  return data
}

export async function deleteDistributor(id: string): Promise<void> {
  await api.delete(`/distributors/${id}`)
}
