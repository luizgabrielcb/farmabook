import { api } from '@/lib/api'
import type { User, Page, UserRole } from '@/types'

export async function listUsers(page = 0, size = 20): Promise<Page<User>> {
  const { data } = await api.get<Page<User>>('/users', {
    params: { page, size, sort: 'name' },
  })
  return data
}

export async function getUser(id: string): Promise<User> {
  const { data } = await api.get<User>(`/users/${id}`)
  return data
}

export async function createUser(body: { name: string; pin: string; role: UserRole }): Promise<User> {
  const { data } = await api.post<User>('/users', body)
  return data
}

export async function updateUser(id: string, body: { name: string; role: UserRole }): Promise<User> {
  const { data } = await api.put<User>(`/users/${id}`, body)
  return data
}

export async function deleteUser(id: string): Promise<void> {
  await api.delete(`/users/${id}`)
}

export async function activateUser(id: string): Promise<void> {
  await api.patch(`/users/${id}/activate`)
}

export async function deactivateUser(id: string): Promise<void> {
  await api.patch(`/users/${id}/deactivate`)
}
