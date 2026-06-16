import { useState } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Trash2, Pencil, CheckCircle, Settings2, X } from 'lucide-react'
import { getShortageOrder, updateShortageOrder, deleteShortageOrder, markShortageOrderAsOrdered } from '@/api/shortageOrders'
import { updateShortage, deleteShortage, markShortageAsOrdered } from '@/api/shortages'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Spinner } from '@/components/ui/spinner'
import { Dialog } from '@/components/ui/dialog'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { ShortageOrderStatusBadge, ShortageStatusBadge } from '@/components/shared/StatusBadge'
import { CategoryBadge, CATEGORY_OPTIONS } from '@/components/shared/CategoryBadge'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { PriceInput, parsePriceInput } from '@/components/shared/PriceInput'
import { DistributorSearch } from '@/components/shared/DistributorSearch'
import { AuditButton } from '@/components/shared/AuditButton'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { formatDate } from '@/lib/utils'
import type { Category, Shortage, Distributor } from '@/types'

const SHORTAGE_TYPE_LABEL: Record<string, string> = {
  WANIA: 'Wania',
  FRANCISCO: 'Francisco',
}

const currencyFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

interface EditForm { product: string; category: Category; quantity: string; costPrice: string }
const emptyEdit = (s: Shortage): EditForm => ({
  product: s.product,
  category: s.category,
  quantity: s.quantity != null ? String(s.quantity) : '',
  costPrice: s.costPrice != null ? String(s.costPrice).replace('.', ',') : '',
})

