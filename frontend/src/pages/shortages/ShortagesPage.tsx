import { useState, useMemo } from 'react'
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
import { Spinner } from '@/components/ui/spinner'
import { Pagination } from '@/components/shared/Pagination'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { Dialog } from '@/components/ui/dialog'
import { ShortageStatusBadge, ShortageOrderStatusBadge } from '@/components/shared/StatusBadge'
import { CategoryBadge, CATEGORY_OPTIONS } from '@/components/shared/CategoryBadge'
import { DistributorSearch } from '@/components/shared/DistributorSearch'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
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
}

function ShortageTab({ shortageType, label, activeView, onViewChange }: TabPanelProps) {
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
                  ? 'bg-gray-900 text-white border-gray-900'
                  : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'
              }`}
            >
              {tab === 'FALTAS' ? 'Faltas' : 'Pedidos'}
            </button>
          ))}
        </div>
      </div>

      {activeView === 'FALTAS'
        ? <FaltasPanel shortageType={shortageType} label={label} />
        : <PedidosPanel shortageType={shortageType} label={label} />
      }
    </div>
  )
}

function FaltasPanel({ shortageType, label }: { shortageType: ShortageType; label: string }) {
  const [statusFilter, setStatusFilter] = useState<ShortageStatus | 'ALL'>('ALL')
  const [categoryFilter, setCategoryFilter] = useState<Category | 'ALL'>('ALL')
  const [query, setQuery] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<Shortage | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const withPin = useWithPin()
  const confirm = useConfirm()
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
    onSuccess: () => { closeDialog(); invalidate() },
  })

  const deleteMutation = useMutation({ mutationFn: deleteShortage, onSuccess: invalidate })
  const markMutation = useMutation({ mutationFn: markShortageAsOrdered, onSuccess: invalidate })

  function openCreate() { setEditing(null); setForm(emptyForm); saveMutation.reset(); setDialogOpen(true) }
  function openEdit(s: Shortage) {
    setEditing(s)
    setForm({ product: s.product, category: s.category, quantity: s.quantity != null ? String(s.quantity) : '' })
    saveMutation.reset(); setDialogOpen(true)
  }
  function closeDialog() { setDialogOpen(false); setEditing(null); setForm(emptyForm) }

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
      <div className="flex justify-end">
        <Button variant="primary" size="sm" onClick={openCreate}>
          <Plus size={13} /> Nova falta — {label}
        </Button>
      </div>

      <div className="bg-white border border-gray-200 rounded-lg p-4 space-y-3">
        <div>
          <p className="text-xs text-gray-400 mb-1.5">Status</p>
          <div className="flex items-center gap-2">
            {STATUS_OPTIONS.map((s) => (
              <button key={s.value} onClick={() => { setStatusFilter(s.value); setPage(0) }}
                className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors cursor-pointer ${
                  statusFilter === s.value
                    ? 'bg-gray-800 text-white border-gray-800'
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
              placeholder="Buscar produto..." className="pl-8" autoComplete="off" />
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
              <div className="flex items-center gap-3 px-4 py-2 bg-blue-50 border-b border-blue-100 text-sm">
                <button onClick={() => setSelectedIds(new Set())} className="text-blue-500 hover:text-blue-700 cursor-pointer">
                  <X size={14} />
                </button>
                <span className="text-blue-700 font-medium">{selectedIds.size} selecionado(s)</span>
                <div className="flex items-center gap-2 ml-2">
                  <Button variant="secondary" size="sm" onClick={bulkMarkOrdered}>Marcar como pedido</Button>
                  <Button variant="danger" size="sm" onClick={bulkDelete}>
                    <Trash2 size={12} /> Excluir
                  </Button>
                </div>
              </div>
            )}
            <Table>
              <TableHead>
                <tr>
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
                  <Th>Produto</Th><Th>Categoria</Th><Th>Qtd.</Th><Th>Status</Th>
                  <Th>Registrado por</Th><Th>Pedido por</Th><Th>Data</Th><Th />
                </tr>
              </TableHead>
              <TableBody>
                {paged.length === 0 && (
                  <tr>
                    <Td colSpan={9} className="text-center text-gray-400 py-10">
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
                    <Td>
                      <div className="flex items-center gap-1.5">
                        <span className="font-medium text-gray-900 block max-w-[200px] truncate" title={s.product}>{s.product}</span>
                        {s.shortageOrderId && (
                          <span title="Parte de um pedido" className="text-blue-400">
                            <Package size={11} />
                          </span>
                        )}
                      </div>
                    </Td>
                    <Td><CategoryBadge category={s.category} /></Td>
                    <Td>{s.quantity ?? '—'}</Td>
                    <Td><ShortageStatusBadge status={s.status} /></Td>
                    <Td className="text-gray-500">{s.createdByName}</Td>
                    <Td className="text-gray-500">{s.orderedByName ?? '—'}</Td>
                    <Td className="text-gray-500">{formatDate(s.createdAt)}</Td>
                    <Td>
                      {s.status === 'PENDING' && !s.shortageOrderId && (
                        <div className="flex items-center gap-1">
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
                        </div>
                      )}
                    </Td>
                  </Tr>
                ))}
              </TableBody>
            </Table>
            <Pagination page={page} totalPages={totalPages || 1}
              totalElements={filtered.length} size={PAGE_SIZE} onPageChange={setPage} />
          </>
        )}
      </div>

      <Dialog open={dialogOpen} onOpenChange={(v) => !v && closeDialog()}
        title={editing ? `Editar falta — ${label}` : `Nova falta — ${label}`}>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Produto</label>
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
            <Input type="number" min={1} value={form.quantity}
              onChange={(e) => setForm((p) => ({ ...p, quantity: e.target.value }))}
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

function PedidosPanel({ shortageType, label }: { shortageType: ShortageType; label: string }) {
  const [createOpen, setCreateOpen] = useState(false)
  const [distributorFilter, setDistributorFilter] = useState<Distributor | null>(null)
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  const withPin = useWithPin()
  const confirm = useConfirm()
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['shortage-orders', shortageType, distributorFilter?.id, page],
    queryFn: () =>
      listShortageOrders(shortageType, distributorFilter?.id, page, PAGE_SIZE),
  })

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

  const orders = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const totalElements = data?.totalElements ?? 0

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 w-64">
          <DistributorSearch
            value={distributorFilter}
            onChange={(d) => { setDistributorFilter(d); setPage(0) }}
            allowCreate={false}
          />
        </div>
        <Button variant="primary" size="sm" onClick={() => setCreateOpen(true)}>
          <Plus size={13} /> Novo pedido — {label}
        </Button>
      </div>

      <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-12"><Spinner /></div>
        ) : (
          <>
            <Table>
              <TableHead>
                <tr>
                  <Th>Distribuidora</Th>
                  <Th>Status</Th>
                  <Th>Criado por</Th>
                  <Th>Pedido por</Th>
                  <Th>Data</Th>
                  <Th />
                </tr>
              </TableHead>
              <TableBody>
                {orders.length === 0 && (
                  <tr>
                    <Td colSpan={6} className="text-center text-gray-400 py-10">
                      Nenhum pedido registrado.
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
                    <Td className="text-gray-500">{o.createdByName}</Td>
                    <Td className="text-gray-500">{o.orderedByName ?? '—'}</Td>
                    <Td className="text-gray-500">{formatDate(o.createdAt)}</Td>
                    <Td>
                      {o.status === 'PENDING' && (
                        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
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
                        </div>
                      )}
                    </Td>
                  </Tr>
                ))}
              </TableBody>
            </Table>
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
      />
    </div>
  )
}

export function ShortagesPage() {
  const [params, setParams] = useSearchParams()
  const activeTab = (params.get('tab') as ShortageType) || 'WANIA'
  const activeView = (params.get('view') as InnerTab) || 'FALTAS'

  function setActiveTab(tab: ShortageType) {
    setParams({ tab, view: activeView })
  }

  function setActiveView(view: InnerTab) {
    setParams({ tab: activeTab, view })
  }

  const tabs: { type: ShortageType; label: string }[] = [
    { type: 'WANIA', label: 'Wania' },
    { type: 'FRANCISCO', label: 'Francisco' },
  ]

  return (
    <div>
      <PageHeader title="Faltas de Estoque" description="Registre produtos em falta para reposição" />

      <div className="px-6 pt-4">
        <div className="flex gap-1 border-b border-gray-200">
          {tabs.map((t) => (
            <button
              key={t.type}
              onClick={() => setActiveTab(t.type)}
              className={`px-5 py-2.5 text-sm font-medium border-b-2 transition-colors cursor-pointer -mb-px ${
                activeTab === t.type
                  ? 'border-gray-900 text-gray-900'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <div className="p-6">
        {activeTab === 'WANIA'
          ? <ShortageTab key="WANIA" shortageType="WANIA" label="Wania" activeView={activeView} onViewChange={setActiveView} />
          : <ShortageTab key="FRANCISCO" shortageType="FRANCISCO" label="Francisco" activeView={activeView} onViewChange={setActiveView} />
        }
      </div>
    </div>
  )
}
