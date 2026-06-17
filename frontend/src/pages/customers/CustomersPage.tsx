import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useToast } from '@/context/ToastContext'
import { useNavigate } from 'react-router-dom'
import { Plus, Pencil, Trash2, Search, History, X } from 'lucide-react'
import { listCustomers, createCustomer, updateCustomer, deleteCustomer } from '@/api/customers'
import { listOrders } from '@/api/orders'
import { listCompoundings } from '@/api/compoundings'
import { listPrescriptions } from '@/api/prescriptions'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { CardList, MobileCard, CardField, CardActions, IconAction, CardEmpty } from '@/components/ui/mobile-card'
import { Spinner } from '@/components/ui/spinner'
import { Pagination } from '@/components/shared/Pagination'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { Dialog } from '@/components/ui/dialog'
import { PhoneInput } from '@/components/shared/PhoneInput'
import { OrderStatusBadge, CompoundingStatusBadge, PrescriptionStatusBadge } from '@/components/shared/StatusBadge'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { formatDateShort, formatDate, parseLocalDate } from '@/lib/utils'
import type { Customer } from '@/types'

interface FormState { name: string; phoneNumber: string }
const emptyForm: FormState = { name: '', phoneNumber: '' }
const PAGE_SIZE = 20
const HISTORY_PAGE_SIZE = 10
type HistoryTab = 'orders' | 'compoundings' | 'prescriptions'

