import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getPrescription,
  deletePrescription,
  updatePrescription,
  markAllAsReceived,
  markItemAsReceived,
  addPrescriptionItem,
  updatePrescriptionItem,
  deletePrescriptionItem,
} from '@/api/prescriptions'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { PrescriptionStatusBadge, PrescriptionItemStatusBadge } from '@/components/shared/StatusBadge'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { CustomerSearch } from '@/components/shared/CustomerSearch'
import { PhoneInput } from '@/components/shared/PhoneInput'
import { createCustomer } from '@/api/customers'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { formatDate } from '@/lib/utils'
import { ArrowLeft, Plus, Trash2, Pencil, Settings2, X } from 'lucide-react'
import type { Customer, PrescriptionItem } from '@/types'

interface ItemForm { product: string; quantity: string; batch: string; expiry: string }
const emptyItemForm: ItemForm = { product: '', quantity: '', batch: '', expiry: '' }

function formatExpiry(raw: string): string {
  const digits = raw.replace(/\D/g, '').slice(0, 6)
  if (digits.length <= 2) return digits
  return digits.slice(0, 2) + '/' + digits.slice(2)
}

export function PrescriptionDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const withPin = useWithPin()
  const confirm = useConfirm()

  const [itemDialogOpen, setItemDialogOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<PrescriptionItem | null>(null)
  const [itemForm, setItemForm] = useState<ItemForm>(emptyItemForm)
  const [editOpen, setEditOpen] = useState(false)
  const [editCustomer, setEditCustomer] = useState<Customer | null>(null)
  const [editObservations, setEditObservations] = useState('')
  const [quickAddOpen, setQuickAddOpen] = useState(false)
  const [newCustomerName, setNewCustomerName] = useState('')
  const [newCustomerPhone, setNewCustomerPhone] = useState('')
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())

  const { data: prescription, isLoading, error } = useQuery({
    queryKey: ['prescription', id],
    queryFn: () => getPrescription(id!),
    enabled: !!id,
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['prescription', id] })

  const deleteMutation = useMutation({
    mutationFn: () => deletePrescription(id!),
    onSuccess: () => navigate('/prescriptions'),
  })

  const updateMutation = useMutation({
    mutationFn: () =>
      updatePrescription(id!, {
        customerId: editCustomer!.id,
        observations: editObservations.trim() || null,
      }),
    onSuccess: () => { setEditOpen(false); invalidate() },
  })

  const markAllMutation = useMutation({
    mutationFn: () => markAllAsReceived(id!),
    onSuccess: invalidate,
  })

  const saveItemMutation = useMutation({
    mutationFn: () => {
      const body = {
        product: itemForm.product,
        quantity: Number(itemForm.quantity),
        batch: itemForm.batch,
        expiry: itemForm.expiry,
      }
      return editingItem
        ? updatePrescriptionItem(id!, editingItem.id, body)
        : addPrescriptionItem(id!, body)
    },
    onSuccess: () => { closeItemDialog(); invalidate() },
  })

  const deleteItemMutation = useMutation({
    mutationFn: (itemId: string) => deletePrescriptionItem(id!, itemId),
    onSuccess: invalidate,
  })

  const quickAddMutation = useMutation({
    mutationFn: () => createCustomer({ name: newCustomerName, phoneNumber: newCustomerPhone || undefined }),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ['customers-all'] })
      setEditCustomer(created)
      setQuickAddOpen(false)
      setNewCustomerName('')
      setNewCustomerPhone('')
    },
  })

  function openEdit() {
    setEditCustomer(prescription ? { id: prescription.customerId, name: prescription.customerName, phoneNumber: null, createdAt: '', updatedAt: '' } : null)
    setEditObservations(prescription?.observations ?? '')
    updateMutation.reset()
    setEditOpen(true)
  }

  function openAddItem() {
    setEditingItem(null)
    setItemForm(emptyItemForm)
    saveItemMutation.reset()
    setItemDialogOpen(true)
  }

  function openEditItem(item: PrescriptionItem) {
    setEditingItem(item)
    setItemForm({ product: item.product, quantity: String(item.quantity), batch: item.batch, expiry: item.expiry })
    saveItemMutation.reset()
    setItemDialogOpen(true)
  }

  function closeItemDialog() {
    setItemDialogOpen(false)
    setEditingItem(null)
    setItemForm(emptyItemForm)
  }

  function toggleSelectItem(itemId: string) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      next.has(itemId) ? next.delete(itemId) : next.add(itemId)
      return next
    })
  }

  function toggleSelectAll() {
    const selectable = (prescription?.items ?? []).filter((i) => i.status === 'PENDING')
    if (selectable.every((i) => selectedIds.has(i.id))) {
      setSelectedIds(new Set())
    } else {
      setSelectedIds(new Set(selectable.map((i) => i.id)))
    }
  }

  async function bulkMarkReceived() {
    const targets = (prescription?.items ?? []).filter((i) => selectedIds.has(i.id) && i.status === 'PENDING')
    if (!targets.length) return
    withPin(async () => {
      for (const i of targets) await markItemAsReceived(id!, i.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  async function bulkDelete() {
    const targets = (prescription?.items ?? []).filter((i) => selectedIds.has(i.id) && i.status === 'PENDING')
    if (!targets.length) return
    if (!await confirm(`Remover ${targets.length} medicamento(s) da pendência?`)) return
    withPin(async () => {
      for (const i of targets) await deletePrescriptionItem(id!, i.id)
      setSelectedIds(new Set())
      invalidate()
    })
  }

  async function handleDelete() {
    if (!await confirm('Excluir esta pendência de receita? Esta ação não pode ser desfeita.')) return
    withPin(() => deleteMutation.mutate())
  }

  async function handleDeleteItem(item: PrescriptionItem) {
    if (!await confirm(`Remover "${item.product}" da pendência?`)) return
    withPin(() => deleteItemMutation.mutate(item.id))
  }

  if (isLoading) return <div className="flex justify-center py-20"><Spinner /></div>
  if (error) return <div className="p-6"><ErrorMessage error={error} /></div>
  if (!prescription) return null

  const isPending = prescription.status === 'PENDING'
  const hasPending = prescription.items.some((i) => i.status === 'PENDING')
  const selectablePending = (prescription?.items ?? []).filter((i) => i.status === 'PENDING')
  const allPendingSelected = selectablePending.length > 0 && selectablePending.every((i) => selectedIds.has(i.id))
  const someSelected = selectedIds.size > 0

  return (
    <div>
      <PageHeader
        title={`Receita — ${prescription.customerName}`}
        description={`Criada por ${prescription.createdByName} em ${formatDate(prescription.createdAt)}`}
        actions={
          <div className="flex items-center gap-2 flex-wrap">
            <Button variant="ghost" size="sm" onClick={() => navigate('/prescriptions')}>
              <ArrowLeft size={13} /> Voltar
            </Button>
            {isPending && (
              <div className="flex items-center gap-2 border-l border-gray-200 pl-3">
                <Button variant="secondary" size="sm" onClick={openEdit}>
                  <Settings2 size={13} /> Editar
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => withPin(() => markAllMutation.mutate())}
                  disabled={!hasPending || markAllMutation.isPending}
                >
                  Todos recebidos
                </Button>
                <Button variant="danger" size="sm" onClick={handleDelete} disabled={deleteMutation.isPending}>
                  <Trash2 size={13} /> Excluir
                </Button>
              </div>
            )}
          </div>
        }
      />

      <div className="p-6 space-y-4">
        <div className="bg-white border border-gray-200 rounded-lg p-4 grid grid-cols-2 gap-x-8 gap-y-3 text-sm sm:grid-cols-3">
          <div>
            <span className="text-gray-500 text-xs uppercase tracking-wide">Status</span>
            <div className="mt-1"><PrescriptionStatusBadge status={prescription.status} /></div>
          </div>
          <div>
            <span className="text-gray-500 text-xs uppercase tracking-wide">Cliente</span>
            <p className="mt-1 font-medium text-gray-900">{prescription.customerName}</p>
          </div>
          <div>
            <span className="text-gray-500 text-xs uppercase tracking-wide">Criado por</span>
            <p className="mt-1 text-gray-700">{prescription.createdByName}</p>
          </div>
          {prescription.observations && (
            <div className="col-span-2">
              <span className="text-gray-500 text-xs uppercase tracking-wide">Observações</span>
              <p className="mt-1 text-gray-700">{prescription.observations}</p>
            </div>
          )}
        </div>

        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
            <span className="text-sm font-medium text-gray-900">Medicamentos</span>
            {isPending && (
              <Button variant="secondary" size="sm" onClick={openAddItem}>
                <Plus size={12} /> Adicionar medicamento
              </Button>
            )}
          </div>

          {someSelected && (
            <div className="flex items-center gap-3 px-4 py-2 bg-blue-50 border-b border-blue-100 text-sm">
              <button onClick={() => setSelectedIds(new Set())} className="text-blue-500 hover:text-blue-700 cursor-pointer">
                <X size={14} />
              </button>
              <span className="text-blue-700 font-medium">{selectedIds.size} selecionado(s)</span>
              <div className="flex items-center gap-2 ml-2">
                {(prescription?.items ?? []).some((i) => selectedIds.has(i.id) && i.status === 'PENDING') && (
                  <Button variant="secondary" size="sm" onClick={bulkMarkReceived}>Marcar recebido</Button>
                )}
                <Button variant="danger" size="sm" onClick={bulkDelete}>
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
                      checked={allPendingSelected}
                      onChange={toggleSelectAll}
                      className="accent-gray-700 cursor-pointer"
                    />
                  </Th>
                )}
                <Th>Medicamento</Th>
                <Th>Qtd.</Th>
                <Th>Lote</Th>
                <Th>Validade</Th>
                <Th>Status</Th>
                <Th>Recebido por</Th>
                {isPending && <Th />}
              </tr>
            </TableHead>
            <TableBody>
              {prescription.items.length === 0 && (
                <tr>
                  <Td colSpan={isPending ? 8 : 7} className="text-center text-gray-400 py-8">
                    Nenhum medicamento cadastrado.
                  </Td>
                </tr>
              )}
              {prescription.items.map((item) => (
                <Tr key={item.id}>
                  {isPending && (
                    <Td>
                      {item.status === 'PENDING' && (
                        <input
                          type="checkbox"
                          checked={selectedIds.has(item.id)}
                          onChange={() => toggleSelectItem(item.id)}
                          className="accent-gray-700 cursor-pointer"
                        />
                      )}
                    </Td>
                  )}
                  <Td>
                    <span className="font-medium text-gray-900 block max-w-[200px] truncate" title={item.product}>
                      {item.product}
                    </span>
                  </Td>
                  <Td>{item.quantity}</Td>
                  <Td className="text-gray-500">{item.batch}</Td>
                  <Td className="text-gray-500">{item.expiry}</Td>
                  <Td><PrescriptionItemStatusBadge status={item.status} /></Td>
                  <Td className="text-gray-500">{item.receivedByName ?? '—'}</Td>
                  {isPending && (
                    <Td>
                      <div className="flex items-center gap-1 justify-end">
                        {item.status === 'PENDING' && (
                          <Button variant="ghost" size="sm"
                            onClick={() => withPin(() => markItemAsReceived(id!, item.id).then(invalidate))}>
                            Marcar recebido
                          </Button>
                        )}
                        {item.status === 'PENDING' && (
                          <div className="flex items-center gap-1 border-l border-gray-200 pl-2 ml-1">
                            <Button variant="ghost" size="sm" onClick={() => openEditItem(item)}>
                              <Pencil size={12} />
                            </Button>
                            <Button variant="ghost" size="sm"
                              className="text-red-400 hover:text-red-600"
                              onClick={() => handleDeleteItem(item)}
                              disabled={deleteItemMutation.isPending}>
                              <Trash2 size={12} />
                            </Button>
                          </div>
                        )}
                      </div>
                    </Td>
                  )}
                </Tr>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>

      {/* Edit prescription dialog */}
      <Dialog open={editOpen} onOpenChange={(v) => !v && setEditOpen(false)} title="Editar pendência">
        <form onSubmit={(e) => { e.preventDefault(); withPin(() => updateMutation.mutate()) }} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Cliente</label>
            <CustomerSearch value={editCustomer} onChange={setEditCustomer} onQuickAdd={() => setQuickAddOpen(true)} />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Observações <span className="text-gray-400 font-normal">(opcional)</span>
            </label>
            <Input value={editObservations} onChange={(e) => setEditObservations(e.target.value)}
              placeholder="Observações sobre a pendência..." maxLength={500} autoComplete="off" />
          </div>
          {updateMutation.isError && <ErrorMessage error={updateMutation.error} />}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setEditOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="primary" disabled={!editCustomer || updateMutation.isPending}>
              {updateMutation.isPending ? 'Salvando...' : 'Salvar'}
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Item add/edit dialog */}
      <Dialog open={itemDialogOpen} onOpenChange={(v) => !v && closeItemDialog()}
        title={editingItem ? 'Editar medicamento' : 'Adicionar medicamento'}>
        <form onSubmit={(e) => { e.preventDefault(); withPin(() => saveItemMutation.mutate()) }} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Medicamento</label>
            <Input value={itemForm.product}
              onChange={(e) => setItemForm((p) => ({ ...p, product: e.target.value }))}
              required autoFocus autoComplete="off" maxLength={150} />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Quantidade</label>
            <Input type="number" min={1} max={999} step={1} value={itemForm.quantity}
              onKeyDown={(e) => ['e', 'E', '+', '-', '.', ','].includes(e.key) && e.preventDefault()}
              onChange={(e) => {
                const val = e.target.value.replace(/[^0-9]/g, '')
                setItemForm((p) => ({ ...p, quantity: val ? String(Math.min(parseInt(val, 10), 999)) : '' }))
              }}
              required />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Lote</label>
            <Input value={itemForm.batch}
              onChange={(e) => setItemForm((p) => ({ ...p, batch: e.target.value }))}
              required autoComplete="off" maxLength={50} />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Validade</label>
            <Input value={itemForm.expiry}
              onChange={(e) => setItemForm((p) => ({ ...p, expiry: formatExpiry(e.target.value) }))}
              placeholder="MM/yyyy" maxLength={7} required
              pattern="^(0[1-9]|1[0-2])/\d{4}$" title="Formato: MM/yyyy (ex: 03/2026)" />
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

      {/* Quick add customer dialog */}
      <Dialog open={quickAddOpen} onOpenChange={(v) => !v && setQuickAddOpen(false)} title="Cadastrar cliente">
        <form onSubmit={(e) => { e.preventDefault(); quickAddMutation.mutate() }} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Nome</label>
            <Input value={newCustomerName} onChange={(e) => setNewCustomerName(e.target.value)}
              required autoFocus autoComplete="off" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Telefone <span className="text-gray-400">(opcional)</span>
            </label>
            <PhoneInput value={newCustomerPhone} onChange={setNewCustomerPhone} />
          </div>
          {quickAddMutation.isError && <ErrorMessage error={quickAddMutation.error} />}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setQuickAddOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="primary" disabled={!newCustomerName.trim() || quickAddMutation.isPending}>
              {quickAddMutation.isPending ? 'Salvando...' : 'Salvar'}
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  )
}
