import { Badge } from '@/components/ui/badge'
import type { OrderStatus, OrderItemStatus, ShortageStatus } from '@/types'

const orderStatusMap: Record<OrderStatus, { label: string; variant: 'gray' | 'blue' | 'yellow' | 'green' | 'red' | 'purple' }> = {
  PENDING: { label: 'Pendente', variant: 'gray' },
  ORDERED: { label: 'Pedido', variant: 'blue' },
  RECEIVED: { label: 'Recebido', variant: 'yellow' },
  DELIVERED: { label: 'Entregue', variant: 'green' },
}

const shortageStatusMap: Record<ShortageStatus, { label: string; variant: 'gray' | 'blue' }> = {
  PENDING: { label: 'Pendente', variant: 'gray' },
  ORDERED: { label: 'Pedido', variant: 'blue' },
}

export function OrderStatusBadge({ status }: { status: OrderStatus | OrderItemStatus }) {
  const map = orderStatusMap[status as OrderStatus]
  return <Badge variant={map.variant}>{map.label}</Badge>
}

export function ShortageStatusBadge({ status }: { status: ShortageStatus }) {
  const map = shortageStatusMap[status]
  return <Badge variant={map.variant}>{map.label}</Badge>
}
