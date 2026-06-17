import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getOrder, deleteOrder, updateOrder,
  markOrderAsOrdered, markOrderAsReceived, markOrderAsDelivered,
  deleteOrderItem, markItemAsOrdered, markItemAsReceived, markItemAsDelivered,
  markItemPaymentAsPaid, markItemPaymentAsMakeNote, markItemPaymentAsNoted, markItemPaymentAsToPay,
  addOrderItem, updateOrderItem,
} from '@/api/orders'
import { listNotifications } from '@/api/notifications'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { CardList, MobileCard } from '@/components/ui/mobile-card'
import { OrderStatusBadge, OrderPaymentStatusBadge } from '@/components/shared/StatusBadge'
import { CategoryBadge, CATEGORY_OPTIONS } from '@/components/shared/CategoryBadge'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { NotificationPopup } from '@/components/shared/NotificationPopup'
import { DistributorSearch } from '@/components/shared/DistributorSearch'
import { PriceInput, parsePriceInput } from '@/components/shared/PriceInput'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { useToast } from '@/context/ToastContext'
import { formatDate, cn } from '@/lib/utils'
import { ArrowLeft, Plus, Trash2, Pencil, MessageCircle, Settings2, X } from 'lucide-react'
import type { Category, Distributor, OrderItem, Notification } from '@/types'

interface ItemForm { product: string; category: Category; quantity: string; price: string }
const emptyItemForm: ItemForm = { product: '', category: 'MEDICAMENTOS', quantity: '', price: '' }

type DistributorPickerState = null | 'bulk' | 'bulk-selected' | { itemId: string }
type DetailTab = 'itens' | 'pagamento' | 'historico'

const currencyFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const withPin = useWithPin()
  const confirm = useConfirm()
  const toast = useToast()

  const [tab, setTab] = useState<DetailTab>('itens')
  const [itemDialogOpen, setItemDialogOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<OrderItem | null>(null)
  const [itemForm, setItemForm] = useState<ItemForm>(emptyItemForm)
  const [popup, setPopup] = useState<Notification | null>(null)
  const [distributorPicker, setDistributorPicker] = useState<DistributorPickerState>(null)
  const [pickedDistributor, setPickedDistributor] = useState<Distributor | null>(null)
  const [editOrderOpen, setEditOrderOpen] = useState(false)
  const [editObservations, setEditObservations] = useState('')
  const [editTotalPrice, setEditTotalPrice] = useState('')
  const [editPaymentStatus, setEditPaymentStatus] = useState<string>('')
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())

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

  async function handleNotify() {
    const notifs = await listNotifications(id!, 0, 1)
    if (notifs.content.length > 0) setPopup(notifs.content[0])
  }

  const deleteMutation = useMutation({
    mutationFn: () => deleteOrder(id!),
    onSuccess: () => { toast.success('Encomenda excluída'); navigate('/orders') },
  })

  const updateOrderMutation = useMutation({
    mutationFn: () => updateOrder(id!, {
      customerId: order!.customerId,
      observations: editObservations.trim() || null,
      totalPrice: parsePriceInput(editTotalPrice),
      paymentStatus: editPaymentStatus || null,
    }),
    onSuccess: () => { toast.success('Alterações salvas'); setEditOrderOpen(false); invalidate() },
  })

  function openEditOrder() {
    setEditObservations(order?.observations ?? '')
    setEditTotalPrice(order?.totalPrice != null ? String(order.totalPrice).replace('.', ',') : '')
    setEditPaymentStatus('')
    updateOrderMutation.reset()
    setEditOrderOpen(true)
  }

  const markAllOrdered = useMutation({
    mutationFn: (distributorId: string) => markOrderAsOrdered(id!, distributorId),
    onSuccess: () => {
      toast.success('Todos os itens marcados como pedido')
      setDistributorPicker(null)
      setPickedDistributor(null)
      invalidate()
    },
  })

  const markAllReceived = useMutation({
    mutationFn: () => markOrderAsReceived(id!),
    onSuccess: () => { toast.success('Todos os itens marcados como recebido'); checkForNotification() },
  })

  const markAllDelivered = useMutation({
    mutationFn: () => markOrderAsDelivered(id!),
    onSuccess: () => { toast.success('Todos os itens marcados como entregue'); invalidate() },
  })

  function toggleSelectItem(itemId: string) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(itemId)) next.delete(itemId)
      else next.add(itemId)
      return next
    })
  }

  function toggleSelectAll() {
    if (allSelected) {
      setSelectedIds(new Set())
    } else {
      setSelectedIds(new Set(selectableItems.map((i) => i.id)))
    }
  }

  async function bulkMarkReceived() {
    const targets = (order?.items ?? []).filter((i) => selectedIds.has(i.id) && i.status === 'ORDERED')
    if (!targets.length) return
    withPin(async () => {
      for (const i of targets) await markItemAsReceived(id!, i.id)
      setSelectedIds(new Set())
      checkForNotification()
    })
  }

  async function bulkMarkDelivered() {
    const targets = (order?.items ?? []).filter((i) => selectedIds.has(i.id) && i.status === 'RECEIVED')
    if (!targets.length) return
    if (!await confirm(`Marcar ${targets.length} item(s) como entregue? Esta ação não pode ser desfeita.`)) return
    withPin(async () => {
      for (const i of targets) await markItemAsDelivered(id!, i.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  async function bulkDelete() {
    const targets = (order?.items ?? []).filter((i) => selectedIds.has(i.id) && i.status !== 'DELIVERED')
    if (!targets.length) return
    if (!await confirm(`Remover ${targets.length} item(s) da encomenda?`)) return
    withPin(async () => {
      for (const i of targets) await deleteOrderItem(id!, i.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  const saveItemMutation = useMutation({
    mutationFn: () => {
      const body = {
        product: itemForm.product,
        category: itemForm.category,
        quantity: Number(itemForm.quantity),
        price: parsePriceInput(itemForm.price),
      }
      return editingItem
        ? updateOrderItem(id!, editingItem.id, body)
        : addOrderItem(id!, body)
    },
    onSuccess: () => { toast.success(editingItem ? 'Item atualizado' : 'Item adicionado'); closeItemDialog(); invalidate() },
  })

  function openAddItem() {
    setEditingItem(null)
    setItemForm(emptyItemForm)
    saveItemMutation.reset()
    setItemDialogOpen(true)
  }

  function openEditItem(item: OrderItem) {
    setEditingItem(item)
    setItemForm({
      product: item.product,
      category: item.category,
      quantity: String(item.quantity ?? ''),
      price: item.price != null ? String(item.price).replace('.', ',') : '',
    })
    saveItemMutation.reset()
    setItemDialogOpen(true)
  }

  function closeItemDialog() {
    setItemDialogOpen(false)
    setEditingItem(null)
    setItemForm(emptyItemForm)
  }

  function openDistributorPicker(state: DistributorPickerState) {
    setPickedDistributor(null)
    setDistributorPicker(state)
  }

  function markItemOrderedWithDistributor(itemId: string, distributorId: string) {
    markItemAsOrdered(id!, itemId, distributorId).then(() => {
      setDistributorPicker(null)
      setPickedDistributor(null)
      invalidate()
    })
  }

  async function bulkMarkOrdered(distributorId: string) {
    const targets = (order?.items ?? []).filter((i) => selectedIds.has(i.id) && i.status === 'PENDING')
    withPin(async () => {
      for (const i of targets) await markItemAsOrdered(id!, i.id, distributorId)
      setDistributorPicker(null)
      setPickedDistributor(null)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  async function bulkPayPaid() {
    const targets = (order?.items ?? []).filter(
      (i) => selectedIds.has(i.id) && (i.paymentStatus === 'TO_PAY' || i.paymentStatus === 'MAKE_NOTE'),
    )
    if (!targets.length) return
    withPin(async () => {
      for (const i of targets) await markItemPaymentAsPaid(id!, i.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  async function bulkPayMakeNote() {
    const targets = (order?.items ?? []).filter((i) => selectedIds.has(i.id) && i.paymentStatus === 'TO_PAY')
    if (!targets.length) return
    withPin(async () => {
      for (const i of targets) await markItemPaymentAsMakeNote(id!, i.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  async function bulkPayToPay() {
    const targets = (order?.items ?? []).filter((i) => selectedIds.has(i.id) && i.paymentStatus === 'MAKE_NOTE')
    if (!targets.length) return
    withPin(async () => {
      for (const i of targets) await markItemPaymentAsToPay(id!, i.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  async function bulkPayNoted() {
    const targets = (order?.items ?? []).filter((i) => selectedIds.has(i.id) && i.paymentStatus === 'MAKE_NOTE')
    if (!targets.length) return
    withPin(async () => {
      for (const i of targets) await markItemPaymentAsNoted(id!, i.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  if (isLoading) return <div className="flex justify-center py-20"><Spinner /></div>
  if (error) return <div className="p-6"><ErrorMessage error={error} /></div>
  if (!order) return null

  const isDelivered = order.status === 'DELIVERED'
  const hasPending = order.items.some((i) => i.status === 'PENDING')
  const hasOrdered = order.items.some((i) => i.status === 'ORDERED')
  const hasReceived = order.items.some((i) => i.status === 'RECEIVED')
  const hasDeliveredItem = order.items.some((i) => i.status === 'DELIVERED')
  const selectableItems = order.items.filter((i) => i.status !== 'DELIVERED')
  const allSelected = selectableItems.length > 0 && selectableItems.every((i) => selectedIds.has(i.id))
  const someSelected = selectedIds.size > 0

  async function handleDelete() {
    if (!await confirm('Excluir esta encomenda? Esta ação não pode ser desfeita.')) return
    withPin(() => deleteMutation.mutate())
  }

  const tabs: { value: DetailTab; label: string; count?: number }[] = [
    { value: 'itens', label: 'Itens', count: order.items.length },
    { value: 'pagamento', label: 'Pagamento' },
    { value: 'historico', label: 'Histórico' },
  ]

  return (
    <div className="p-4 sm:p-6">
      <button
        onClick={() => navigate('/orders')}
        className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 mb-4 cursor-pointer transition-colors"
      >
        <ArrowLeft size={15} /> Encomendas
      </button>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_300px] gap-4 items-start">
        {/* MAIN CARD */}
        <div className="bg-white border border-gray-150 rounded-2xl shadow-sm overflow-hidden">
          {/* header */}
          <div className="p-5 border-b border-gray-150">
            <div className="flex items-start gap-3">
              <p className="flex-1 min-w-0 font-bold text-gray-900 text-[17px]" title={order.customerName}>{order.customerName}</p>
              <OrderStatusBadge status={order.status} />
            </div>
            {order.observations && (
              <div className="mt-3 pt-3 border-t border-gray-100">
                <span className="text-xs font-semibold uppercase tracking-wide text-gray-400">Observações</span>
                <p className="text-sm text-gray-700 mt-1 break-words">{order.observations}</p>
              </div>
            )}
          </div>

          {/* tabs */}
          <div className="flex gap-1 px-4 border-b border-gray-150">
            {tabs.map((t) => (
              <button
                key={t.value}
                onClick={() => setTab(t.value)}
                className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors cursor-pointer ${
                  tab === t.value
                    ? 'border-brand-600 text-brand-700'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
              >
                {t.label}
                {t.count != null && (
                  <span className={`ml-1.5 text-xs px-1.5 py-0.5 rounded-full ${
                    tab === t.value ? 'bg-brand-600 text-white' : 'bg-gray-100 text-gray-500'
                  }`}>{t.count}</span>
                )}
              </button>
            ))}
          </div>

          {/* ===== ITENS TAB ===== */}
          {tab === 'itens' && (
            <div>
              {!isDelivered && (
                <div className="flex items-center justify-between px-4 py-3 border-b border-gray-150">
                  <span className="text-xs text-gray-400">{order.items.length} item(s)</span>
                  <Button variant="secondary" size="sm" onClick={openAddItem}>
                    <Plus size={12} /> Adicionar item
                  </Button>
                </div>
              )}

              {someSelected && (
                <div className="flex items-center gap-3 px-4 py-2 bg-brand-50 border-b border-brand-100 text-sm flex-wrap">
                  <button onClick={() => setSelectedIds(new Set())} className="text-brand-500 hover:text-brand-700 cursor-pointer">
                    <X size={14} />
                  </button>
                  <span className="text-brand-700 font-medium">{selectedIds.size} selecionado(s)</span>
                  <div className="flex items-center gap-2 ml-2 flex-wrap">
                    {order.items.some((i) => selectedIds.has(i.id) && i.status === 'PENDING') && (
                      <Button variant="secondary" size="sm" onClick={() => openDistributorPicker('bulk-selected')}>Marcar pedido</Button>
                    )}
                    {order.items.some((i) => selectedIds.has(i.id) && i.status === 'ORDERED') && (
                      <Button variant="secondary" size="sm" onClick={bulkMarkReceived}>Marcar recebido</Button>
                    )}
                    {order.items.some((i) => selectedIds.has(i.id) && i.status === 'RECEIVED') && (
                      <Button variant="secondary" size="sm" onClick={bulkMarkDelivered}>Marcar entregue</Button>
                    )}
                    <Button variant="danger" size="sm" onClick={bulkDelete}>
                      <Trash2 size={12} /> Excluir
                    </Button>
                  </div>
                </div>
              )}

              <div className="hidden md:block">
              <Table>
                <TableHead>
                  <tr>
                    {!isDelivered && (
                      <Th className="w-8">
                        <input type="checkbox" checked={allSelected} onChange={toggleSelectAll} className="accent-brand-600 cursor-pointer" />
                      </Th>
                    )}
                    <Th>Item</Th><Th>Categoria</Th><Th>Qtd.</Th><Th>Status</Th>
                    <Th>Distribuidora</Th><Th className="text-right">Preço un.</Th><Th />
                  </tr>
                </TableHead>
                <TableBody>
                  {order.items.length === 0 && (
                    <tr><Td colSpan={isDelivered ? 7 : 8} className="text-center text-gray-400 py-8">Nenhum item.</Td></tr>
                  )}
                  {order.items.map((item) => (
                    <Tr key={item.id}>
                      {!isDelivered && (
                        <Td>
                          {item.status !== 'DELIVERED' && (
                            <input type="checkbox" checked={selectedIds.has(item.id)} onChange={() => toggleSelectItem(item.id)} className="accent-brand-600 cursor-pointer" />
                          )}
                        </Td>
                      )}
                      <Td className="max-w-[260px] whitespace-normal">
                        <span className="font-medium text-gray-900 break-words">{item.product}</span>
                      </Td>
                      <Td><CategoryBadge category={item.category} /></Td>
                      <Td>{item.quantity}</Td>
                      <Td><OrderStatusBadge status={item.status} /></Td>
                      <Td className="text-gray-500">{item.distributorName ?? '—'}</Td>
                      <Td className="text-right font-mono text-gray-600">{item.price != null ? currencyFmt.format(item.price) : '—'}</Td>
                      <Td>
                        <ItemStatusActions
                          orderId={id!}
                          item={item}
                          onEdit={() => openEditItem(item)}
                          onSuccess={item.status === 'ORDERED' ? checkForNotification : invalidate}
                          onMarkOrdered={() => openDistributorPicker({ itemId: item.id })}
                        />
                      </Td>
                    </Tr>
                  ))}
                </TableBody>
              </Table>
              </div>

              <CardList>
                {order.items.length === 0 && <p className="text-center text-gray-400 py-8 text-sm">Nenhum item.</p>}
                {order.items.map((item) => (
                  <MobileCard key={item.id}>
                    <div className="flex items-start gap-2">
                      {!isDelivered && item.status !== 'DELIVERED' && (
                        <input type="checkbox" checked={selectedIds.has(item.id)} onChange={() => toggleSelectItem(item.id)} className="accent-brand-600 cursor-pointer mt-1 h-4 w-4 shrink-0" />
                      )}
                      <span className="font-medium text-gray-900 break-words min-w-0 flex-1">{item.product}</span>
                      <OrderStatusBadge status={item.status} />
                    </div>
                    <div className="flex items-center justify-between gap-3 text-sm">
                      <CategoryBadge category={item.category} />
                      <span className="text-gray-400">
                        Qtd: <span className="text-gray-700">{item.quantity}</span>
                        {item.price != null && <> · <span className="font-mono text-gray-600">{currencyFmt.format(item.price)}</span></>}
                      </span>
                    </div>
                    {item.distributorName && (
                      <div className="text-sm text-gray-400">Distribuidora: <span className="text-gray-700">{item.distributorName}</span></div>
                    )}
                    {item.status !== 'DELIVERED' && (
                      <div className="pt-1.5 mt-0.5 border-t border-gray-100">
                        <ItemStatusActions
                          mobile
                          orderId={id!}
                          item={item}
                          onEdit={() => openEditItem(item)}
                          onSuccess={item.status === 'ORDERED' ? checkForNotification : invalidate}
                          onMarkOrdered={() => openDistributorPicker({ itemId: item.id })}
                        />
                      </div>
                    )}
                  </MobileCard>
                ))}
              </CardList>
            </div>
          )}

          {/* ===== PAGAMENTO TAB ===== */}
          {tab === 'pagamento' && (
            <div>
              <div className="flex items-center justify-between px-5 py-4 border-b border-gray-150">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-500">Status de pagamento da encomenda:</span>
                  <OrderPaymentStatusBadge status={order.paymentStatus} />
                </div>
                {order.totalPrice != null && (
                  <div className="text-right">
                    <span className="block text-xs font-semibold uppercase tracking-wide text-gray-400">Valor total</span>
                    <span className="font-mono font-bold text-lg text-gray-900">{currencyFmt.format(order.totalPrice)}</span>
                  </div>
                )}
              </div>

              {someSelected && order.items.some((i) => selectedIds.has(i.id) && (i.paymentStatus === 'TO_PAY' || i.paymentStatus === 'MAKE_NOTE')) && (
                <div className="flex items-center gap-3 px-4 py-2 bg-brand-50 border-b border-brand-100 text-sm flex-wrap">
                  <button onClick={() => setSelectedIds(new Set())} className="text-brand-500 hover:text-brand-700 cursor-pointer">
                    <X size={14} />
                  </button>
                  <span className="text-brand-700 font-medium">{selectedIds.size} selecionado(s)</span>
                  <div className="flex items-center gap-2 ml-2 flex-wrap">
                    {order.items.some((i) => selectedIds.has(i.id) && (i.paymentStatus === 'TO_PAY' || i.paymentStatus === 'MAKE_NOTE')) && (
                      <Button variant="secondary" size="sm" onClick={bulkPayPaid}>Pago</Button>
                    )}
                    {order.items.some((i) => selectedIds.has(i.id) && i.paymentStatus === 'TO_PAY') && (
                      <Button variant="secondary" size="sm" onClick={bulkPayMakeNote}>Fazer nota</Button>
                    )}
                    {order.items.some((i) => selectedIds.has(i.id) && i.paymentStatus === 'MAKE_NOTE') && (
                      <>
                        <Button variant="secondary" size="sm" onClick={bulkPayToPay}>A pagar</Button>
                        <Button variant="secondary" size="sm" onClick={bulkPayNoted}>Anotado</Button>
                      </>
                    )}
                  </div>
                </div>
              )}

              <div className="hidden md:block">
              <Table>
                <TableHead>
                  <tr>
                    <Th className="w-8" />
                    <Th>Item</Th><Th className="text-right">Preço un.</Th><Th>Pagamento</Th><Th />
                  </tr>
                </TableHead>
                <TableBody>
                  {order.items.map((item) => (
                    <Tr key={item.id}>
                      <Td>
                        {(item.paymentStatus === 'TO_PAY' || item.paymentStatus === 'MAKE_NOTE') && (
                          <input type="checkbox" checked={selectedIds.has(item.id)} onChange={() => toggleSelectItem(item.id)} className="accent-brand-600 cursor-pointer" />
                        )}
                      </Td>
                      <Td className="max-w-[280px] whitespace-normal">
                        <span className="font-medium text-gray-900 break-words">{item.product}</span>
                      </Td>
                      <Td className="text-right font-mono text-gray-600">{item.price != null ? currencyFmt.format(item.price) : '—'}</Td>
                      <Td>
                        <OrderPaymentStatusBadge status={item.paymentStatus} />
                        {item.paymentChangedByName && (
                          <p className="text-[11px] text-gray-400 mt-0.5">
                            por {item.paymentChangedByName} · {formatDate(item.paymentChangedAt)}
                          </p>
                        )}
                      </Td>
                      <Td>
                        <ItemPaymentActions orderId={id!} item={item} onSuccess={invalidate} />
                      </Td>
                    </Tr>
                  ))}
                </TableBody>
              </Table>
              </div>

              <CardList>
                {order.items.map((item) => {
                  const selectable = item.paymentStatus === 'TO_PAY' || item.paymentStatus === 'MAKE_NOTE'
                  return (
                    <MobileCard key={item.id}>
                      <div className="flex items-start gap-2">
                        {selectable && (
                          <input type="checkbox" checked={selectedIds.has(item.id)} onChange={() => toggleSelectItem(item.id)} className="accent-brand-600 cursor-pointer mt-1 h-4 w-4 shrink-0" />
                        )}
                        <span className="font-medium text-gray-900 break-words min-w-0 flex-1">{item.product}</span>
                        <OrderPaymentStatusBadge status={item.paymentStatus} />
                      </div>
                      <div className="flex items-center justify-between gap-3 text-sm">
                        <span className="text-gray-400">Preço un.</span>
                        <span className="font-mono text-gray-600">{item.price != null ? currencyFmt.format(item.price) : '—'}</span>
                      </div>
                      {item.paymentChangedByName && (
                        <p className="text-[11px] text-gray-400">por {item.paymentChangedByName} · {formatDate(item.paymentChangedAt)}</p>
                      )}
                      {selectable && (
                        <div className="pt-1.5 mt-0.5 border-t border-gray-100">
                          <ItemPaymentActions mobile orderId={id!} item={item} onSuccess={invalidate} />
                        </div>
                      )}
                    </MobileCard>
                  )
                })}
              </CardList>
            </div>
          )}

          {/* ===== HISTÓRICO TAB ===== */}
          {tab === 'historico' && (
            <div className="p-5 space-y-5 text-sm">
              <div className="space-y-2">
                <HistoryRow label="Criada" by={order.createdByName} at={order.createdAt} />
                {order.notifiedAt && (
                  <div className="flex items-center gap-3">
                    <span className="w-24 text-gray-500 shrink-0">Notificado</span>
                    <span className="flex items-center gap-1.5 text-[#1da851] font-medium"><MessageCircle size={14} /> {formatDate(order.notifiedAt)}</span>
                  </div>
                )}
              </div>

              <div className="pt-4 border-t border-gray-150">
                <span className="text-xs font-semibold uppercase tracking-wide text-gray-500">Auditoria dos itens</span>
                <div className="mt-3 space-y-4">
                  {order.items.map((item) => (
                    <div key={item.id}>
                      <p className="font-semibold text-gray-900 mb-1 break-words">{item.product}</p>
                      <div className="flex flex-col gap-0.5 text-[13px] text-gray-600">
                        {item.orderedByName && <span><span className="text-gray-400">Pedido:</span> {item.orderedByName} · {formatDate(item.orderedAt)}</span>}
                        {item.receivedByName && <span><span className="text-gray-400">Recebido:</span> {item.receivedByName} · {formatDate(item.receivedAt)}</span>}
                        {item.deliveredByName && <span><span className="text-gray-400">Entregue:</span> {item.deliveredByName} · {formatDate(item.deliveredAt)}</span>}
                        {!item.orderedByName && !item.receivedByName && !item.deliveredByName && <span className="text-gray-400">Sem movimentações.</span>}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* SIDEBAR — AÇÕES RÁPIDAS */}
        <div className="bg-white border border-gray-150 rounded-2xl shadow-sm p-4">
          <p className="text-sm font-semibold text-gray-900 mb-3">Ações rápidas</p>
          <div className="flex flex-col gap-2">
            {!isDelivered ? (
              <>
                <Button variant="secondary" size="md" className="w-full" disabled={!hasPending} onClick={() => openDistributorPicker('bulk')}>
                  Marcar todos pedidos
                </Button>
                <Button variant="secondary" size="md" className="w-full" disabled={!hasOrdered || markAllReceived.isPending} onClick={() => withPin(() => markAllReceived.mutate())}>
                  Marcar todos recebidos
                </Button>
                <Button variant="secondary" size="md" className="w-full" disabled={!hasReceived || markAllDelivered.isPending} onClick={() => withPin(() => markAllDelivered.mutate())}>
                  Marcar todos entregues
                </Button>
              </>
            ) : (
              <div className="text-center text-sm font-medium text-brand-700 bg-brand-50 rounded-md py-2">Encomenda concluída</div>
            )}

            <div className="h-px bg-gray-100 my-1" />

            <Button variant="whatsapp" size="md" className="w-full" disabled={!order.notifiedAt} onClick={handleNotify}>
              <MessageCircle size={14} /> Notificar cliente
            </Button>

            {!isDelivered && (
              <>
                <Button variant="secondary" size="md" className="w-full" onClick={openEditOrder}>
                  <Settings2 size={14} /> Editar
                </Button>
                <Button
                  variant="ghost"
                  size="md"
                  className="w-full bg-red-50 text-red-600 border border-red-100 hover:bg-red-100"
                  onClick={handleDelete}
                  disabled={deleteMutation.isPending || hasDeliveredItem}
                  title={hasDeliveredItem ? 'Não é possível excluir: a encomenda tem itens entregues' : undefined}
                >
                  <Trash2 size={14} /> Excluir
                </Button>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Item add/edit dialog */}
      <Dialog
        open={itemDialogOpen}
        onOpenChange={(v) => !v && closeItemDialog()}
        title={editingItem ? 'Editar item' : 'Adicionar item'}
      >
        <form
          onSubmit={(e) => { e.preventDefault(); withPin(() => saveItemMutation.mutate()) }}
          className="space-y-3"
        >
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Item</label>
            <Input
              value={itemForm.product}
              onChange={(e) => setItemForm((p) => ({ ...p, product: e.target.value }))}
              required autoFocus autoComplete="off" maxLength={150}
            />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Categoria</label>
            <Select
              value={itemForm.category}
              onChange={(e) => setItemForm((p) => ({ ...p, category: e.target.value as Category }))}
            >
              {CATEGORY_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </Select>
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Quantidade</label>
            <Input
              type="number" min={1} max={1000} step={1}
              value={itemForm.quantity}
              onKeyDown={(e) => ['e', 'E', '+', '-', '.', ','].includes(e.key) && e.preventDefault()}
              onChange={(e) => {
                const val = e.target.value.replace(/[^0-9]/g, '')
                if (val && parseInt(val, 10) > 1000) return
                setItemForm((p) => ({ ...p, quantity: val }))
              }}
              required
            />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Preço unitário <span className="text-gray-400 font-normal">(opcional)</span>
            </label>
            <PriceInput value={itemForm.price} onChange={(v) => setItemForm((p) => ({ ...p, price: v }))} />
          </div>
          {saveItemMutation.isError && <ErrorMessage error={saveItemMutation.error} />}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={closeItemDialog}>Cancelar</Button>
            <Button type="submit" variant="primary" disabled={saveItemMutation.isPending || !itemForm.quantity}>
              {saveItemMutation.isPending ? 'Salvando...' : editingItem ? 'Salvar' : 'Adicionar'}
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Distributor picker dialog */}
      <Dialog
        open={distributorPicker !== null}
        onOpenChange={(v) => {
          if (!v) {
            setDistributorPicker(null)
            setPickedDistributor(null)
          }
        }}
        title="Selecionar distribuidora"
        description="Escolha a distribuidora para este pedido"
      >
        <div className="space-y-4">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Distribuidora</label>
            <DistributorSearch value={pickedDistributor} onChange={setPickedDistributor} />
          </div>
          <div className="flex justify-end gap-2 pt-1">
            <Button
              type="button" variant="secondary"
              onClick={() => { setDistributorPicker(null); setPickedDistributor(null) }}
            >
              Cancelar
            </Button>
            <Button
              type="button" variant="primary"
              disabled={!pickedDistributor || markAllOrdered.isPending}
              onClick={() => {
                if (!pickedDistributor) return
                if (distributorPicker === 'bulk') {
                  withPin(() => markAllOrdered.mutate(pickedDistributor.id))
                } else if (distributorPicker === 'bulk-selected') {
                  bulkMarkOrdered(pickedDistributor.id)
                } else if (distributorPicker && typeof distributorPicker === 'object') {
                  withPin(() => markItemOrderedWithDistributor(distributorPicker.itemId, pickedDistributor.id))
                }
              }}
            >
              {markAllOrdered.isPending ? 'Salvando...' : 'Confirmar'}
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Edit order dialog */}
      <Dialog
        open={editOrderOpen}
        onOpenChange={(v) => !v && setEditOrderOpen(false)}
        title="Editar encomenda"
      >
        <form
          onSubmit={async (e) => {
            e.preventDefault()
            const hasItemPrices = order?.items.some((i) => i.price != null) ?? false
            const settingManualTotal = parsePriceInput(editTotalPrice) != null
            if (settingManualTotal && hasItemPrices) {
              if (!await confirm('Os preços dos itens serão removidos para usar o preço total manual. Deseja continuar?')) return
            }
            withPin(() => updateOrderMutation.mutate())
          }}
          className="space-y-3"
        >
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Observações <span className="text-gray-400 font-normal">(opcional)</span>
            </label>
            <Input
              value={editObservations}
              onChange={(e) => setEditObservations(e.target.value)}
              placeholder="Observações sobre a encomenda..."
              maxLength={500} autoComplete="off"
            />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Preço total <span className="text-gray-400 font-normal">(opcional — usado quando não há preço por item)</span>
            </label>
            <PriceInput value={editTotalPrice} onChange={setEditTotalPrice} />
          </div>
          {(order.paymentStatus === 'TO_PAY' || order.paymentStatus === 'MAKE_NOTE') && (
            <div>
              <label className="text-xs font-medium text-gray-700 block mb-1">Status de pagamento</label>
              <Select value={editPaymentStatus} onChange={(e) => setEditPaymentStatus(e.target.value)}>
                <option value="">Manter atual</option>
                {order.paymentStatus === 'TO_PAY' && (
                  <>
                    <option value="PAID">Pago</option>
                    <option value="MAKE_NOTE">Fazer nota</option>
                  </>
                )}
                {order.paymentStatus === 'MAKE_NOTE' && (
                  <>
                    <option value="PAID">Pago</option>
                    <option value="TO_PAY">A pagar</option>
                    <option value="NOTED">Anotado</option>
                  </>
                )}
              </Select>
            </div>
          )}
          {updateOrderMutation.isError && <ErrorMessage error={updateOrderMutation.error} />}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setEditOrderOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="primary" disabled={updateOrderMutation.isPending}>
              {updateOrderMutation.isPending ? 'Salvando...' : 'Salvar'}
            </Button>
          </div>
        </form>
      </Dialog>

      {popup && <NotificationPopup notification={popup} onClose={() => setPopup(null)} />}
    </div>
  )
}

function HistoryRow({ label, by, at }: { label: string; by: string | null | undefined; at: string | null | undefined }) {
  return (
    <div className="flex items-center gap-3">
      <span className="w-24 text-gray-500 shrink-0">{label}</span>
      <span className="text-gray-900 font-medium">{by ?? '—'}</span>
      <span className="text-gray-500 text-[13px]">{formatDate(at)}</span>
    </div>
  )
}

function ItemStatusActions({
  orderId,
  item,
  onEdit,
  onSuccess,
  onMarkOrdered,
  mobile = false,
}: {
  orderId: string
  item: OrderItem
  onEdit: () => void
  onSuccess: () => void
  onMarkOrdered: () => void
  mobile?: boolean
}) {
  const withPin = useWithPin()
  const confirm = useConfirm()

  const deleteMutation = useMutation({
    mutationFn: () => deleteOrderItem(orderId, item.id),
    onSuccess,
  })

  const advanceMutation = useMutation({
    mutationFn: () => {
      if (item.status === 'ORDERED') return markItemAsReceived(orderId, item.id)
      return markItemAsDelivered(orderId, item.id)
    },
    onSuccess,
  })

  async function handleDelete() {
    if (!await confirm('Remover este item da encomenda?')) return
    withPin(() => deleteMutation.mutate())
  }

  const textBtn = mobile ? 'h-11 px-3' : ''
  const iconBtn = mobile ? 'h-11 w-11' : ''
  const iconSize = mobile ? 17 : 12

  return (
    <div className="flex items-center gap-1 justify-end whitespace-nowrap">
      {item.status === 'PENDING' && (
        <Button variant="ghost" size="sm" className={textBtn} onClick={onMarkOrdered}>Marcar pedido</Button>
      )}
      {item.status === 'ORDERED' && (
        <Button variant="ghost" size="sm" className={textBtn} onClick={() => withPin(() => advanceMutation.mutate())} disabled={advanceMutation.isPending}>
          Marcar recebido
        </Button>
      )}
      {item.status === 'RECEIVED' && (
        <Button variant="ghost" size="sm" className={textBtn} onClick={async () => {
          if (!await confirm(`Marcar "${item.product}" como entregue? Esta ação não pode ser desfeita.`)) return
          withPin(() => advanceMutation.mutate())
        }} disabled={advanceMutation.isPending}>
          Marcar entregue
        </Button>
      )}

      {item.status !== 'DELIVERED' && (
        <div className="flex items-center gap-1 border-l border-gray-200 pl-2 ml-1">
          <Button variant="ghost" size="sm" className={iconBtn} onClick={onEdit}><Pencil size={iconSize} /></Button>
          <Button variant="ghost" size="sm" onClick={handleDelete} disabled={deleteMutation.isPending} className={cn('text-red-400 hover:text-red-600', iconBtn)}>
            <Trash2 size={iconSize} />
          </Button>
        </div>
      )}
    </div>
  )
}

function ItemPaymentActions({ orderId, item, onSuccess, mobile = false }: { orderId: string; item: OrderItem; onSuccess: () => void; mobile?: boolean }) {
  const withPin = useWithPin()

  const payPaidMutation = useMutation({ mutationFn: () => markItemPaymentAsPaid(orderId, item.id), onSuccess })
  const payMakeNoteMutation = useMutation({ mutationFn: () => markItemPaymentAsMakeNote(orderId, item.id), onSuccess })
  const payNotedMutation = useMutation({ mutationFn: () => markItemPaymentAsNoted(orderId, item.id), onSuccess })
  const payToPayMutation = useMutation({ mutationFn: () => markItemPaymentAsToPay(orderId, item.id), onSuccess })

  const isPaymentFinal = item.paymentStatus === 'PAID' || item.paymentStatus === 'NOTED'
  if (isPaymentFinal) return null

  const textBtn = mobile ? 'h-11 px-3' : ''

  return (
    <div className="flex items-center gap-1 justify-end whitespace-nowrap">
      {item.paymentStatus === 'TO_PAY' && (
        <>
          <Button variant="ghost" size="sm" className={textBtn} onClick={() => withPin(() => payPaidMutation.mutate())} disabled={payPaidMutation.isPending}>Pago</Button>
          <Button variant="ghost" size="sm" className={textBtn} onClick={() => withPin(() => payMakeNoteMutation.mutate())} disabled={payMakeNoteMutation.isPending}>Fazer nota</Button>
        </>
      )}
      {item.paymentStatus === 'MAKE_NOTE' && (
        <>
          <Button variant="ghost" size="sm" className={textBtn} onClick={() => withPin(() => payToPayMutation.mutate())} disabled={payToPayMutation.isPending}>A pagar</Button>
          <Button variant="ghost" size="sm" className={textBtn} onClick={() => withPin(() => payPaidMutation.mutate())} disabled={payPaidMutation.isPending}>Pago</Button>
          <Button variant="ghost" size="sm" className={textBtn} onClick={() => withPin(() => payNotedMutation.mutate())} disabled={payNotedMutation.isPending}>Anotado</Button>
        </>
      )}
    </div>
  )
}
