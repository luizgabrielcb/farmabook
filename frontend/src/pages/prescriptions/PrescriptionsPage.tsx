import { useState, useMemo, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Search, X, Plus, Pencil, Trash2, CheckCircle } from 'lucide-react'
import { useToast } from '@/context/ToastContext'
import {
  listPrescriptions,
  createPrescription,
  deletePrescription,
  markAllAsReceived,
  updatePrescriptionItem,
} from '@/api/prescriptions'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { CardList, MobileCard, CardField, CardActions, IconAction, CardEmpty } from '@/components/ui/mobile-card'
import { Spinner } from '@/components/ui/spinner'
import { Pagination } from '@/components/shared/Pagination'
import { PrescriptionStatusBadge } from '@/components/shared/StatusBadge'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { Dialog } from '@/components/ui/dialog'
import { AuditButton } from '@/components/shared/AuditButton'
import { CreatePrescriptionDialog } from './CreatePrescriptionDialog'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { formatDate, formatDateShort, parseLocalDate } from '@/lib/utils'
import type { Prescription, PrescriptionStatus } from '@/types'

const STATUS_OPTIONS: { value: PrescriptionStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'Todos' },
  { value: 'PENDING', label: 'Pendente' },
  { value: 'FINISHED', label: 'Finalizado' },
]

type SectionTab = 'customers' | 'stock'
const PAGE_SIZE = 20

function formatExpiry(raw: string): string {
  const digits = raw.replace(/\D/g, '').slice(0, 6)
  if (digits.length <= 2) return digits
  return digits.slice(0, 2) + '/' + digits.slice(2)
}

