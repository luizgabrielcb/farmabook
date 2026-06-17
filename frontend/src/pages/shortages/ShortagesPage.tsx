import { useState, useMemo, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, CheckCircle, Search, X, Package } from 'lucide-react'
import { listShortages, createShortage, updateShortage, deleteShortage, markShortageAsOrdered } from '@/api/shortages'
import { listShortageOrders, deleteShortageOrder, markShortageOrderAsOrdered } from '@/api/shortageOrders'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { CardList, MobileCard, CardActions, IconAction, CardEmpty } from '@/components/ui/mobile-card'
import { Spinner } from '@/components/ui/spinner'
import { Pagination } from '@/components/shared/Pagination'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { Dialog } from '@/components/ui/dialog'
import { ShortageStatusBadge, ShortageOrderStatusBadge } from '@/components/shared/StatusBadge'
import { CategoryBadge, CATEGORY_OPTIONS } from '@/components/shared/CategoryBadge'
import { DistributorSearch } from '@/components/shared/DistributorSearch'
import { AuditButton } from '@/components/shared/AuditButton'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { useToast } from '@/context/ToastContext'
import { CreateShortageOrderDialog } from './CreateShortageOrderDialog'
import { formatDate, parseLocalDate } from '@/lib/utils'
import type { Distributor, Shortage, Category, ShortageStatus, ShortageType } from '@/types'

interface FormState { product: string; category: Category; quantity: string }
const emptyForm: FormState = { product: '', category: 'MEDICAMENTOS', quantity: '' }
const PAGE_SIZE = 20

const STATUS_OPTIONS: { value: ShortageStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'Todos' },
  { value: 'PENDING', label: 'Pendente' },
  { value: 'ORDERED', label: 'Pedido' },
]

const CATEGORY_FILTER_OPTIONS = [
  { value: 'ALL', label: 'Todas' },
  ...CATEGORY_OPTIONS.map((o) => ({ value: o.value, label: o.label })),
]

type InnerTab = 'FALTAS' | 'PEDIDOS'

interface TabPanelProps {
  shortageType: ShortageType
  label: string
  activeView: InnerTab
  onViewChange: (v: InnerTab) => void
  createOpen: boolean
  setCreateOpen: (v: boolean) => void
}

function ShortageTab({ shortageType, label, activeView, onViewChange, createOpen, setCreateOpen }: TabPanelProps) {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex gap-1">
          {(['FALTAS', 'PEDIDOS'] as InnerTab[]).map((tab) => (
            <button
              key={tab}
              onClick={() => onViewChange(tab)}
              className={`px-4 py-1.5 text-xs font-medium rounded-full border transition-colors cursor-pointer ${
                activeView === tab
                  ? 'bg-brand-600 text-white border-brand-600'
                  : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'
              }`}
            >
              {tab === 'FALTAS' ? 'Faltas' : 'Pedidos'}
            </button>
          ))}
        </div>
      </div>

      {activeView === 'FALTAS'
        ? <FaltasPanel shortageType={shortageType} label={label} createOpen={createOpen} setCreateOpen={setCreateOpen} />
        : <PedidosPanel shortageType={shortageType} label={label} createOpen={createOpen} setCreateOpen={setCreateOpen} />
      }
    </div>
  )
}

interface PanelProps { shortageType: ShortageType; label: string; createOpen: boolean; setCreateOpen: (v: boolean) => void }