export function ShortageOrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const qc = useQueryClient()
  const withPin = useWithPin()
  const confirm = useConfirm()

  const [editingShortage, setEditingShortage] = useState<Shortage | null>(null)
  const [editForm, setEditForm] = useState<EditForm | null>(null)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [editingOrder, setEditingOrder] = useState(false)
  const [orderDistributor, setOrderDistributor] = useState<Distributor | null>(null)
  const [orderObservations, setOrderObservations] = useState('')

  const { data: order, isLoading, error } = useQuery({
    queryKey: ['shortage-order', id],
    queryFn: () => getShortageOrder(id!),
    enabled: !!id,
  })

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['shortage-order', id] })
    if (order) {
      qc.invalidateQueries({ queryKey: ['shortage-orders', order.shortageType] })
      qc.invalidateQueries({ queryKey: ['shortages-all'] })
    }
  }

  function handleBack() {
    const from = (location.state as { from?: string } | null)?.from
    if (from) navigate(from)
    else navigate(-1)
  }

  const deleteMutation = useMutation({
    mutationFn: () => deleteShortageOrder(id!),
    onSuccess: handleBack,
  })

  const markOrderedMutation = useMutation({
    mutationFn: () => markShortageOrderAsOrdered(id!),
    onSuccess: invalidate,
  })

  const updateOrderMutation = useMutation({
    mutationFn: () => updateShortageOrder(id!, {
      distributorId: orderDistributor!.id,
      observations: orderObservations.trim() || null,
    }),
    onSuccess: () => { setEditingOrder(false); invalidate() },
  })

  const editShortageMutation = useMutation({
    mutationFn: () =>
      updateShortage(editingShortage!.id, {
        product: editForm!.product,
        category: editForm!.category,
        quantity: editForm!.quantity ? Number(editForm!.quantity) : null,
        costPrice: parsePriceInput(editForm!.costPrice),
        shortageType: order!.shortageType,
      }),
    onSuccess: () => { setEditingShortage(null); setEditForm(null); invalidate() },
  })

  const deleteShortageMutation = useMutation({
    mutationFn: (shortageId: string) => deleteShortage(shortageId),
    onSuccess: invalidate,
  })

  const markShortageOrderedMutation = useMutation({
    mutationFn: (shortageId: string) => markShortageAsOrdered(shortageId),
    onSuccess: invalidate,
  })

  async function handleDeleteOrder() {
    if (!await confirm('Excluir este pedido? As faltas associadas também serão excluídas.')) return
    withPin(() => deleteMutation.mutate())
  }

  async function handleMarkOrdered() {
    if (!await confirm(`Marcar pedido da ${order?.distributorName} como pedido? As faltas serão marcadas como pedidas.`)) return
    withPin(() => markOrderedMutation.mutate())
  }

  async function handleDeleteShortage(s: Shortage) {
    if (!await confirm(`Excluir a falta "${s.product}"?`)) return
    withPin(() => deleteShortageMutation.mutate(s.id))
  }

  async function handleMarkShortageOrdered(s: Shortage) {
    if (!await confirm(`Marcar "${s.product}" como pedido?`)) return
    withPin(() => markShortageOrderedMutation.mutate(s.id))
  }

  function openEdit(s: Shortage) {
    setEditingShortage(s)
    setEditForm(emptyEdit(s))
    editShortageMutation.reset()
  }

  function openEditOrder() {
    setOrderDistributor(order ? { id: order.distributorId, name: order.distributorName } as Distributor : null)
    setOrderObservations(order?.observations ?? '')
    updateOrderMutation.reset()
    setEditingOrder(true)
  }

  async function bulkMarkOrdered() {
    const targets = (order?.shortages ?? []).filter((s) => selectedIds.has(s.id) && s.status === 'PENDING')
    if (!targets.length) return
    if (!await confirm(`Marcar ${targets.length} falta(s) como pedido?`)) return
    withPin(async () => {
      for (const s of targets) await markShortageAsOrdered(s.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  async function bulkDeleteShortages() {
    const targets = (order?.shortages ?? []).filter((s) => selectedIds.has(s.id) && s.status === 'PENDING')
    if (!targets.length) return
    if (!await confirm(`Excluir ${targets.length} falta(s)?`)) return
    withPin(async () => {
      for (const s of targets) await deleteShortage(s.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  if (isLoading) return <div className="flex justify-center py-20"><Spinner /></div>
  if (error) return <div className="p-6"><ErrorMessage error={error} /></div>
  if (!order) return null

  const isPending = order.status === 'PENDING'
  const caderno = SHORTAGE_TYPE_LABEL[order.shortageType] ?? order.shortageType
  const selectablePending = order.shortages.filter((s) => s.status === 'PENDING')
  const allSelected = selectablePending.length > 0 && selectablePending.every((s) => selectedIds.has(s.id))
  const someSelected = selectedIds.size > 0

  return (
    <div>
      <PageHeader
        title={`Pedido de falta — ${order.distributorName}`}
        description={`Caderno de ${caderno} · Criado por ${order.createdByName} em ${formatDate(order.createdAt)}`}
        actions={
          <div className="flex items-center gap-2 flex-wrap">
            <Button variant="ghost" size="sm" onClick={handleBack}>
              <ArrowLeft size={13} /> Voltar
            </Button>
            {isPending && (
              <div className="flex items-center gap-2 border-l border-gray-200 pl-3">
                <Button variant="ghost" size="sm" onClick={openEditOrder}>
                  <Settings2 size={13} /> Editar
                </Button>
                <Button variant="secondary" size="sm" onClick={handleMarkOrdered} disabled={markOrderedMutation.isPending}>
                  {markOrderedMutation.isPending ? 'Salvando...' : 'Marcar como pedido'}
                </Button>
                <Button variant="danger" size="sm" onClick={handleDeleteOrder} disabled={deleteMutation.isPending}>
                  <Trash2 size={13} /> Excluir
                </Button>
              </div>
            )}
          </div>
        }
      />

      <div className="p-6 space-y-4">
        <div className="bg-white border border-gray-200 rounded-lg p-4 grid grid-cols-2 gap-x-8 gap-y-3 text-sm sm:grid-cols-3 lg:grid-cols-4">
          <div>
            <span className="text-gray-500 text-xs uppercase tracking-wide">Status</span>
            <div className="mt-1"><ShortageOrderStatusBadge status={order.status} /></div>
          </div>
          <div>
            <span className="text-gray-500 text-xs uppercase tracking-wide">Distribuidora</span>
            <p className="mt-1 font-medium text-gray-900">{order.distributorName}</p>
          </div>
          <div>
            <span className="text-gray-500 text-xs uppercase tracking-wide">Caderno</span>
            <p className="mt-1 font-medium text-gray-900">{caderno}</p>
          </div>
          {order.orderedAt && (
            <div>
              <span className="text-gray-500 text-xs uppercase tracking-wide">Pedido por</span>
              <p className="mt-1 text-gray-700">{order.orderedByName} em {formatDate(order.orderedAt)}</p>
            </div>
          )}
          {order.observations && (
            <div className="col-span-2 lg:col-span-4">
              <span className="text-gray-500 text-xs uppercase tracking-wide">Observações</span>
              <p className="mt-1 text-gray-700">{order.observations}</p>
            </div>
          )}
        </div>

        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          <div className="px-4 py-3 border-b border-gray-200">
            <span className="text-sm font-medium text-gray-900">
              Itens em falta ({order.shortages.length})
            </span>
          </div>

          {someSelected && (
            <div className="flex items-center gap-3 px-4 py-2 bg-brand-50 border-b border-brand-100 text-sm">
              <button onClick={() => setSelectedIds(new Set())} className="text-brand-500 hover:text-brand-700 cursor-pointer">
                <X size={14} />
              </button>
              <span className="text-brand-700 font-medium">{selectedIds.size} selecionado(s)</span>
              <div className="flex items-center gap-2 ml-2">
                <Button variant="secondary" size="sm" onClick={bulkMarkOrdered}>Marcar como pedido</Button>
                <Button variant="danger" size="sm" onClick={bulkDeleteShortages}>
                  <Trash2 size={12} /> Excluir
                </Button>
              </div>
            </div>
          )}

          <Table>
            <TableHead>
              <tr>
                {isPending && (
                  <Th className="w-8">
                    <input
                      type="checkbox"
                      checked={allSelected}
                      onChange={() => {
                        if (allSelected) setSelectedIds(new Set())
                        else setSelectedIds(new Set(selectablePending.map((s) => s.id)))
                      }}
                      className="accent-gray-700 cursor-pointer"
                    />
                  </Th>
                )}
                <Th>Item</Th>
                <Th>Categoria</Th>
                <Th>Qtd.</Th>
                <Th>Preço custo</Th>
                <Th>Status</Th>
                <Th>Data</Th>
                <Th />
              </tr>
            </TableHead>
            <TableBody>
              {order.shortages.length === 0 && (
                <tr>
                  <Td colSpan={isPending ? 8 : 7} className="text-center text-gray-400 py-8">Nenhum item neste pedido.</Td>
                </tr>
              )}
              {order.shortages.map((s) => (
                <Tr key={s.id}>
                  {isPending && (
                    <Td>
                      {s.status === 'PENDING' && (
                        <input
                          type="checkbox"
                          checked={selectedIds.has(s.id)}
                          onChange={() => setSelectedIds((prev) => {
                            const next = new Set(prev)
                            next.has(s.id) ? next.delete(s.id) : next.add(s.id)
                            return next
                          })}
                          className="accent-gray-700 cursor-pointer"
                        />
                      )}
                    </Td>
                  )}
                  <Td className="max-w-[240px] whitespace-normal">
                    <span className="font-medium text-gray-900 break-words">
                      {s.product}
                    </span>
                  </Td>
                  <Td><CategoryBadge category={s.category} /></Td>
                  <Td>{s.quantity ?? '—'}</Td>
                  <Td className="text-gray-500 font-mono">
                    {s.costPrice != null ? currencyFmt.format(s.costPrice) : '—'}
                  </Td>
                  <Td><ShortageStatusBadge status={s.status} /></Td>
                  <Td className="text-gray-500">{formatDate(s.createdAt)}</Td>
                  <Td>
                    <div className="flex items-center gap-1 justify-end whitespace-nowrap">
                      {s.status === 'PENDING' && (
                        <>
                          <Button variant="ghost" size="sm" className="text-blue-500 hover:text-blue-700"
                            onClick={() => handleMarkShortageOrdered(s)}
                            disabled={markShortageOrderedMutation.isPending}>
                            <CheckCircle size={12} /> Pedido
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => openEdit(s)}>
                            <Pencil size={12} />
                          </Button>
                          <Button variant="ghost" size="sm" className="text-red-400 hover:text-red-600"
                            onClick={() => handleDeleteShortage(s)}
                            disabled={deleteShortageMutation.isPending}>
                            <Trash2 size={12} />
                          </Button>
                        </>
                      )}
                      <AuditButton rows={[
                        { label: 'Registrado', value: `${s.createdByName} · ${formatDate(s.createdAt)}` },
                        { label: 'Pedido', value: s.orderedByName ? `${s.orderedByName} · ${formatDate(s.orderedAt)}` : '—' },
                      ]} />
                    </div>
                  </Td>
                </Tr>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>

      <Dialog
        open={editingOrder}
        onOpenChange={(v) => !v && setEditingOrder(false)}
        title="Editar pedido"
      >
        <form
          onSubmit={(e) => { e.preventDefault(); withPin(() => updateOrderMutation.mutate()) }}
          className="space-y-3"
        >
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Distribuidora</label>
            <DistributorSearch value={orderDistributor} onChange={setOrderDistributor} allowCreate={false} />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Observações <span className="text-gray-400 font-normal">(opcional)</span>
            </label>
            <Input
              value={orderObservations}
              onChange={(e) => setOrderObservations(e.target.value)}
              placeholder="Ex: pedir com urgência"
              maxLength={500}
            />
          </div>
          {updateOrderMutation.isError && <ErrorMessage error={updateOrderMutation.error} />}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setEditingOrder(false)}>Cancelar</Button>
            <Button type="submit" variant="primary" disabled={updateOrderMutation.isPending || !orderDistributor}>
              {updateOrderMutation.isPending ? 'Salvando...' : 'Salvar'}
            </Button>
          </div>
        </form>
      </Dialog>

      <Dialog
        open={!!editingShortage}
        onOpenChange={(v) => !v && setEditingShortage(null)}
        title="Editar falta"
      >
        {editForm && (
          <form
            onSubmit={(e) => { e.preventDefault(); withPin(() => editShortageMutation.mutate()) }}
            className="space-y-3"
          >
            <div>
              <label className="text-xs font-medium text-gray-700 block mb-1">Item</label>
              <Input value={editForm.product}
                onChange={(e) => setEditForm((p) => p && { ...p, product: e.target.value })}
                maxLength={150} required autoFocus autoComplete="off" />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-700 block mb-1">Categoria</label>
              <Select value={editForm.category}
                onChange={(e) => setEditForm((p) => p && { ...p, category: e.target.value as Category })}>
                {CATEGORY_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
              </Select>
            </div>
            <div className="flex gap-3">
              <div className="flex-1">
                <label className="text-xs font-medium text-gray-700 block mb-1">
                  Quantidade <span className="text-gray-400 font-normal">(opcional)</span>
                </label>
                <Input type="number" min={1} max={999} step={1} value={editForm.quantity}
                  onKeyDown={(e) => ['e', 'E', '+', '-', '.', ','].includes(e.key) && e.preventDefault()}
                  onChange={(e) => {
                    const val = e.target.value.replace(/[^0-9]/g, '')
                    if (val && parseInt(val, 10) > 999) return
                    setEditForm((p) => p && { ...p, quantity: val })
                  }}
                  placeholder="—" />
              </div>
              <div className="flex-1">
                <label className="text-xs font-medium text-gray-700 block mb-1">
                  Preço custo <span className="text-gray-400 font-normal">(opcional)</span>
                </label>
                <PriceInput value={editForm.costPrice}
                  onChange={(v) => setEditForm((p) => p && { ...p, costPrice: v })} />
              </div>
            </div>
            {editShortageMutation.isError && <ErrorMessage error={editShortageMutation.error} />}
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="secondary" onClick={() => setEditingShortage(null)}>Cancelar</Button>
              <Button type="submit" variant="primary" disabled={editShortageMutation.isPending}>
                {editShortageMutation.isPending ? 'Salvando...' : 'Salvar'}
              </Button>
            </div>
          </form>
        )}
      </Dialog>
    </div>
  )
}
