import { api } from '@/lib/api'
import type { Customer, Page } from '@/types'

export async function listCustomers(page = 0, size = 20): Promise<Page<Customer>> {
  const { data } = await api.get<Page<Customer>>('/customers', {
    params: { page, size, sort: 'name' },
  })
  return data
}

export async function getCustomer(id: string): Promise<Customer> {
  const { data } = await api.get<Customer>(`/customers/${id}`)
  return data
}

export async function createCustomer(body: { name: string; phoneNumber?: string }): Promise<Customer> {
  const { data } = await api.post<Customer>('/customers', body)
  return data
}

export async function updateCustomer(
  id: string,
  body: { name: string; phoneNumber?: string },
): Promise<Customer> {
  const { data } = await api.put<Customer>(`/customers/${id}`, body)
  return data
}

export async function deleteCustomer(id: string): Promise<void> {
  await api.delete(`/customers/${id}`)
}