function FaltasPanel({ shortageType, label, createOpen, setCreateOpen }: PanelProps) {
  const [statusFilter, setStatusFilter] = useState<ShortageStatus | 'ALL'>('ALL')
  const [categoryFilter, setCategoryFilter] = useState<Category | 'ALL'>('ALL')
  const [query, setQuery] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Shortage | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const dialogOpen = createOpen || editing !== null
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const withPin = useWithPin()
  const confirm = useConfirm()
  const toast = useToast()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['shortages-all'],
    queryFn: () => listShortages(0, 500),
  })

  const filtered = useMemo(() => {
    let items = (data?.content ?? []).filter((s) => s.shortageType === shortageType)
    if (statusFilter !== 'ALL') items = items.filter((s) => s.status === statusFilter)
    if (categoryFilter !== 'ALL') items = items.filter((s) => s.category === categoryFilter)
    if (query.trim()) items = items.filter((s) => s.product.toLowerCase().includes(query.toLowerCase()))
    if (dateFrom) { const from = parseLocalDate(dateFrom); items = items.filter((s) => new Date(s.createdAt) >= from) }
    if (dateTo) { const to = parseLocalDate(dateTo); to.setHours(23, 59, 59, 999); items = items.filter((s) => new Date(s.createdAt) <= to) }
    return items
  }, [data, shortageType, statusFilter, categoryFilter, query, dateFrom, dateTo])

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paged = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  const invalidate = () => qc.invalidateQueries({ queryKey: ['shortages-all'] })

  const saveMutation = useMutation({
    mutationFn: () =>
      editing
        ? updateShortage(editing.id, { ...form, quantity: form.quantity ? Number(form.quantity) : null, shortageType, costPrice: null })
        : createShortage({ ...form, quantity: form.quantity ? Number(form.quantity) : null, shortageType }),
    onSuccess: () => { toast.success(editing ? 'Alterações salvas' : 'Falta registrada'); closeDialog(); invalidate() },
  })

  const deleteMutation = useMutation({ mutationFn: deleteShortage, onSuccess: () => { toast.success('Falta excluída'); invalidate() } })
  const markMutation = useMutation({ mutationFn: markShortageAsOrdered, onSuccess: () => { toast.success('Marcado como pedido'); invalidate() } })

  // Reset the form when the create dialog is opened from the page header
  useEffect(() => {
    if (createOpen) { setEditing(null); setForm(emptyForm); saveMutation.reset() }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [createOpen])

  function openEdit(s: Shortage) {
    setEditing(s)
    setForm({ product: s.product, category: s.category, quantity: s.quantity != null ? String(s.quantity) : '' })
    saveMutation.reset()
  }
  function closeDialog() { setCreateOpen(false); setEditing(null); setForm(emptyForm) }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    withPin(() => saveMutation.mutate())
  }

  async function handleMarkOrdered(s: Shortage) {
    if (!await confirm(`Marcar "${s.product}" como pedido?`)) return
    withPin(() => markMutation.mutate(s.id))
  }

  async function handleDelete(s: Shortage) {
    if (!await confirm(`Excluir a falta "${s.product}"?`)) return
    withPin(() => deleteMutation.mutate(s.id))
  }

  const selectablePending = paged.filter((s) => s.status === 'PENDING' && !s.shortageOrderId)
  const allSelected = selectablePending.length > 0 && selectablePending.every((s) => selectedIds.has(s.id))
  const someSelected = selectedIds.size > 0

  async function bulkMarkOrdered() {
    const targets = paged.filter((s) => selectedIds.has(s.id) && s.status === 'PENDING')
    if (!targets.length) return
    if (!await confirm(`Marcar ${targets.length} falta(s) como pedido?`)) return
    withPin(async () => {
      for (const s of targets) await markShortageAsOrdered(s.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  async function bulkDelete() {
    const targets = paged.filter((s) => selectedIds.has(s.id) && s.status === 'PENDING')
    if (!targets.length) return
    if (!await confirm(`Excluir ${targets.length} falta(s)?`)) return
    withPin(async () => {
      for (const s of targets) await deleteShortage(s.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  const hasFilter = statusFilter !== 'ALL' || categoryFilter !== 'ALL' || query || dateFrom || dateTo

  return (
    <div className="space-y-4">
      <div className="bg-white border border-gray-200 rounded-lg p-4 space-y-3">
        <div>
          <p className="text-xs text-gray-400 mb-1.5">Status</p>
          <div className="flex items-center gap-2">
            {STATUS_OPTIONS.map((s) => (
              <button key={s.value} onClick={() => { setStatusFilter(s.value); setPage(0) }}
                className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors cursor-pointer ${
                  statusFilter === s.value
                    ? 'bg-brand-600 text-white border-brand-600'
                    : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'
                }`}>
                {s.label}
              </button>
            ))}
          </div>
        </div>

        <div>
          <p className="text-xs text-gray-400 mb-1.5">Categoria</p>
          <div className="flex items-center gap-2 flex-wrap">
            {CATEGORY_FILTER_OPTIONS.map((c) => (
              <button key={c.value} onClick={() => { setCategoryFilter(c.value as Category | 'ALL'); setPage(0) }}
                className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors cursor-pointer ${
                  categoryFilter === c.value
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'
                }`}>
                {c.label}
              </button>
            ))}
          </div>
        </div>

        <div className="flex gap-3 flex-wrap pt-1 border-t border-gray-100">
          <div className="relative flex-1 min-w-48">
            <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
            <Input value={query} onChange={(e) => { setQuery(e.target.value); setPage(0) }}
              placeholder="Buscar item..." className="pl-8" autoComplete="off" />
          </div>
          <div className="flex items-center gap-2">
            <Input type="date" value={dateFrom} onChange={(e) => { setDateFrom(e.target.value); setPage(0) }} className="w-36" />
            <span className="text-gray-400 text-xs">até</span>
            <Input type="date" value={dateTo} onChange={(e) => { setDateTo(e.target.value); setPage(0) }} className="w-36" />
          </div>
          {hasFilter && (
            <Button variant="ghost" size="sm"
              onClick={() => { setStatusFilter('ALL'); setCategoryFilter('ALL'); setQuery(''); setDateFrom(''); setDateTo(''); setPage(0) }}>
              <X size={12} /> Limpar
            </Button>
          )}
        </div>
      </div>

      <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-12"><Spinner /></div>
        ) : (
          <>
            {someSelected && (
              <div className="flex items-center gap-3 px-4 py-2 bg-brand-50 border-b border-brand-100 text-sm">
                <button onClick={() => setSelectedIds(new Set())} className="text-brand-500 hover:text-brand-700 cursor-pointer">
                  <X size={14} />
                </button>
                <span className="text-brand-700 font-medium">{selectedIds.size} selecionado(s)</span>
                <div className="flex items-center gap-2 ml-2">
                  <Button variant="secondary" size="sm" onClick={bulkMarkOrdered}>Marcar como pedido</Button>
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
                  <Th className="w-8">
                    {selectablePending.length > 0 && (
                      <input
                        type="checkbox"
                        checked={allSelected}
                        onChange={() => {
                          if (allSelected) setSelectedIds(new Set())
                          else setSelectedIds(new Set(selectablePending.map((s) => s.id)))
                        }}
                        className="accent-gray-700 cursor-pointer"
                      />
                    )}
                  </Th>
                  <Th>Item</Th><Th>Categoria</Th><Th>Qtd.</Th><Th>Status</Th>
                  <Th>Data</Th><Th />
                </tr>
              </TableHead>
              <TableBody>
                {paged.length === 0 && (
                  <tr>
                    <Td colSpan={7} className="text-center text-gray-400 py-10">
                      {hasFilter ? 'Nenhuma falta encontrada com esses filtros.' : 'Nenhuma falta registrada.'}
                    </Td>
                  </tr>
                )}
                {paged.map((s) => (
                  <Tr key={s.id}>
                    <Td>
                      {s.status === 'PENDING' && !s.shortageOrderId && (
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
                    <Td className="max-w-[260px] whitespace-normal">
                      <div className="flex items-center gap-1.5 min-w-0">
                        <span className="font-medium text-gray-900 break-all min-w-0">{s.product}</span>
                        {s.shortageOrderId && (
                          <button
                            title="Ver pedido de falta"
                            onClick={() => navigate(`/shortage-orders/${s.shortageOrderId}`)}
                            className="text-blue-400 hover:text-blue-600 shrink-0 cursor-pointer"
                          >
                            <Package size={11} />
                          </button>
                        )}
                      </div>
                    </Td>
                    <Td><CategoryBadge category={s.category} /></Td>
                    <Td>{s.quantity ?? '—'}</Td>
                    <Td><ShortageStatusBadge status={s.status} /></Td>
                    <Td className="text-gray-500">{formatDate(s.createdAt)}</Td>
                    <Td>
                      <div className="flex items-center gap-1 justify-end whitespace-nowrap">
                        {s.status === 'PENDING' && !s.shortageOrderId && (
                          <>
                            <Button variant="ghost" size="sm" className="text-blue-500 hover:text-blue-700"
                              onClick={() => handleMarkOrdered(s)}>
                              <CheckCircle size={12} /> Pedido
                            </Button>
                            <Button variant="ghost" size="sm" onClick={() => openEdit(s)}>
                              <Pencil size={12} />
                            </Button>
                            <Button variant="ghost" size="sm" className="text-red-400 hover:text-red-600"
                              onClick={() => handleDelete(s)}>
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

            <CardList>
              {paged.length === 0 && (
                <CardEmpty>{hasFilter ? 'Nenhuma falta encontrada com esses filtros.' : 'Nenhuma falta registrada.'}</CardEmpty>
              )}
              {paged.map((s) => {
                const selectable = s.status === 'PENDING' && !s.shortageOrderId
                return (
                  <MobileCard key={s.id}>
                    <div className="flex items-start gap-2">
                      {selectable && (
                        <input
                          type="checkbox"
                          checked={selectedIds.has(s.id)}
                          onChange={() => setSelectedIds((prev) => {
                            const next = new Set(prev)
                            if (next.has(s.id)) next.delete(s.id); else next.add(s.id)
                            return next
                          })}
                          className="accent-gray-700 cursor-pointer mt-1 h-4 w-4 shrink-0"
                        />
                      )}
                      <div className="flex items-center gap-1.5 min-w-0 flex-1">
                        <span className="font-semibold text-gray-900 break-words">{s.product}</span>
                        {s.shortageOrderId && (
                          <button
                            title="Ver pedido de falta"
                            onClick={() => navigate(`/shortage-orders/${s.shortageOrderId}`)}
                            className="text-blue-500 shrink-0 cursor-pointer"
                          >
                            <Package size={13} />
                          </button>
                        )}
                      </div>
                      <ShortageStatusBadge status={s.status} />
                    </div>
                    <div className="flex items-center justify-between gap-3 text-sm">
                      <CategoryBadge category={s.category} />
                      <span className="text-gray-400">
                        Qtd: <span className="text-gray-700">{s.quantity ?? '—'}</span> · {formatDate(s.createdAt)}
                      </span>
                    </div>
                    <CardActions>
                      {selectable && (
                        <>
                          <Button variant="ghost" className="h-11 px-3 text-blue-500" onClick={() => handleMarkOrdered(s)}>
                            <CheckCircle size={15} /> Pedido
                          </Button>
                          <IconAction label="Editar" onClick={() => openEdit(s)}><Pencil size={17} /></IconAction>
                          <IconAction label="Excluir" className="text-red-500" onClick={() => handleDelete(s)}><Trash2 size={17} /></IconAction>
                        </>
                      )}
                      <AuditButton
                        triggerClassName="grid place-items-center h-11 w-11 p-0"
                        iconSize={18}
                        rows={[
                          { label: 'Registrado', value: `${s.createdByName} · ${formatDate(s.createdAt)}` },
                          { label: 'Pedido', value: s.orderedByName ? `${s.orderedByName} · ${formatDate(s.orderedAt)}` : '—' },
                        ]}
                      />
                    </CardActions>
                  </MobileCard>
                )
              })}
            </CardList>

            <Pagination page={page} totalPages={totalPages || 1}
              totalElements={filtered.length} size={PAGE_SIZE} onPageChange={setPage} />
          </>
        )}
      </div>

      <Dialog open={dialogOpen} onOpenChange={(v) => !v && closeDialog()}
        title={editing ? `Editar falta — ${label}` : `Nova falta — ${label}`}>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Item</label>
            <Input value={form.product} onChange={(e) => setForm((p) => ({ ...p, product: e.target.value }))}
              maxLength={150} required autoFocus autoComplete="off" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Categoria</label>
            <Select value={form.category} onChange={(e) => setForm((p) => ({ ...p, category: e.target.value as Category }))}>
              {CATEGORY_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
            </Select>
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Quantidade <span className="text-gray-400">(opcional)</span>
            </label>
            <Input type="number" min={1} max={999} step={1} value={form.quantity}
              onKeyDown={(e) => ['e', 'E', '+', '-', '.', ','].includes(e.key) && e.preventDefault()}
              onChange={(e) => {
                const val = e.target.value.replace(/[^0-9]/g, '')
                if (val && parseInt(val, 10) > 999) return
                setForm((p) => ({ ...p, quantity: val }))
              }}
              placeholder="—" />
          </div>
          {saveMutation.isError && <ErrorMessage error={saveMutation.error} />}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={closeDialog}>Cancelar</Button>
            <Button type="submit" variant="primary" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Salvando...' : 'Salvar'}
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  )
}

const ORDER_STATUS_OPTIONS: { value: 'ALL' | 'PENDING' | 'ORDERED'; label: string }[] = [
  { value: 'ALL', label: 'Todos' },
  { value: 'PENDING', label: 'Pendente' },
  { value: 'ORDERED', label: 'Pedido' },
]

function PedidosPanel({ shortageType, label, createOpen, setCreateOpen }: PanelProps) {
  const [distributorFilter, setDistributorFilter] = useState<Distributor | null>(null)
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'PENDING' | 'ORDERED'>('ALL')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  const withPin = useWithPin()
  const confirm = useConfirm()
  const toast = useToast()
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['shortage-orders', shortageType],
    queryFn: () => listShortageOrders(shortageType, undefined, 0, 500),
  })

  const filtered = useMemo(() => {
    let items = data?.content ?? []
    if (distributorFilter) items = items.filter((o) => o.distributorId === distributorFilter.id)
    if (statusFilter !== 'ALL') items = items.filter((o) => o.status === statusFilter)
    if (dateFrom) { const from = parseLocalDate(dateFrom); items = items.filter((o) => new Date(o.createdAt) >= from) }
    if (dateTo) { const to = parseLocalDate(dateTo); to.setHours(23, 59, 59, 999); items = items.filter((o) => new Date(o.createdAt) <= to) }
    return items
  }, [data, distributorFilter, statusFilter, dateFrom, dateTo])

  const hasFilter = !!distributorFilter || statusFilter !== 'ALL' || !!dateFrom || !!dateTo

  const invalidate = () => qc.invalidateQueries({ queryKey: ['shortage-orders', shortageType] })

  const deleteMutation = useMutation({
    mutationFn: deleteShortageOrder,
    onSuccess: () => {
      invalidate()
      qc.invalidateQueries({ queryKey: ['shortages-all'] })
    },
  })

  const markOrderedMutation = useMutation({
    mutationFn: markShortageOrderAsOrdered,
    onSuccess: () => {
      invalidate()
      qc.invalidateQueries({ queryKey: ['shortages-all'] })
    },
  })

  async function handleDelete(id: string, distributorName: string) {
    if (!await confirm(`Excluir pedido da ${distributorName}? As faltas associadas também serão excluídas.`)) return
    withPin(() => deleteMutation.mutate(id))
  }

  async function handleMarkOrdered(id: string, distributorName: string) {
    if (!await confirm(`Marcar pedido da ${distributorName} como pedido?`)) return
    withPin(() => markOrderedMutation.mutate(id))
  }

  const totalElements = filtered.length
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const orders = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  return (
    <div className="space-y-4">
      <div className="bg-white border border-gray-200 rounded-lg p-4 space-y-3">
        <div>
          <p className="text-xs text-gray-400 mb-1.5">Status</p>
          <div className="flex items-center gap-2 flex-wrap">
            {ORDER_STATUS_OPTIONS.map((s) => (
              <button key={s.value} onClick={() => { setStatusFilter(s.value); setPage(0) }}
                className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors cursor-pointer ${
                  statusFilter === s.value
                    ? 'bg-brand-600 text-white border-brand-600'
                    : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'
                }`}>
                {s.label}
              </button>
            ))}
          </div>
        </div>
        <div className="flex gap-3 flex-wrap pt-1 border-t border-gray-100">
          <div className="w-64">
            <DistributorSearch
              value={distributorFilter}
              onChange={(d) => { setDistributorFilter(d); setPage(0) }}
              allowCreate={false}
            />
          </div>
          <div className="flex items-center gap-2">
            <Input type="date" value={dateFrom} onChange={(e) => { setDateFrom(e.target.value); setPage(0) }} className="w-36" />
            <span className="text-gray-400 text-xs">até</span>
            <Input type="date" value={dateTo} onChange={(e) => { setDateTo(e.target.value); setPage(0) }} className="w-36" />
          </div>
          {hasFilter && (
            <Button variant="ghost" size="sm"
              onClick={() => { setDistributorFilter(null); setStatusFilter('ALL'); setDateFrom(''); setDateTo(''); setPage(0) }}>
              <X size={12} /> Limpar
            </Button>
          )}
        </div>
      </div>

      <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-12"><Spinner /></div>
        ) : (
          <>
            <div className="hidden md:block">
            <Table>
              <TableHead>
                <tr>
                  <Th>Distribuidora</Th>
                  <Th>Status</Th>
                  <Th>Data</Th>
                  <Th />
                </tr>
              </TableHead>
              <TableBody>
                {orders.length === 0 && (
                  <tr>
                    <Td colSpan={4} className="text-center text-gray-400 py-10">
                      {hasFilter ? 'Nenhum pedido encontrado com esses filtros.' : 'Nenhum pedido registrado.'}
                    </Td>
                  </tr>
                )}
                {orders.map((o) => (
                  <Tr
                    key={o.id}
                    className="cursor-pointer"
                    onClick={() => navigate(`/shortage-orders/${o.id}`, { state: { from: `/shortages?tab=${shortageType}&view=PEDIDOS` } })}
                  >
                    <Td>
                      <span className="font-medium text-gray-900">{o.distributorName}</span>
                    </Td>
                    <Td><ShortageOrderStatusBadge status={o.status} /></Td>
                    <Td className="text-gray-500">{formatDate(o.createdAt)}</Td>
                    <Td>
                      <div className="flex items-center gap-1 justify-end whitespace-nowrap" onClick={(e) => e.stopPropagation()}>
                        {o.status === 'PENDING' && (
                          <>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-blue-500 hover:text-blue-700"
                              onClick={() => handleMarkOrdered(o.id, o.distributorName)}
                              disabled={markOrderedMutation.isPending}
                            >
                              <CheckCircle size={12} /> Pedido
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-red-400 hover:text-red-600"
                              onClick={() => handleDelete(o.id, o.distributorName)}
                              disabled={deleteMutation.isPending}
                            >
                              <Trash2 size={12} />
                            </Button>
                          </>
                        )}
                        <AuditButton rows={[
                          { label: 'Criado', value: `${o.createdByName} · ${formatDate(o.createdAt)}` },
                          { label: 'Pedido', value: o.orderedByName ? `${o.orderedByName} · ${formatDate(o.orderedAt)}` : '—' },
                        ]} />
                      </div>
                    </Td>
                  </Tr>
                ))}
              </TableBody>
            </Table>
            </div>

            <CardList>
              {orders.length === 0 && <CardEmpty>{hasFilter ? 'Nenhum pedido encontrado com esses filtros.' : 'Nenhum pedido registrado.'}</CardEmpty>}
              {orders.map((o) => (
                <MobileCard
                  key={o.id}
                  onClick={() => navigate(`/shortage-orders/${o.id}`, { state: { from: `/shortages?tab=${shortageType}&view=PEDIDOS` } })}
                >
                  <div className="flex items-start gap-2">
                    <span className="font-semibold text-gray-900 break-words min-w-0 flex-1">{o.distributorName}</span>
                    <ShortageOrderStatusBadge status={o.status} />
                  </div>
                  <div className="text-sm text-gray-400">{formatDate(o.createdAt)}</div>
                  <CardActions>
                    {o.status === 'PENDING' && (
                      <>
                        <Button
                          variant="ghost"
                          className="h-11 px-3 text-blue-500"
                          onClick={(e) => { e.stopPropagation(); handleMarkOrdered(o.id, o.distributorName) }}
                          disabled={markOrderedMutation.isPending}
                        >
                          <CheckCircle size={15} /> Pedido
                        </Button>
                        <IconAction
                          label="Excluir"
                          className="text-red-500"
                          onClick={() => handleDelete(o.id, o.distributorName)}
                          disabled={deleteMutation.isPending}
                        >
                          <Trash2 size={17} />
                        </IconAction>
                      </>
                    )}
                    <span onClick={(e) => e.stopPropagation()}>
                      <AuditButton
                        triggerClassName="grid place-items-center h-11 w-11 p-0"
                        iconSize={18}
                        rows={[
                          { label: 'Criado', value: `${o.createdByName} · ${formatDate(o.createdAt)}` },
                          { label: 'Pedido', value: o.orderedByName ? `${o.orderedByName} · ${formatDate(o.orderedAt)}` : '—' },
                        ]}
                      />
                    </span>
                  </CardActions>
                </MobileCard>
              ))}
            </CardList>

            <Pagination
              page={page}
              totalPages={totalPages}
              totalElements={totalElements}
              size={PAGE_SIZE}
              onPageChange={setPage}
            />
          </>
        )}
      </div>

      <CreateShortageOrderDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        shortageType={shortageType}
        label={label}
        onSuccess={(id) => {
          toast.success('Pedido criado')
          navigate(`/shortage-orders/${id}`, { state: { from: `/shortages?tab=${shortageType}&view=PEDIDOS` } })
        }}
      />
    </div>
  )
}

export function ShortagesPage() {
  const [params, setParams] = useSearchParams()
  const activeTab = (params.get('tab') as ShortageType) || 'WANIA'
  const activeView = (params.get('view') as InnerTab) || 'FALTAS'
  const [createOpen, setCreateOpen] = useState(false)

  function setActiveTab(tab: ShortageType) {
    setCreateOpen(false)
    setParams({ tab, view: activeView })
  }

  function setActiveView(view: InnerTab) {
    setCreateOpen(false)
    setParams({ tab: activeTab, view })
  }

  const tabs: { type: ShortageType; label: string }[] = [
    { type: 'WANIA', label: 'Wania' },
    { type: 'FRANCISCO', label: 'Francisco' },
  ]

  return (
    <div>
      <PageHeader
        title="Faltas de Estoque"
        description="Registre itens em falta para reposição"
        actions={
          <Button variant="primary" size="md" className="px-4" onClick={() => setCreateOpen(true)}>
            <Plus size={15} /> {activeView === 'FALTAS' ? 'Registrar falta' : 'Novo pedido'}
          </Button>
        }
      />

      <div className="px-4 sm:px-6 pt-4">
        <div className="flex gap-1 border-b border-gray-200">
          {tabs.map((t) => (
            <button
              key={t.type}
              onClick={() => setActiveTab(t.type)}
              className={`px-5 py-2.5 text-sm font-medium border-b-2 transition-colors cursor-pointer -mb-px ${
                activeTab === t.type
                  ? 'border-brand-600 text-brand-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <div className="p-4 sm:p-6">
        {activeTab === 'WANIA'
          ? <ShortageTab key="WANIA" shortageType="WANIA" label="Wania" activeView={activeView} onViewChange={setActiveView} createOpen={createOpen} setCreateOpen={setCreateOpen} />
          : <ShortageTab key="FRANCISCO" shortageType="FRANCISCO" label="Francisco" activeView={activeView} onViewChange={setActiveView} createOpen={createOpen} setCreateOpen={setCreateOpen} />
        }
      </div>
    </div>
  )
}