export function PrescriptionsPage() {
  const [section, setSection] = useState<SectionTab>('customers')
  const [createOpen, setCreateOpen] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['prescriptions-all'],
    queryFn: () => listPrescriptions(0, 500),
  })

  const customerItems = useMemo(() => (data?.content ?? []).filter((p) => p.customerId != null), [data])
  const stockItems = useMemo(() => (data?.content ?? []).filter((p) => p.customerId == null), [data])

  return (
    <div>
      <PageHeader
        title="Receitas"
        description="Controle de pendências de receitas"
        actions={
          <Button variant="primary" size="md" className="px-4" onClick={() => setCreateOpen(true)}>
            <Plus size={15} /> Nova pendência
          </Button>
        }
      />

      <div className="px-4 sm:px-6 pt-4">
        <div className="flex gap-1 border-b border-gray-200">
          {([
            { value: 'customers', label: 'Clientes' },
            { value: 'stock', label: 'Controle de estoque' },
          ] as { value: SectionTab; label: string }[]).map((t) => (
            <button
              key={t.value}
              onClick={() => setSection(t.value)}
              className={`px-5 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors cursor-pointer ${
                section === t.value
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
        {section === 'customers'
          ? <CustomersPanel items={customerItems} isLoading={isLoading} createOpen={createOpen} setCreateOpen={setCreateOpen} />
          : <StockPanel items={stockItems} isLoading={isLoading} createOpen={createOpen} setCreateOpen={setCreateOpen} />
        }
      </div>
    </div>
  )
}

interface PanelProps { items: Prescription[]; isLoading: boolean; createOpen: boolean; setCreateOpen: (v: boolean) => void }

function CustomersPanel({ items, isLoading, createOpen, setCreateOpen }: PanelProps) {
  const navigate = useNavigate()
  const toast = useToast()
  const [statusFilter, setStatusFilter] = useState<PrescriptionStatus | 'ALL'>('ALL')
  const [customerQuery, setCustomerQuery] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)

  const filtered = useMemo(() => {
    let result = items
    if (statusFilter !== 'ALL') result = result.filter((p) => p.status === statusFilter)
    if (customerQuery.trim())
      result = result.filter((p) =>
        (p.customerName ?? '').toLowerCase().includes(customerQuery.toLowerCase()),
      )
    if (dateFrom) {
      const from = parseLocalDate(dateFrom)
      result = result.filter((p) => new Date(p.createdAt) >= from)
    }
    if (dateTo) {
      const to = parseLocalDate(dateTo)
      to.setHours(23, 59, 59, 999)
      result = result.filter((p) => new Date(p.createdAt) <= to)
    }
    return result
  }, [items, statusFilter, customerQuery, dateFrom, dateTo])

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paged = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)
  const hasFilter = statusFilter !== 'ALL' || customerQuery || dateFrom || dateTo

  function handleCreated(id: string) {
    toast.success('Pendência criada')
    setCreateOpen(false)
    navigate(`/prescriptions/${id}`)
  }

  return (
    <div className="space-y-4">
      <div className="bg-white border border-gray-200 rounded-lg p-4 space-y-3">
        <div className="flex items-center gap-2 flex-wrap">
          {STATUS_OPTIONS.map((s) => (
            <button
              key={s.value}
              onClick={() => { setStatusFilter(s.value); setPage(0) }}
              className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors cursor-pointer ${
                statusFilter === s.value
                  ? 'bg-brand-600 text-white border-brand-600'
                  : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'
              }`}
            >
              {s.label}
            </button>
          ))}
        </div>
        <div className="flex gap-3 flex-wrap">
          <div className="relative flex-1 min-w-48">
            <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
            <Input
              value={customerQuery}
              onChange={(e) => { setCustomerQuery(e.target.value); setPage(0) }}
              placeholder="Buscar cliente..."
              className="pl-8"
              autoComplete="off"
            />
          </div>
          <div className="flex items-center gap-2">
            <Input type="date" value={dateFrom} onChange={(e) => { setDateFrom(e.target.value); setPage(0) }} className="w-36" />
            <span className="text-gray-400 text-xs">até</span>
            <Input type="date" value={dateTo} onChange={(e) => { setDateTo(e.target.value); setPage(0) }} className="w-36" />
          </div>
          {hasFilter && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => { setStatusFilter('ALL'); setCustomerQuery(''); setDateFrom(''); setDateTo(''); setPage(0) }}
            >
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
                  <Th>Cliente</Th>
                  <Th>Status</Th>
                  <Th>Medicamentos</Th>
                  <Th>Criado por</Th>
                  <Th>Data</Th>
                </tr>
              </TableHead>
              <TableBody>
                {paged.length === 0 && (
                  <tr>
                    <Td colSpan={5} className="text-center text-gray-400 py-10">
                      {hasFilter
                        ? 'Nenhuma pendência encontrada com esses filtros.'
                        : 'Nenhuma pendência de receita registrada.'}
                    </Td>
                  </tr>
                )}
                {paged.map((p) => (
                  <Tr
                    key={p.id}
                    className="cursor-pointer hover:bg-gray-50"
                    onClick={() => navigate(`/prescriptions/${p.id}`)}
                  >
                    <Td>
                      <span
                        className="font-medium text-gray-900 block max-w-[180px] break-words whitespace-normal"
                        title={p.customerName ?? undefined}
                      >
                        {p.customerName}
                      </span>
                    </Td>
                    <Td><PrescriptionStatusBadge status={p.status} /></Td>
                    <Td className="text-gray-500">{p.items?.length ?? 0} medicamento(s)</Td>
                    <Td className="text-gray-500">{p.createdByName}</Td>
                    <Td className="text-gray-500">{formatDateShort(p.createdAt)}</Td>
                  </Tr>
                ))}
              </TableBody>
            </Table>
            </div>

            <CardList>
              {paged.length === 0 && (
                <CardEmpty>{hasFilter ? 'Nenhuma pendência encontrada com esses filtros.' : 'Nenhuma pendência de receita registrada.'}</CardEmpty>
              )}
              {paged.map((p) => (
                <MobileCard key={p.id} onClick={() => navigate(`/prescriptions/${p.id}`)}>
                  <div className="flex items-start gap-2">
                    <span className="font-semibold text-gray-900 break-words min-w-0 flex-1" title={p.customerName ?? undefined}>{p.customerName}</span>
                    <PrescriptionStatusBadge status={p.status} />
                  </div>
                  <CardField label="Medicamentos">{p.items?.length ?? 0} medicamento(s)</CardField>
                  <CardField label="Criado">{p.createdByName} · {formatDateShort(p.createdAt)}</CardField>
                </MobileCard>
              ))}
            </CardList>

            <Pagination page={page} totalPages={totalPages || 1} totalElements={filtered.length} size={PAGE_SIZE} onPageChange={setPage} />
          </>
        )}
      </div>

      <CreatePrescriptionDialog open={createOpen} onClose={() => setCreateOpen(false)} onSuccess={handleCreated} />
    </div>
  )
}

