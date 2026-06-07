import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Plus, Pencil, Trash2, Search, History } from 'lucide-react'
import { listCustomers, createCustomer, updateCustomer, deleteCustomer } from '@/api/customers'
import { listOrders } from '@/api/orders'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { Spinner } from '@/components/ui/spinner'
import { Pagination } from '@/components/shared/Pagination'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { Dialog } from '@/components/ui/dialog'
import { PhoneInput } from '@/components/shared/PhoneInput'
import { OrderStatusBadge } from '@/components/shared/StatusBadge'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { formatDateShort, formatDate } from '@/lib/utils'
import type { Customer } from '@/types'

interface FormState { name: string; phoneNumber: string }
const emptyForm: FormState = { name: '', phoneNumber: '' }
const PAGE_SIZE = 20

export function CustomersPage() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [historyCustomer, setHistoryCustomer] = useState<Customer | null>(null)
  const [historyPage, setHistoryPage] = useState(0)
  const HISTORY_PAGE_SIZE = 10
  const [editing, setEditing] = useState<Customer | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const withPin = useWithPin()
  const confirm = useConfirm()
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

  const customerOrders = useMemo(() => {
    if (!historyCustomer || !ordersData) return []
    return [...ordersData.content]
      .filter((o) => o.customerId === historyCustomer.id)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }, [ordersData, historyCustomer])

  const historyTotalPages = Math.ceil(customerOrders.length / HISTORY_PAGE_SIZE)
  const pagedHistory = customerOrders.slice(historyPage * HISTORY_PAGE_SIZE, (historyPage + 1) * HISTORY_PAGE_SIZE)

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
    onSuccess: () => { closeDialog(); invalidate() },
  })

  const deleteMutation = useMutation({ mutationFn: deleteCustomer, onSuccess: invalidate })

  function openHistory(c: Customer) { setHistoryCustomer(c); setHistoryPage(0) }

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
          <Button variant="primary" size="sm" onClick={openCreate}>
            <Plus size={13} /> Novo cliente
          </Button>
        }
      />

      <div className="p-6 space-y-4">
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
                      <Td className="text-gray-500">{c.phoneNumber ?? '—'}</Td>
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

      {/* Dialog: histórico de encomendas */}
      <Dialog
        open={!!historyCustomer}
        onOpenChange={(v) => !v && setHistoryCustomer(null)}
        title={`Encomendas — ${historyCustomer?.name ?? ''}`}
        className="max-w-2xl"
      >
        {customerOrders.length === 0 ? (
          <p className="text-sm text-gray-400 py-4 text-center">
            {ordersData ? 'Nenhuma encomenda registrada para este cliente.' : 'Carregando...'}
          </p>
        ) : (
          <>
            <div className="overflow-hidden rounded border border-gray-200">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="text-left px-3 py-2 text-xs font-medium text-gray-500 uppercase tracking-wide">Status</th>
                    <th className="text-left px-3 py-2 text-xs font-medium text-gray-500 uppercase tracking-wide">Itens</th>
                    <th className="text-left px-3 py-2 text-xs font-medium text-gray-500 uppercase tracking-wide">Criado por</th>
                    <th className="text-left px-3 py-2 text-xs font-medium text-gray-500 uppercase tracking-wide">Data</th>
                    <th />
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {pagedHistory.map((o) => (
                    <tr key={o.id} className="hover:bg-gray-50">
                      <td className="px-3 py-2"><OrderStatusBadge status={o.status} /></td>
                      <td className="px-3 py-2 text-gray-600">{o.items?.length ?? 0} item(s)</td>
                      <td className="px-3 py-2 text-gray-500">{o.createdByName}</td>
                      <td className="px-3 py-2 text-gray-500">{formatDate(o.createdAt)}</td>
                      <td className="px-3 py-2">
                        <button
                          onClick={() => { setHistoryCustomer(null); navigate(`/orders/${o.id}`) }}
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
            {historyTotalPages > 1 && (
              <div className="flex items-center justify-between mt-3 text-xs text-gray-500">
                <span>{customerOrders.length} encomenda(s)</span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setHistoryPage((p) => Math.max(0, p - 1))}
                    disabled={historyPage === 0}
                    className="px-2 py-1 border border-gray-300 rounded disabled:opacity-40 hover:bg-gray-50 cursor-pointer disabled:cursor-not-allowed"
                  >‹</button>
                  <span>{historyPage + 1} / {historyTotalPages}</span>
                  <button
                    onClick={() => setHistoryPage((p) => Math.min(historyTotalPages - 1, p + 1))}
                    disabled={historyPage >= historyTotalPages - 1}
                    className="px-2 py-1 border border-gray-300 rounded disabled:opacity-40 hover:bg-gray-50 cursor-pointer disabled:cursor-not-allowed"
                  >›</button>
                </div>
              </div>
            )}
          </>
        )}
        <div className="flex justify-end mt-4">
          <Button variant="secondary" onClick={() => setHistoryCustomer(null)}>Fechar</Button>
        </div>
      </Dialog>
    </div>
  )
}
