import { api } from '@/lib/api'
import type { User } from '@/types'

export async function validatePin(pin: string): Promise<User> {
  const { data } = await api.post<User>('/auth/validate-pin', { pin })
  return data
}

export async function changePin(currentPin: string, newPin: string): Promise<void> {
  await api.post('/auth/change-pin', { currentPin, newPin })
}