export function CustomersPage() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [historyCustomer, setHistoryCustomer] = useState<Customer | null>(null)
  const [historyTab, setHistoryTab] = useState<HistoryTab>('orders')
  const [historyPage, setHistoryPage] = useState(0)
  const [historyDateFrom, setHistoryDateFrom] = useState('')
  const [historyDateTo, setHistoryDateTo] = useState('')
  const [editing, setEditing] = useState<Customer | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const withPin = useWithPin()
  const confirm = useConfirm()
  const toast = useToast()
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['customers-all'],
    queryFn: () => listCustomers(0, 500),
  })

  const { data: ordersData } = useQuery({
    queryKey: ['orders-all'],
    queryFn: () => listOrders(0, 500),
    enabled: !!historyCustomer,
  })

  const { data: compoundingsData } = useQuery({
    queryKey: ['compoundings-all'],
    queryFn: () => listCompoundings(0, 500),
    enabled: !!historyCustomer,
  })

  const { data: prescriptionsData } = useQuery({
    queryKey: ['prescriptions-all'],
    queryFn: () => listPrescriptions(0, 500),
    enabled: !!historyCustomer,
  })

  const customerOrders = useMemo(() => {
    if (!historyCustomer || !ordersData) return []
    return [...ordersData.content]
      .filter((o) => o.customerId === historyCustomer.id)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }, [ordersData, historyCustomer])

  const customerCompoundings = useMemo(() => {
    if (!historyCustomer || !compoundingsData) return []
    return [...compoundingsData.content]
      .filter((c) => c.customerId === historyCustomer.id)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }, [compoundingsData, historyCustomer])

  const customerPrescriptions = useMemo(() => {
    if (!historyCustomer || !prescriptionsData) return []
    return [...prescriptionsData.content]
      .filter((p) => p.customerId === historyCustomer.id)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }, [prescriptionsData, historyCustomer])

  const historyFiltered = useMemo(() => {
    let items = (
      historyTab === 'orders' ? customerOrders
        : historyTab === 'compoundings' ? customerCompoundings
          : customerPrescriptions
    ) as { id: string; createdAt: string; createdByName: string; status: string }[]
    if (historyDateFrom) {
      const from = parseLocalDate(historyDateFrom)
      items = items.filter((i) => new Date(i.createdAt) >= from)
    }
    if (historyDateTo) {
      const to = parseLocalDate(historyDateTo)
      to.setHours(23, 59, 59, 999)
      items = items.filter((i) => new Date(i.createdAt) <= to)
    }
    return items
  }, [historyTab, customerOrders, customerCompoundings, customerPrescriptions, historyDateFrom, historyDateTo])

  const currentHistoryAll = historyTab === 'orders' ? customerOrders
    : historyTab === 'compoundings' ? customerCompoundings
      : customerPrescriptions

  const historyTotalPages = Math.ceil(historyFiltered.length / HISTORY_PAGE_SIZE)
  const pagedHistory = historyFiltered.slice(historyPage * HISTORY_PAGE_SIZE, (historyPage + 1) * HISTORY_PAGE_SIZE)

  const filtered = useMemo(() => {
    const items = data?.content ?? []
    if (!query.trim()) return items
    const q = query.toLowerCase()
    return items.filter((c) => c.name.toLowerCase().includes(q) || (c.phoneNumber ?? '').includes(q))
  }, [data, query])

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paged = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  const invalidate = () => qc.invalidateQueries({ queryKey: ['customers-all'] })

  const saveMutation = useMutation({
    mutationFn: () =>
      editing
        ? updateCustomer(editing.id, { name: form.name, phoneNumber: form.phoneNumber || undefined })
        : createCustomer({ name: form.name, phoneNumber: form.phoneNumber || undefined }),
    onSuccess: () => { toast.success(editing ? 'Alterações salvas' : 'Cliente cadastrado'); closeDialog(); invalidate() },
  })

  const deleteMutation = useMutation({ mutationFn: deleteCustomer, onSuccess: () => { toast.success('Cliente excluído'); invalidate() } })

  function openHistory(c: Customer) {
    setHistoryCustomer(c)
    setHistoryTab('orders')
    setHistoryPage(0)
    setHistoryDateFrom('')
    setHistoryDateTo('')
  }

  function openCreate() { setEditing(null); setForm(emptyForm); saveMutation.reset(); setDialogOpen(true) }
  function openEdit(c: Customer) {
    setEditing(c); setForm({ name: c.name, phoneNumber: c.phoneNumber ?? '' })
    saveMutation.reset(); setDialogOpen(true)
  }
  function closeDialog() { setDialogOpen(false); setEditing(null); setForm(emptyForm) }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    withPin(() => saveMutation.mutate())
  }

  async function handleDelete(c: Customer) {
    if (!await confirm(`Excluir o cliente "${c.name}"?`)) return
    withPin(() => deleteMutation.mutate(c.id))
  }

  return (
    <div>
      <PageHeader
        title="Clientes"
        description="Cadastro de clientes da farmácia"
        actions={
          <Button variant="primary" size="md" className="px-4" onClick={openCreate}>
            <Plus size={15} /> Novo cliente
          </Button>
        }
      />

      <div className="p-4 sm:p-6 space-y-4">
        <div className="bg-white border border-gray-200 rounded-lg p-3">
          <div className="relative max-w-sm">
            <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
            <Input value={query} onChange={(e) => { setQuery(e.target.value); setPage(0) }}
              placeholder="Buscar por nome ou telefone..." className="pl-8" autoComplete="off" />
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
                  <tr><Th>Nome</Th><Th>Telefone</Th><Th>Cadastrado em</Th><Th /></tr>
                </TableHead>
                <TableBody>
                  {paged.length === 0 && (
                    <tr>
                      <Td colSpan={4} className="text-center text-gray-400 py-10">
                        {query ? 'Nenhum cliente encontrado.' : 'Nenhum cliente cadastrado.'}
                      </Td>
                    </tr>
                  )}
                  {paged.map((c) => (
                    <Tr key={c.id}>
                      <Td className="font-medium text-gray-900">{c.name}</Td>
                      <Td className="text-gray-500 font-mono text-xs">{c.phoneNumber ?? '—'}</Td>
                      <Td className="text-gray-500">{formatDateShort(c.createdAt)}</Td>
                      <Td>
                        <div className="flex items-center gap-1">
                          <Button variant="ghost" size="sm" title="Ver encomendas"
                            onClick={() => openHistory(c)}>
                            <History size={13} />
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => openEdit(c)}>
                            <Pencil size={12} />
                          </Button>
                          <Button variant="ghost" size="sm" className="text-red-400 hover:text-red-600"
                            onClick={() => handleDelete(c)}>
                            <Trash2 size={12} />
                          </Button>
                        </div>
                      </Td>
                    </Tr>
                  ))}
                </TableBody>
              </Table>
              </div>

              <CardList>
                {paged.length === 0 && <CardEmpty>{query ? 'Nenhum cliente encontrado.' : 'Nenhum cliente cadastrado.'}</CardEmpty>}
                {paged.map((c) => (
                  <MobileCard key={c.id}>
                    <div className="font-semibold text-gray-900 break-words">{c.name}</div>
                    <CardField label="Telefone"><span className="font-mono text-xs">{c.phoneNumber ?? '—'}</span></CardField>
                    <CardField label="Cadastrado">{formatDateShort(c.createdAt)}</CardField>
                    <CardActions>
                      <IconAction label="Ver encomendas" onClick={() => openHistory(c)}><History size={18} /></IconAction>
                      <IconAction label="Editar" onClick={() => openEdit(c)}><Pencil size={17} /></IconAction>
                      <IconAction label="Excluir" className="text-red-500" onClick={() => handleDelete(c)}><Trash2 size={17} /></IconAction>
                    </CardActions>
                  </MobileCard>
                ))}
              </CardList>

              <Pagination page={page} totalPages={totalPages || 1}
                totalElements={filtered.length} size={PAGE_SIZE} onPageChange={setPage} />
            </>
          )}
        </div>
      </div>

      {/* Dialog: editar/criar cliente */}
      <Dialog open={dialogOpen} onOpenChange={(v) => !v && closeDialog()}
        title={editing ? 'Editar cliente' : 'Novo cliente'}>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Nome</label>
            <Input value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
              maxLength={100} required autoFocus autoComplete="off" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Telefone <span className="text-gray-400">(opcional)</span>
            </label>
            <PhoneInput value={form.phoneNumber} onChange={(v) => setForm((p) => ({ ...p, phoneNumber: v }))} />
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

      {/* Dialog: histórico do cliente */}
      <Dialog
        open={!!historyCustomer}
        onOpenChange={(v) => !v && setHistoryCustomer(null)}
        title={`Histórico — ${historyCustomer?.name ?? ''}`}
        className="max-w-2xl"
      >
        {/* Tabs */}
        <div className="flex gap-1 border-b border-gray-200">
          {([
            { key: 'orders', label: 'Encomendas', count: customerOrders.length },
            { key: 'compoundings', label: 'Manipulações', count: customerCompoundings.length },
            { key: 'prescriptions', label: 'Receitas', count: customerPrescriptions.length },
          ] as { key: HistoryTab; label: string; count: number }[]).map((t) => (
            <button key={t.key}
              onClick={() => { setHistoryTab(t.key); setHistoryPage(0) }}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors cursor-pointer -mb-px ${
                historyTab === t.key
                  ? 'border-brand-600 text-brand-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}>
              {t.label}
              <span className={`ml-1.5 text-xs px-1.5 py-0.5 rounded-full ${
                historyTab === t.key ? 'bg-brand-600 text-white' : 'bg-gray-100 text-gray-500'
              }`}>{t.count}</span>
            </button>
          ))}
        </div>

        {/* Date filters */}
        <div className="flex items-center gap-2 py-3 border-b border-gray-100">
          <span className="text-xs text-gray-500 shrink-0">Período:</span>
          <Input type="date" value={historyDateFrom}
            onChange={(e) => { setHistoryDateFrom(e.target.value); setHistoryPage(0) }}
            className="w-36 text-xs" />
          <span className="text-xs text-gray-400">até</span>
          <Input type="date" value={historyDateTo}
            onChange={(e) => { setHistoryDateTo(e.target.value); setHistoryPage(0) }}
            className="w-36 text-xs" />
          {(historyDateFrom || historyDateTo) && (
            <button onClick={() => { setHistoryDateFrom(''); setHistoryDateTo(''); setHistoryPage(0) }}
              className="text-gray-400 hover:text-gray-600 cursor-pointer">
              <X size={13} />
            </button>
          )}
          {(historyDateFrom || historyDateTo) && (
            <span className="text-xs text-gray-400 ml-auto">
              {historyFiltered.length} de {currentHistoryAll.length} registro(s)
            </span>
          )}
        </div>

        {/* Table */}
        {pagedHistory.length === 0 ? (
          <p className="text-sm text-gray-400 py-6 text-center">
            {(historyTab === 'orders' ? ordersData : historyTab === 'compoundings' ? compoundingsData : prescriptionsData)
              ? (historyDateFrom || historyDateTo)
                ? 'Nenhum registro encontrado neste período.'
                : `Nenhuma ${historyTab === 'orders' ? 'encomenda' : historyTab === 'compoundings' ? 'manipulação' : 'receita'} registrada para este cliente.`
              : 'Carregando...'}
          </p>
        ) : (
          <div className="overflow-x-auto rounded border border-gray-200 mt-3">
            <table className="w-full text-sm min-w-[420px]">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-3 py-2 text-xs font-medium text-gray-500 uppercase tracking-wide">Status</th>
                  {historyTab === 'compoundings'
                    ? <th className="text-left px-3 py-2 text-xs font-medium text-gray-500 uppercase tracking-wide">Farmácia</th>
                    : <th className="text-left px-3 py-2 text-xs font-medium text-gray-500 uppercase tracking-wide">Itens</th>
                  }
                  <th className="text-left px-3 py-2 text-xs font-medium text-gray-500 uppercase tracking-wide">Criado por</th>
                  <th className="text-left px-3 py-2 text-xs font-medium text-gray-500 uppercase tracking-wide">Data</th>
                  <th />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {pagedHistory.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50">
                    <td className="px-3 py-2">
                      {historyTab === 'orders'
                        ? <OrderStatusBadge status={(item as { status: string }).status as Parameters<typeof OrderStatusBadge>[0]['status']} />
                        : historyTab === 'compoundings'
                          ? <CompoundingStatusBadge status={(item as { status: string }).status as Parameters<typeof CompoundingStatusBadge>[0]['status']} />
                          : <PrescriptionStatusBadge status={(item as { status: string }).status as Parameters<typeof PrescriptionStatusBadge>[0]['status']} />
                      }
                    </td>
                    <td className="px-3 py-2 text-gray-600">
                      {historyTab === 'compoundings'
                        ? (item as unknown as { pharmacyName: string }).pharmacyName
                        : `${(item as unknown as { items?: unknown[] }).items?.length ?? 0} item(s)`
                      }
                    </td>
                    <td className="px-3 py-2 text-gray-500">{item.createdByName}</td>
                    <td className="px-3 py-2 text-gray-500">{formatDate(item.createdAt)}</td>
                    <td className="px-3 py-2">
                      <button
                        onClick={() => {
                          setHistoryCustomer(null)
                          navigate(
                            historyTab === 'orders' ? `/orders/${item.id}`
                              : historyTab === 'compoundings' ? `/compoundings/${item.id}`
                                : `/prescriptions/${item.id}`,
                          )
                        }}
                        className="text-xs text-blue-600 hover:underline cursor-pointer"
                      >
                        Ver
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {historyTotalPages > 1 && (
          <div className="flex items-center justify-between mt-3 text-xs text-gray-500">
            <span>
              {historyPage * HISTORY_PAGE_SIZE + 1}–{Math.min((historyPage + 1) * HISTORY_PAGE_SIZE, historyFiltered.length)} de {historyFiltered.length}
            </span>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setHistoryPage(0)}
                disabled={historyPage === 0}
                className="px-2 py-1 border border-gray-300 rounded disabled:opacity-40 hover:bg-gray-50 cursor-pointer disabled:cursor-not-allowed"
              >«</button>
              <button
                onClick={() => setHistoryPage((p) => Math.max(0, p - 1))}
                disabled={historyPage === 0}
                className="px-2 py-1 border border-gray-300 rounded disabled:opacity-40 hover:bg-gray-50 cursor-pointer disabled:cursor-not-allowed"
              >‹</button>
              <span className="px-2">página {historyPage + 1} de {historyTotalPages}</span>
              <button
                onClick={() => setHistoryPage((p) => Math.min(historyTotalPages - 1, p + 1))}
                disabled={historyPage >= historyTotalPages - 1}
                className="px-2 py-1 border border-gray-300 rounded disabled:opacity-40 hover:bg-gray-50 cursor-pointer disabled:cursor-not-allowed"
              >›</button>
              <button
                onClick={() => setHistoryPage(historyTotalPages - 1)}
                disabled={historyPage >= historyTotalPages - 1}
                className="px-2 py-1 border border-gray-300 rounded disabled:opacity-40 hover:bg-gray-50 cursor-pointer disabled:cursor-not-allowed"
              >»</button>
            </div>
          </div>
        )}

        <div className="flex justify-end mt-4">
          <Button variant="secondary" onClick={() => setHistoryCustomer(null)}>Fechar</Button>
        </div>
      </Dialog>
    </div>
  )
}
