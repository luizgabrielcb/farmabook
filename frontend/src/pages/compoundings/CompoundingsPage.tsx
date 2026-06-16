import { useState, useMemo, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Plus, Search, X, Pencil, Trash2 } from 'lucide-react'
import { listCompoundings } from '@/api/compoundings'
import {
  listCompoundingPharmacies,
  createCompoundingPharmacy,
  updateCompoundingPharmacy,
  deleteCompoundingPharmacy,
} from '@/api/compounding-pharmacies'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { Spinner } from '@/components/ui/spinner'
import { Pagination } from '@/components/shared/Pagination'
import { Dialog } from '@/components/ui/dialog'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { CompoundingStatusBadge, PaymentStatusBadge } from '@/components/shared/StatusBadge'
import { CreateCompoundingDialog } from './CreateCompoundingDialog'
import { formatDateShort, parseLocalDate } from '@/lib/utils'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { useToast } from '@/context/ToastContext'
import type { CompoundingStatus, CompoundingPharmacy, PaymentStatus } from '@/types'

const STATUS_OPTIONS: { value: CompoundingStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'Todos' },
  { value: 'PENDING', label: 'Pendente' },
  { value: 'ORDERED', label: 'Pedido' },
  { value: 'RECEIVED', label: 'Recebido' },
  { value: 'DELIVERED', label: 'Entregue' },
]

const PAYMENT_OPTIONS: { value: PaymentStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'Todos' },
  { value: 'TO_PAY', label: 'A Pagar' },
  { value: 'MAKE_NOTE', label: 'Fazer Nota' },
  { value: 'PAID', label: 'Pago' },
  { value: 'NOTED', label: 'Anotado' },
]

const PAGE_SIZE = 20

type Tab = 'compoundings' | 'pharmacies'

