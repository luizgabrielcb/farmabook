import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getOrder, deleteOrder,
  markOrderAsOrdered, markOrderAsReceived, markOrderAsDelivered,
  deleteOrderItem, markItemAsOrdered, markItemAsReceived, markItemAsDelivered,
  addOrderItem, updateOrderItem,
} from '@/api/orders'
import { listNotifications } from '@/api/notifications'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { OrderStatusBadge } from '@/components/shared/StatusBadge'
import { CategoryBadge, CATEGORY_OPTIONS } from '@/components/shared/CategoryBadge'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { NotificationPopup } from '@/components/shared/NotificationPopup'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { formatDate } from '@/lib/utils'
import { ArrowLeft, Plus, Trash2, Pencil, MessageCircle } from 'lucide-react'
import type { Category, OrderItem, Notification } from '@/types'

interface ItemForm { product: string; category: Category; quantity: string }
const emptyItemForm: ItemForm = { product: '', category: 'MEDICAMENTOS', quantity: '' }

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const withPin = useWithPin()
  const confirm = useConfirm()

  const [itemDialogOpen, setItemDialogOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<OrderItem | null>(null)
  const [itemForm, setItemForm] = useState<ItemForm>(emptyItemForm)
  const [popup, setPopup] = useState<Notification | null>(null)

  const { data: order, isLoading, error } = useQuery({
    queryKey: ['order', id],
    queryFn: () => getOrder(id!),
    enabled: !!id,
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['order', id] })

  async function checkForNotification() {
    const fresh = await getOrder(id!)
    if (fresh.status === 'RECEIVED' && fresh.notifiedAt) {
      const notifs = await listNotifications(id!, 0, 1)
      if (notifs.content.length > 0) setPopup(notifs.content[0])
    }
    qc.invalidateQueries({ queryKey: ['order', id] })
  }

  const deleteMutation = useMutation({ mutationFn: () => deleteOrder(id!), onSuccess: () => navigate('/orders') })
  const markAllOrdered = useMutation({ mutationFn: () => markOrderAsOrdered(id!), onSuccess: invalidate })
  const markAllReceived = useMutation({ mutationFn: () => markOrderAsReceived(id!), onSuccess: checkForNotification })
  const markAllDelivered = useMutation({ mutationFn: () => markOrderAsDelivered(id!), onSuccess: invalidate })

  const saveItemMutation = useMutation({
    mutationFn: () => {
      const body = { product: itemForm.product, category: itemForm.category, quantity: itemForm.quantity ? Number(itemForm.quantity) : null }
      return editingItem
        ? updateOrderItem(id!, editingItem.id, body)
        : addOrderItem(id!, body)
    },
    onSuccess: () => { closeItemDialog(); invalidate() },
  })

  function openAddItem() { setEditingItem(null); setItemForm(emptyItemForm); saveItemMutation.reset(); setItemDialogOpen(true) }
  function openEditItem(item: OrderItem) {
    setEditingItem(item)
    setItemForm({ product: item.product, category: item.category, quantity: item.quantity != null ? String(item.quantity) : '' })
    saveItemMutation.reset(); setItemDialogOpen(true)
  }
  function closeItemDialog() { setItemDialogOpen(false); setEditingItem(null); setItemForm(emptyItemForm) }

  if (isLoading) return <div className="flex justify-center py-20"><Spinner /></div>
  if (error) return <div className="p-6"><ErrorMessage error={error} /></div>
  if (!order) return null

  const isDelivered = order.status === 'DELIVERED'
  const hasPending = order.items.some((i) => i.status === 'PENDING')
  const hasOrdered = order.items.some((i) => i.status === 'ORDERED')
  const hasReceived = order.items.some((i) => i.status === 'RECEIVED')

  async function handleDelete() {
    if (!await confirm('Excluir esta encomenda? Esta ação não pode ser desfeita.')) return
    withPin(() => deleteMutation.mutate())
  }

  return (
    <div>
      <PageHeader
        title={`Encomenda — ${order.customerName}`}
        description={`Criada por ${order.createdByName} em ${formatDate(order.createdAt)}`}
        actions={
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" onClick={() => navigate('/orders')}>
              <ArrowLeft size={13} /> Voltar
            </Button>
            {!isDelivered && (
              <>
                <Button variant="secondary" size="sm"
                  onClick={() => withPin(() => markAllOrdered.mutate())}
                  disabled={markAllOrdered.isPending || !hasPending}>
                  Todos pedidos
                </Button>
                <Button variant="secondary" size="sm"
                  onClick={() => withPin(() => markAllReceived.mutate())}
                  disabled={markAllReceived.isPending || !hasOrdered}>
                  Todos recebidos
                </Button>
                <Button variant="secondary" size="sm"
                  onClick={() => withPin(() => markAllDelivered.mutate())}
                  disabled={markAllDelivered.isPending || !hasReceived}>
                  Todos entregues
                </Button>
                <Button variant="danger" size="sm" onClick={handleDelete} disabled={deleteMutation.isPending}>
                  <Trash2 size={13} /> Excluir
                </Button>
              </>
            )}
          </div>
        }
      />

      <div className="p-6 space-y-4">
        <div className="bg-white border border-gray-200 rounded-lg p-4 flex gap-8 text-sm">
          <div>
            <span className="text-gray-500 text-xs uppercase tracking-wide">Status</span>
            <div className="mt-1"><OrderStatusBadge status={order.status} /></div>
          </div>
          <div>
            <span className="text-gray-500 text-xs uppercase tracking-wide">Cliente</span>
            <p className="mt-1 font-medium text-gray-900">{order.customerName}</p>
          </div>
          {order.notifiedAt && (
            <div>
              <span className="text-gray-500 text-xs uppercase tracking-wide">Notificado em</span>
              <div className="mt-1 flex items-center gap-1 text-green-600">
                <MessageCircle size={13} /><span>{formatDate(order.notifiedAt)}</span>
              </div>
            </div>
          )}
        </div>

        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
            <span className="text-sm font-medium text-gray-900">Itens</span>
            {!isDelivered && (
              <Button variant="secondary" size="sm" onClick={openAddItem}>
                <Plus size={12} /> Adicionar item
              </Button>
            )}
          </div>
          <Table>
            <TableHead>
              <tr>
                <Th>Produto</Th><Th>Categoria</Th><Th>Qtd.</Th><Th>Status</Th>
                <Th>Pedido por</Th><Th>Recebido por</Th><Th>Entregue por</Th><Th />
              </tr>
            </TableHead>
            <TableBody>
              {order.items.length === 0 && (
                <tr><Td colSpan={8} className="text-center text-gray-400 py-8">Nenhum item.</Td></tr>
              )}
              {order.items.map((item) => (
                <Tr key={item.id}>
                  <Td className="font-medium text-gray-900">{item.product}</Td>
                  <Td><CategoryBadge category={item.category} /></Td>
                  <Td>{item.quantity ?? '—'}</Td>
                  <Td><OrderStatusBadge status={item.status} /></Td>
                  <Td className="text-gray-500">{item.orderedByName ?? '—'}</Td>
                  <Td className="text-gray-500">{item.receivedByName ?? '—'}</Td>
                  <Td className="text-gray-500">{item.deliveredByName ?? '—'}</Td>
                  <Td>
                    <ItemActions
                      orderId={id!} item={item}
                      onEdit={() => openEditItem(item)}
                      onSuccess={item.status === 'ORDERED' ? checkForNotification : invalidate}
                    />
                  </Td>
                </Tr>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>

      <Dialog open={itemDialogOpen} onOpenChange={(v) => !v && closeItemDialog()}
        title={editingItem ? 'Editar item' : 'Adicionar item'}>
        <form onSubmit={(e) => { e.preventDefault(); withPin(() => saveItemMutation.mutate()) }} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Produto</label>
            <Input value={itemForm.product}
              onChange={(e) => setItemForm((p) => ({ ...p, product: e.target.value }))}
              required autoFocus autoComplete="off" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Categoria</label>
            <Select value={itemForm.category}
              onChange={(e) => setItemForm((p) => ({ ...p, category: e.target.value as Category }))}>
              {CATEGORY_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
            </Select>
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Quantidade <span className="text-gray-400">(opcional)</span>
            </label>
            <Input type="number" min={1} value={itemForm.quantity}
              onChange={(e) => setItemForm((p) => ({ ...p, quantity: e.target.value }))}
              placeholder="—" />
          </div>
          {saveItemMutation.isError && <ErrorMessage error={saveItemMutation.error} />}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={closeItemDialog}>Cancelar</Button>
            <Button type="submit" variant="primary" disabled={saveItemMutation.isPending}>
              {saveItemMutation.isPending ? 'Salvando...' : editingItem ? 'Salvar' : 'Adicionar'}
            </Button>
          </div>
        </form>
      </Dialog>

      {popup && <NotificationPopup notification={popup} onClose={() => setPopup(null)} />}
    </div>
  )
}

function ItemActions({
  orderId, item, onEdit, onSuccess,
}: { orderId: string; item: OrderItem; onEdit: () => void; onSuccess: () => void }) {
  const withPin = useWithPin()
  const confirm = useConfirm()

  const deleteMutation = useMutation({ mutationFn: () => deleteOrderItem(orderId, item.id), onSuccess })
  const advanceMutation = useMutation({
    mutationFn: () => {
      if (item.status === 'PENDING') return markItemAsOrdered(orderId, item.id)
      if (item.status === 'ORDERED') return markItemAsReceived(orderId, item.id)
      return markItemAsDelivered(orderId, item.id)
    },
    onSuccess,
  })

  const advanceLabel =
    item.status === 'PENDING' ? 'Marcar pedido'
    : item.status === 'ORDERED' ? 'Marcar recebido'
    : item.status === 'RECEIVED' ? 'Marcar entregue'
    : null

  if (item.status === 'DELIVERED') return null

  async function handleDelete() {
    if (!await confirm('Remover este item da encomenda?')) return
    withPin(() => deleteMutation.mutate())
  }

  return (
    <div className="flex items-center gap-1">
      {advanceLabel && (
        <Button variant="ghost" size="sm"
          onClick={() => withPin(() => advanceMutation.mutate())}
          disabled={advanceMutation.isPending}>
          {advanceLabel}
        </Button>
      )}
      <Button variant="ghost" size="sm" onClick={onEdit} title="Editar item">
        <Pencil size={12} />
      </Button>
      <Button variant="ghost" size="sm" onClick={handleDelete}
        disabled={deleteMutation.isPending} className="text-red-400 hover:text-red-600">
        <Trash2 size={12} />
      </Button>
    </div>
  )
}
