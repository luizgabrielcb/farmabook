import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, CheckCircle, Search, X } from 'lucide-react'
import { listShortages, createShortage, updateShortage, deleteShortage, markShortageAsOrdered } from '@/api/shortages'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { Spinner } from '@/components/ui/spinner'
import { Pagination } from '@/components/shared/Pagination'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { Dialog } from '@/components/ui/dialog'
import { ShortageStatusBadge } from '@/components/shared/StatusBadge'
import { CategoryBadge, CATEGORY_OPTIONS } from '@/components/shared/CategoryBadge'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { formatDate, parseLocalDate } from '@/lib/utils'
import type { Shortage, Category, ShortageStatus, ShortageType } from '@/types'

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

interface TabPanelProps {
  shortageType: ShortageType
  label: string
}

function ShortageTab({ shortageType, label }: TabPanelProps) {
  const [statusFilter, setStatusFilter] = useState<ShortageStatus | 'ALL'>('ALL')
  const [categoryFilter, setCategoryFilter] = useState<Category | 'ALL'>('ALL')
  const [query, setQuery] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<Shortage | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
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
        ? updateShortage(editing.id, { ...form, quantity: form.quantity ? Number(form.quantity) : null, shortageType })
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

  const hasFilter = statusFilter !== 'ALL' || categoryFilter !== 'ALL' || query || dateFrom || dateTo

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button variant="primary" size="sm" onClick={openCreate}>
          <Plus size={13} /> Nova falta — {label}
        </Button>
      </div>

      <div className="bg-white border border-gray-200 rounded-lg p-4 space-y-3">
        {/* Linha 1: Status */}
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

        {/* Linha 2: Categoria */}
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

        {/* Linha 3: Busca e datas */}
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
            <Table>
              <TableHead>
                <tr>
                  <Th>Produto</Th><Th>Categoria</Th><Th>Qtd.</Th><Th>Status</Th>
                  <Th>Registrado por</Th><Th>Pedido por</Th><Th>Data</Th><Th />
                </tr>
              </TableHead>
              <TableBody>
                {paged.length === 0 && (
                  <tr>
                    <Td colSpan={8} className="text-center text-gray-400 py-10">
                      {hasFilter ? 'Nenhuma falta encontrada com esses filtros.' : 'Nenhuma falta registrada.'}
                    </Td>
                  </tr>
                )}
                {paged.map((s) => (
                  <Tr key={s.id}>
                    <Td><span className="font-medium text-gray-900 block max-w-[200px] truncate" title={s.product}>{s.product}</span></Td>
                    <Td><CategoryBadge category={s.category} /></Td>
                    <Td>{s.quantity ?? '—'}</Td>
                    <Td><ShortageStatusBadge status={s.status} /></Td>
                    <Td className="text-gray-500">{s.createdByName}</Td>
                    <Td className="text-gray-500">{s.orderedByName ?? '—'}</Td>
                    <Td className="text-gray-500">{formatDate(s.createdAt)}</Td>
                    <Td>
                      {s.status === 'PENDING' && (
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

export function ShortagesPage() {
  const [activeTab, setActiveTab] = useState<ShortageType>('WANIA')

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
          ? <ShortageTab key="WANIA" shortageType="WANIA" label="Wania" />
          : <ShortageTab key="FRANCISCO" shortageType="FRANCISCO" label="Francisco" />
        }
      </div>
    </div>
  )
}
