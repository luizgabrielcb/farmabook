import { api } from '@/lib/api'
import type { Notification, Page } from '@/types'

export async function listNotifications(orderId: string, page = 0, size = 20): Promise<Page<Notification>> {
  const { data } = await api.get<Page<Notification>>(`/orders/${orderId}/notifications`, {
    params: { page, size },
  })
  return data
}

export async function resendNotification(id: string): Promise<Notification> {
  const { data } = await api.post<Notification>(`/notifications/${id}/resend`)
  return data
}