export function CompoundingsPage() {
  const [activeTab, setActiveTab] = useState<Tab>('compoundings')
  const [createCompoundingOpen, setCreateCompoundingOpen] = useState(false)
  const [createPharmacyOpen, setCreatePharmacyOpen] = useState(false)

  return (
    <div>
      <PageHeader
        title="Manipulações"
        description="Pedidos de manipulação em farmácias externas"
        actions={
          activeTab === 'compoundings' ? (
            <Button variant="primary" size="md" className="px-4" onClick={() => setCreateCompoundingOpen(true)}>
              <Plus size={15} /> Nova manipulação
            </Button>
          ) : (
            <Button variant="primary" size="md" className="px-4" onClick={() => setCreatePharmacyOpen(true)}>
              <Plus size={15} /> Nova farmácia
            </Button>
          )
        }
      />
      <div className="px-6 pt-4">
        <div className="flex gap-1 border-b border-gray-200">
          {([
            { key: 'compoundings', label: 'Manipulações' },
            { key: 'pharmacies', label: 'Farmácias de Manipulação' },
          ] as { key: Tab; label: string }[]).map((t) => (
            <button key={t.key} onClick={() => setActiveTab(t.key)}
              className={`px-5 py-2.5 text-sm font-medium border-b-2 transition-colors cursor-pointer -mb-px ${
                activeTab === t.key
                  ? 'border-brand-600 text-brand-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}>
              {t.label}
            </button>
          ))}
        </div>
      </div>
      <div className="p-6">
        {activeTab === 'compoundings'
          ? <CompoundingsList createOpen={createCompoundingOpen} setCreateOpen={setCreateCompoundingOpen} />
          : <PharmaciesList createOpen={createPharmacyOpen} setCreateOpen={setCreatePharmacyOpen} />
        }
      </div>
    </div>
  )
}

interface CompoundingsListProps { createOpen: boolean; setCreateOpen: (v: boolean) => void }

function CompoundingsList({ createOpen, setCreateOpen }: CompoundingsListProps) {
  const navigate = useNavigate()
  const toast = useToast()
  const [statusFilter, setStatusFilter] = useState<CompoundingStatus | 'ALL'>('ALL')
  const [paymentFilter, setPaymentFilter] = useState<PaymentStatus | 'ALL'>('ALL')
  const [customerQuery, setCustomerQuery] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['compoundings-all'],
    queryFn: () => listCompoundings(0, 500),
  })

  const filtered = useMemo(() => {
    let items = data?.content ?? []
    if (statusFilter !== 'ALL') items = items.filter((c) => c.status === statusFilter)
    if (paymentFilter !== 'ALL') items = items.filter((c) => c.paymentStatus === paymentFilter)
    if (customerQuery.trim()) items = items.filter((c) => c.customerName.toLowerCase().includes(customerQuery.toLowerCase()))
    if (dateFrom) { const from = parseLocalDate(dateFrom); items = items.filter((c) => new Date(c.createdAt) >= from) }
    if (dateTo) { const to = parseLocalDate(dateTo); to.setHours(23, 59, 59, 999); items = items.filter((c) => new Date(c.createdAt) <= to) }
    return items
  }, [data, statusFilter, paymentFilter, customerQuery, dateFrom, dateTo])

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paged = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)
  const hasFilter = statusFilter !== 'ALL' || paymentFilter !== 'ALL' || customerQuery || dateFrom || dateTo

  return (
    <div className="space-y-4">
      <div className="bg-white border border-gray-200 rounded-lg p-4 space-y-3">
        <div>
          <p className="text-xs text-gray-400 mb-1.5">Status</p>
          <div className="flex items-center gap-2 flex-wrap">
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
          <p className="text-xs text-gray-400 mb-1.5">Pagamento</p>
          <div className="flex items-center gap-2 flex-wrap">
            {PAYMENT_OPTIONS.map((s) => (
              <button key={s.value} onClick={() => { setPaymentFilter(s.value); setPage(0) }}
                className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors cursor-pointer ${
                  paymentFilter === s.value
                    ? 'bg-brand-600 text-white border-brand-600'
                    : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'
                }`}>
                {s.label}
              </button>
            ))}
          </div>
        </div>
        <div className="flex gap-3 flex-wrap pt-1 border-t border-gray-100">
          <div className="relative flex-1 min-w-48">
            <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
            <Input value={customerQuery} onChange={(e) => { setCustomerQuery(e.target.value); setPage(0) }}
              placeholder="Buscar cliente..." className="pl-8" autoComplete="off" />
          </div>
          <div className="flex items-center gap-2">
            <Input type="date" value={dateFrom} onChange={(e) => { setDateFrom(e.target.value); setPage(0) }} className="w-36" />
            <span className="text-gray-400 text-xs">até</span>
            <Input type="date" value={dateTo} onChange={(e) => { setDateTo(e.target.value); setPage(0) }} className="w-36" />
          </div>
          {hasFilter && (
            <Button variant="ghost" size="sm"
              onClick={() => { setStatusFilter('ALL'); setPaymentFilter('ALL'); setCustomerQuery(''); setDateFrom(''); setDateTo(''); setPage(0) }}>
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
                  <Th>Cliente</Th><Th>Farmácia</Th><Th>Qtd.</Th>
                  <Th>Status</Th><Th>Pagamento</Th><Th>Data</Th>
                </tr>
              </TableHead>
              <TableBody>
                {paged.length === 0 && (
                  <tr>
                    <Td colSpan={6} className="text-center text-gray-400 py-10">
                      {hasFilter ? 'Nenhuma manipulação encontrada com esses filtros.' : 'Nenhuma manipulação registrada.'}
                    </Td>
                  </tr>
                )}
                {paged.map((c) => (
                  <Tr key={c.id} onClick={() => navigate(`/compoundings/${c.id}`)}>
                    <Td><span className="font-medium text-gray-900 block max-w-[160px] break-words whitespace-normal" title={c.customerName}>{c.customerName}</span></Td>
                    <Td><span className="text-gray-500 block max-w-[140px] break-words whitespace-normal" title={c.pharmacyName ?? ''}>{c.pharmacyName}</span></Td>
                    <Td className="text-gray-500">{c.quantity}</Td>
                    <Td><CompoundingStatusBadge status={c.status} /></Td>
                    <Td><PaymentStatusBadge status={c.paymentStatus} /></Td>
                    <Td className="text-gray-500">{formatDateShort(c.createdAt)}</Td>
                  </Tr>
                ))}
              </TableBody>
            </Table>
            <Pagination page={page} totalPages={totalPages || 1}
              totalElements={filtered.length} size={PAGE_SIZE} onPageChange={setPage} />
          </>
        )}
      </div>

      <CreateCompoundingDialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSuccess={(id) => { toast.success('Manipulação criada'); setCreateOpen(false); navigate(`/compoundings/${id}`) }}
      />
    </div>
  )
}