interface StockFormState { product: string; quantity: string; batch: string; expiry: string }
const emptyStockForm: StockFormState = { product: '', quantity: '', batch: '', expiry: '' }

function StockPanel({ items, isLoading, createOpen, setCreateOpen }: PanelProps) {
  const toast = useToast()
  const confirm = useConfirm()
  const withPin = useWithPin()
  const qc = useQueryClient()
  const [statusFilter, setStatusFilter] = useState<PrescriptionStatus | 'ALL'>('ALL')
  const [query, setQuery] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Prescription | null>(null)
  const [form, setForm] = useState<StockFormState>(emptyStockForm)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const dialogOpen = createOpen || editing !== null

  const filtered = useMemo(() => {
    let result = items
    if (statusFilter !== 'ALL') result = result.filter((p) => p.status === statusFilter)
    if (query.trim())
      result = result.filter((p) => (p.items[0]?.product ?? '').toLowerCase().includes(query.toLowerCase()))
    if (dateFrom) {
      const from = parseLocalDate(dateFrom)
      result = result.filter((p) => new Date(p.createdAt) >= from)
    }
    if (dateTo) {
      const to = parseLocalDate(dateTo)
      to.setHours(23, 59, 59, 999)
      result = result.filter((p) => new Date(p.createdAt) <= to)
    }
    return result
  }, [items, statusFilter, query, dateFrom, dateTo])

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paged = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)
  const hasFilter = statusFilter !== 'ALL' || query || dateFrom || dateTo

  const invalidate = () => qc.invalidateQueries({ queryKey: ['prescriptions-all'] })

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = { product: form.product, quantity: Number(form.quantity) || 1, batch: form.batch, expiry: form.expiry }
      if (editing) await updatePrescriptionItem(editing.id, editing.items[0].id, body)
      else await createPrescription({ items: [body] })
    },
    onSuccess: () => { toast.success(editing ? 'Alterações salvas' : 'Pendência registrada'); closeDialog(); invalidate() },
  })

  const deleteMutation = useMutation({ mutationFn: deletePrescription, onSuccess: () => { toast.success('Pendência excluída'); invalidate() } })
  const markReceivedMutation = useMutation({ mutationFn: markAllAsReceived, onSuccess: () => { toast.success('Marcado como finalizado'); invalidate() } })

  useEffect(() => {
    if (createOpen) { setEditing(null); setForm(emptyStockForm); saveMutation.reset() }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [createOpen])

  function openEdit(p: Prescription) {
    const item = p.items[0]
    setEditing(p)
    setForm({ product: item.product, quantity: String(item.quantity), batch: item.batch, expiry: item.expiry })
    saveMutation.reset()
  }
  function closeDialog() { setCreateOpen(false); setEditing(null); setForm(emptyStockForm) }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    withPin(() => saveMutation.mutate())
  }

  async function handleMarkReceived(p: Prescription) {
    if (!await confirm(`Marcar "${p.items[0]?.product}" como finalizado?`)) return
    withPin(() => markReceivedMutation.mutate(p.id))
  }

  async function handleDelete(p: Prescription) {
    if (!await confirm(`Excluir a pendência de "${p.items[0]?.product}"?`)) return
    withPin(() => deleteMutation.mutate(p.id))
  }

  const selectablePending = paged.filter((p) => p.status === 'PENDING')
  const allSelected = selectablePending.length > 0 && selectablePending.every((p) => selectedIds.has(p.id))
  const someSelected = selectedIds.size > 0

  function toggleId(id: string) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id); else next.add(id)
      return next
    })
  }

  async function bulkFinalize() {
    const targets = paged.filter((p) => selectedIds.has(p.id) && p.status === 'PENDING')
    if (!targets.length) return
    if (!await confirm(`Marcar ${targets.length} pendência(s) como finalizada(s)?`)) return
    withPin(async () => {
      for (const p of targets) await markAllAsReceived(p.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  async function bulkDelete() {
    const targets = paged.filter((p) => selectedIds.has(p.id) && p.status === 'PENDING')
    if (!targets.length) return
    if (!await confirm(`Excluir ${targets.length} pendência(s)?`)) return
    withPin(async () => {
      for (const p of targets) await deletePrescription(p.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  return (
    <div className="space-y-4">
      <div className="bg-white border border-gray-200 rounded-lg p-4 space-y-3">
        <div className="flex items-center gap-2 flex-wrap">
          {STATUS_OPTIONS.map((s) => (
            <button
              key={s.value}
              onClick={() => { setStatusFilter(s.value); setPage(0) }}
              className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors cursor-pointer ${
                statusFilter === s.value
                  ? 'bg-brand-600 text-white border-brand-600'
                  : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'
              }`}
            >
              {s.label}
            </button>
          ))}
        </div>
        <div className="flex gap-3 flex-wrap">
          <div className="relative flex-1 min-w-48">
            <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
            <Input
              value={query}
              onChange={(e) => { setQuery(e.target.value); setPage(0) }}
              placeholder="Buscar medicamento..."
              className="pl-8"
              autoComplete="off"
            />
          </div>
          <div className="flex items-center gap-2">
            <Input type="date" value={dateFrom} onChange={(e) => { setDateFrom(e.target.value); setPage(0) }} className="w-36" />
            <span className="text-gray-400 text-xs">até</span>
            <Input type="date" value={dateTo} onChange={(e) => { setDateTo(e.target.value); setPage(0) }} className="w-36" />
          </div>
          {hasFilter && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => { setStatusFilter('ALL'); setQuery(''); setDateFrom(''); setDateTo(''); setPage(0) }}
            >
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
                  <Button variant="secondary" size="sm" onClick={bulkFinalize}>Finalizar</Button>
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
                          else setSelectedIds(new Set(selectablePending.map((p) => p.id)))
                        }}
                        className="accent-gray-700 cursor-pointer"
                      />
                    )}
                  </Th>
                  <Th>Medicamento</Th>
                  <Th>Lote</Th>
                  <Th>Validade</Th>
                  <Th>Status</Th>
                  <Th>Criado por</Th>
                  <Th>Data</Th>
                  <Th />
                </tr>
              </TableHead>
              <TableBody>
                {paged.length === 0 && (
                  <tr>
                    <Td colSpan={8} className="text-center text-gray-400 py-10">
                      {hasFilter
                        ? 'Nenhuma pendência encontrada com esses filtros.'
                        : 'Nenhuma pendência de estoque registrada.'}
                    </Td>
                  </tr>
                )}
                {paged.map((p) => {
                  const item = p.items[0]
                  return (
                    <Tr key={p.id}>
                      <Td>
                        {p.status === 'PENDING' && (
                          <input
                            type="checkbox"
                            checked={selectedIds.has(p.id)}
                            onChange={() => toggleId(p.id)}
                            className="accent-gray-700 cursor-pointer"
                          />
                        )}
                      </Td>
                      <Td>
                        <span className="font-medium text-gray-900 block max-w-[200px] break-words whitespace-normal" title={item?.product}>
                          {item?.product}
                        </span>
                      </Td>
                      <Td className="text-gray-500">{item?.batch}</Td>
                      <Td className="text-gray-500">{item?.expiry}</Td>
                      <Td><PrescriptionStatusBadge status={p.status} /></Td>
                      <Td className="text-gray-500">{p.createdByName}</Td>
                      <Td className="text-gray-500">{formatDateShort(p.createdAt)}</Td>
                      <Td>
                        <div className="flex items-center gap-1 justify-end whitespace-nowrap">
                          {p.status === 'PENDING' && (
                            <>
                              <Button variant="ghost" size="sm" className="text-blue-500 hover:text-blue-700" onClick={() => handleMarkReceived(p)}>
                                <CheckCircle size={12} /> Finalizado
                              </Button>
                              <Button variant="ghost" size="sm" onClick={() => openEdit(p)}>
                                <Pencil size={12} />
                              </Button>
                              <Button variant="ghost" size="sm" className="text-red-400 hover:text-red-600" onClick={() => handleDelete(p)}>
                                <Trash2 size={12} />
                              </Button>
                            </>
                          )}
                          <AuditButton rows={[
                            { label: 'Registrado', value: `${p.createdByName} · ${formatDate(p.createdAt)}` },
                            { label: 'Recebido', value: item?.receivedByName ? `${item.receivedByName} · ${formatDate(item.receivedAt)}` : '—' },
                          ]} />
                        </div>
                      </Td>
                    </Tr>
                  )
                })}
              </TableBody>
            </Table>
            </div>

            <CardList>
              {paged.length === 0 && (
                <CardEmpty>{hasFilter ? 'Nenhuma pendência encontrada com esses filtros.' : 'Nenhuma pendência de estoque registrada.'}</CardEmpty>
              )}
              {paged.map((p) => {
                const item = p.items[0]
                return (
                  <MobileCard key={p.id}>
                    <div className="flex items-start gap-2">
                      {p.status === 'PENDING' && (
                        <input
                          type="checkbox"
                          checked={selectedIds.has(p.id)}
                          onChange={() => toggleId(p.id)}
                          className="accent-gray-700 cursor-pointer mt-1 h-4 w-4 shrink-0"
                        />
                      )}
                      <span className="font-semibold text-gray-900 break-words min-w-0 flex-1" title={item?.product}>{item?.product}</span>
                      <PrescriptionStatusBadge status={p.status} />
                    </div>
                    <CardField label="Lote">{item?.batch}</CardField>
                    <CardField label="Validade">{item?.expiry}</CardField>
                    <CardField label="Criado">{p.createdByName} · {formatDateShort(p.createdAt)}</CardField>
                    <CardActions>
                      {p.status === 'PENDING' && (
                        <>
                          <Button variant="ghost" className="h-11 px-3 text-blue-500" onClick={() => handleMarkReceived(p)}>
                            <CheckCircle size={15} /> Finalizado
                          </Button>
                          <IconAction label="Editar" onClick={() => openEdit(p)}><Pencil size={17} /></IconAction>
                          <IconAction label="Excluir" className="text-red-500" onClick={() => handleDelete(p)}><Trash2 size={17} /></IconAction>
                        </>
                      )}
                      <AuditButton
                        triggerClassName="grid place-items-center h-11 w-11 p-0"
                        iconSize={18}
                        rows={[
                          { label: 'Registrado', value: `${p.createdByName} · ${formatDate(p.createdAt)}` },
                          { label: 'Recebido', value: item?.receivedByName ? `${item.receivedByName} · ${formatDate(item.receivedAt)}` : '—' },
                        ]}
                      />
                    </CardActions>
                  </MobileCard>
                )
              })}
            </CardList>

            <Pagination page={page} totalPages={totalPages || 1} totalElements={filtered.length} size={PAGE_SIZE} onPageChange={setPage} />
          </>
        )}
      </div>

      <Dialog open={dialogOpen} onOpenChange={(v) => !v && closeDialog()} title={editing ? 'Editar pendência' : 'Nova pendência de estoque'}>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Medicamento</label>
            <Input value={form.product} onChange={(e) => setForm((p) => ({ ...p, product: e.target.value }))}
              maxLength={150} required autoFocus autoComplete="off" />
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="text-xs font-medium text-gray-700 block mb-1">Qtd.</label>
              <Input type="number" min={1} max={999} step={1} value={form.quantity}
                onKeyDown={(e) => ['e', 'E', '+', '-', '.', ','].includes(e.key) && e.preventDefault()}
                onChange={(e) => {
                  const val = e.target.value.replace(/[^0-9]/g, '')
                  if (val && parseInt(val, 10) > 999) return
                  setForm((p) => ({ ...p, quantity: val }))
                }}
                required />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-700 block mb-1">Lote</label>
              <Input value={form.batch} onChange={(e) => setForm((p) => ({ ...p, batch: e.target.value }))}
                required autoComplete="off" maxLength={50} />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-700 block mb-1">Validade</label>
              <Input value={form.expiry} onChange={(e) => setForm((p) => ({ ...p, expiry: formatExpiry(e.target.value) }))}
                placeholder="MM/yyyy" maxLength={7} required
                pattern="^(0[1-9]|1[0-2])/\d{4}$" title="Formato: MM/yyyy (ex: 03/2026)" />
            </div>
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
