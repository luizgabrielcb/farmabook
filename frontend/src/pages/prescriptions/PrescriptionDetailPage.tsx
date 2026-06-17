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
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { CardList, MobileCard, CardActions, IconAction } from '@/components/ui/mobile-card'
import { PrescriptionStatusBadge, PrescriptionItemStatusBadge } from '@/components/shared/StatusBadge'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { CustomerSearch } from '@/components/shared/CustomerSearch'
import { PhoneInput } from '@/components/shared/PhoneInput'
import { createCustomer } from '@/api/customers'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { useToast } from '@/context/ToastContext'
import { formatDate } from '@/lib/utils'
import { ArrowLeft, Plus, Trash2, Pencil, Settings2, X } from 'lucide-react'
import type { Customer, PrescriptionItem } from '@/types'

interface ItemForm { product: string; quantity: string; batch: string; expiry: string }
const emptyItemForm: ItemForm = { product: '', quantity: '', batch: '', expiry: '' }

type DetailTab = 'medicamentos' | 'historico'

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
  const toast = useToast()

  const [tab, setTab] = useState<DetailTab>('medicamentos')
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
    onSuccess: () => { toast.success('Pendência excluída'); navigate('/prescriptions') },
  })

  const updateMutation = useMutation({
    mutationFn: () =>
      updatePrescription(id!, {
        customerId: editCustomer!.id,
        observations: editObservations.trim() || null,
      }),
    onSuccess: () => { toast.success('Alterações salvas'); setEditOpen(false); invalidate() },
  })

  const markAllMutation = useMutation({
    mutationFn: () => markAllAsReceived(id!),
    onSuccess: () => { toast.success('Todos os itens marcados como recebidos'); invalidate() },
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
    onSuccess: () => { toast.success(editingItem ? 'Medicamento atualizado' : 'Medicamento adicionado'); closeItemDialog(); invalidate() },
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
    setEditCustomer(prescription ? { id: prescription.customerId!, name: prescription.customerName!, phoneNumber: null, createdAt: '', updatedAt: '' } : null)
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
  const selectablePending = prescription.items.filter((i) => i.status === 'PENDING')
  const allPendingSelected = selectablePending.length > 0 && selectablePending.every((i) => selectedIds.has(i.id))
  const someSelected = selectedIds.size > 0

  const tabs: { value: DetailTab; label: string; count?: number }[] = [
    { value: 'medicamentos', label: 'Medicamentos', count: prescription.items.length },
    { value: 'historico', label: 'Histórico' },
  ]

  return (
    <div className="p-4 sm:p-6">
      <button
        onClick={() => navigate('/prescriptions')}
        className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 mb-4 cursor-pointer transition-colors"
      >
        <ArrowLeft size={15} /> Receitas
      </button>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_300px] gap-4 items-start">
        {/* MAIN CARD */}
        <div className="bg-white border border-gray-150 rounded-2xl shadow-sm overflow-hidden">
          {/* header */}
          <div className="p-5 border-b border-gray-150">
            <div className="flex items-start gap-3">
              <p className="flex-1 min-w-0 font-bold text-gray-900 text-[17px]">{prescription.customerName}</p>
              <PrescriptionStatusBadge status={prescription.status} />
            </div>
            <p className="text-xs text-gray-400 mt-1">
              Criada por {prescription.createdByName} em {formatDate(prescription.createdAt)}
            </p>
            {prescription.observations && (
              <div className="mt-3 pt-3 border-t border-gray-100">
                <span className="text-xs font-semibold uppercase tracking-wide text-gray-400">Observações</span>
                <p className="text-sm text-gray-700 mt-1 break-words">{prescription.observations}</p>
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

          {/* MEDICAMENTOS TAB */}
          {tab === 'medicamentos' && (
            <div>
              {isPending && (
                <div className="flex items-center justify-between px-4 py-3 border-b border-gray-150">
                  <span className="text-xs text-gray-400">{prescription.items.length} medicamento(s)</span>
                  <Button variant="secondary" size="sm" onClick={openAddItem}>
                    <Plus size={12} /> Adicionar medicamento
                  </Button>
                </div>
              )}

              {someSelected && (
                <div className="flex items-center gap-3 px-4 py-2 bg-brand-50 border-b border-brand-100 text-sm flex-wrap">
                  <button onClick={() => setSelectedIds(new Set())} className="text-brand-500 hover:text-brand-700 cursor-pointer">
                    <X size={14} />
                  </button>
                  <span className="text-brand-700 font-medium">{selectedIds.size} selecionado(s)</span>
                  <div className="flex items-center gap-2 ml-2">
                    {prescription.items.some((i) => selectedIds.has(i.id) && i.status === 'PENDING') && (
                      <Button variant="secondary" size="sm" onClick={bulkMarkReceived}>Marcar recebido</Button>
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
                    {isPending && (
                      <Th className="w-8">
                        <input type="checkbox" checked={allPendingSelected} onChange={toggleSelectAll} className="accent-gray-700 cursor-pointer" />
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
                            <input type="checkbox" checked={selectedIds.has(item.id)} onChange={() => toggleSelectItem(item.id)} className="accent-gray-700 cursor-pointer" />
                          )}
                        </Td>
                      )}
                      <Td>
                        <span className="font-medium text-gray-900 block max-w-[200px] break-words whitespace-normal" title={item.product}>
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
                          <div className="flex items-center gap-1 justify-end whitespace-nowrap">
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

              <CardList>
                {prescription.items.length === 0 && (
                  <p className="text-center text-gray-400 py-8 text-sm">Nenhum medicamento cadastrado.</p>
                )}
                {prescription.items.map((item) => (
                  <MobileCard key={item.id}>
                    <div className="flex items-start gap-2">
                      {isPending && item.status === 'PENDING' && (
                        <input type="checkbox" checked={selectedIds.has(item.id)} onChange={() => toggleSelectItem(item.id)} className="accent-gray-700 cursor-pointer mt-1 h-4 w-4 shrink-0" />
                      )}
                      <span className="font-medium text-gray-900 break-words min-w-0 flex-1">{item.product}</span>
                      <PrescriptionItemStatusBadge status={item.status} />
                    </div>
                    <div className="flex items-center justify-between gap-3 text-sm text-gray-400">
                      <span>Qtd: <span className="text-gray-700">{item.quantity}</span></span>
                      <span>Lote: <span className="text-gray-700">{item.batch}</span></span>
                      <span>Val: <span className="text-gray-700">{item.expiry}</span></span>
                    </div>
                    {item.receivedByName && (
                      <div className="text-sm text-gray-400">Recebido por: <span className="text-gray-700">{item.receivedByName}</span></div>
                    )}
                    {isPending && item.status === 'PENDING' && (
                      <CardActions>
                        <Button variant="ghost" className="h-11 px-3" onClick={() => withPin(() => markItemAsReceived(id!, item.id).then(invalidate))}>
                          Marcar recebido
                        </Button>
                        <IconAction label="Editar" onClick={() => openEditItem(item)}><Pencil size={17} /></IconAction>
                        <IconAction label="Excluir" className="text-red-500" onClick={() => handleDeleteItem(item)} disabled={deleteItemMutation.isPending}><Trash2 size={17} /></IconAction>
                      </CardActions>
                    )}
                  </MobileCard>
                ))}
              </CardList>
            </div>
          )}

          {/* HISTÓRICO TAB */}
          {tab === 'historico' && (
            <div className="p-5 space-y-5 text-sm">
              <div className="flex items-center gap-3">
                <span className="w-24 text-gray-500 shrink-0">Criado</span>
                <span className="text-gray-900 font-medium">{prescription.createdByName}</span>
                <span className="text-gray-500 text-[13px]">{formatDate(prescription.createdAt)}</span>
              </div>

              {prescription.items.some((i) => i.receivedByName) && (
                <div className="pt-4 border-t border-gray-150">
                  <span className="text-xs font-semibold uppercase tracking-wide text-gray-500">Recebimento dos medicamentos</span>
                  <div className="mt-3 space-y-3">
                    {prescription.items.filter((i) => i.receivedByName).map((item) => (
                      <div key={item.id}>
                        <p className="font-semibold text-gray-900 mb-0.5 break-words">{item.product}</p>
                        <span className="text-[13px] text-gray-600">
                          <span className="text-gray-400">Recebido:</span> {item.receivedByName}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* SIDEBAR — AÇÕES RÁPIDAS */}
        <div className="bg-white border border-gray-150 rounded-2xl shadow-sm p-4">
          <p className="text-sm font-semibold text-gray-900 mb-3">Ações rápidas</p>
          <div className="flex flex-col gap-2">
            {isPending ? (
              <Button variant="secondary" size="md" className="w-full"
                disabled={!hasPending || markAllMutation.isPending}
                onClick={() => withPin(() => markAllMutation.mutate())}>
                Marcar todos recebidos
              </Button>
            ) : (
              <div className="text-center text-sm font-medium text-brand-700 bg-brand-50 rounded-md py-2">Receita finalizada</div>
            )}

            {isPending && (
              <>
                <div className="h-px bg-gray-100 my-1" />
                <Button variant="secondary" size="md" className="w-full" onClick={openEdit}>
                  <Settings2 size={14} /> Editar
                </Button>
                <Button
                  variant="ghost"
                  size="md"
                  className="w-full bg-red-50 text-red-600 border border-red-100 hover:bg-red-100"
                  onClick={handleDelete}
                  disabled={deleteMutation.isPending}
                >
                  <Trash2 size={14} /> Excluir
                </Button>
              </>
            )}
          </div>
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
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="text-xs font-medium text-gray-700 block mb-1">Qtd.</label>
              <Input type="number" min={1} max={999} step={1} value={itemForm.quantity}
                onKeyDown={(e) => ['e', 'E', '+', '-', '.', ','].includes(e.key) && e.preventDefault()}
                onChange={(e) => {
                  const val = e.target.value.replace(/[^0-9]/g, '')
                  if (val && parseInt(val, 10) > 999) return
                  setItemForm((p) => ({ ...p, quantity: val }))
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