interface PharmaciesListProps { createOpen: boolean; setCreateOpen: (v: boolean) => void }

function PharmaciesList({ createOpen, setCreateOpen }: PharmaciesListProps) {
  const [editing, setEditing] = useState<CompoundingPharmacy | null>(null)
  const [form, setForm] = useState({ name: '', city: '' })
  const dialogOpen = createOpen || editing !== null
  const withPin = useWithPin()
  const confirm = useConfirm()
  const toast = useToast()
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['compounding-pharmacies'],
    queryFn: () => listCompoundingPharmacies(),
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['compounding-pharmacies'] })

  const saveMutation = useMutation({
    mutationFn: () =>
      editing
        ? updateCompoundingPharmacy(editing.id, form)
        : createCompoundingPharmacy(form),
    onSuccess: () => { toast.success(editing ? 'Alterações salvas' : 'Farmácia cadastrada'); closeDialog(); invalidate() },
  })

  const deleteMutation = useMutation({ mutationFn: deleteCompoundingPharmacy, onSuccess: () => { toast.success('Farmácia excluída'); invalidate() } })

  useEffect(() => {
    if (createOpen) { setEditing(null); setForm({ name: '', city: '' }); saveMutation.reset() }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [createOpen])

  function openEdit(p: CompoundingPharmacy) {
    setEditing(p); setForm({ name: p.name, city: p.city }); saveMutation.reset()
  }
  function closeDialog() { setCreateOpen(false); setEditing(null); setForm({ name: '', city: '' }) }

  async function handleDelete(p: CompoundingPharmacy) {
    if (!await confirm(`Excluir a farmácia "${p.name}"?`)) return
    withPin(() => deleteMutation.mutate(p.id))
  }

  return (
    <div className="space-y-4">
      <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-12"><Spinner /></div>
        ) : (
          <Table>
            <TableHead>
              <tr><Th>Nome</Th><Th>Cidade</Th><Th>Cadastrada em</Th><Th /></tr>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).length === 0 && (
                <tr><Td colSpan={4} className="text-center text-gray-400 py-10">Nenhuma farmácia cadastrada.</Td></tr>
              )}
              {(data?.content ?? []).map((p) => (
                <Tr key={p.id}>
                  <Td><span className="font-medium text-gray-900 block max-w-[200px] break-words whitespace-normal" title={p.name}>{p.name}</span></Td>
                  <Td><span className="text-gray-500 block max-w-[140px] break-words whitespace-normal" title={p.city}>{p.city}</span></Td>
                  <Td className="text-gray-500">{formatDateShort(p.createdAt)}</Td>
                  <Td>
                    <div className="flex items-center gap-1">
                      <Button variant="ghost" size="sm" onClick={() => openEdit(p)}><Pencil size={12} /></Button>
                      <Button variant="ghost" size="sm" className="text-red-400 hover:text-red-600"
                        onClick={() => handleDelete(p)}><Trash2 size={12} /></Button>
                    </div>
                  </Td>
                </Tr>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      <Dialog open={dialogOpen} onOpenChange={(v) => !v && closeDialog()}
        title={editing ? 'Editar farmácia' : 'Nova farmácia de manipulação'}>
        <form onSubmit={(e) => { e.preventDefault(); withPin(() => saveMutation.mutate()) }} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Nome</label>
            <Input value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
              maxLength={150} required autoFocus autoComplete="off" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Cidade</label>
            <Input value={form.city} onChange={(e) => setForm((p) => ({ ...p, city: e.target.value }))}
              maxLength={100} required autoComplete="off" />
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
