import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import { createPrescription } from '@/api/prescriptions'
import { createCustomer } from '@/api/customers'
import { Dialog } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { CustomerSearch } from '@/components/shared/CustomerSearch'
import { PhoneInput } from '@/components/shared/PhoneInput'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { useWithPin } from '@/context/PinContext'
import type { Customer } from '@/types'

interface ItemForm {
  product: string
  quantity: string
  batch: string
  expiry: string
}

const emptyItem = (): ItemForm => ({ product: '', quantity: '', batch: '', expiry: '' })

interface Props {
  open: boolean
  onClose: () => void
  onSuccess: (id: string) => void
}

function formatExpiry(raw: string): string {
  const digits = raw.replace(/\D/g, '').slice(0, 6)
  if (digits.length <= 2) return digits
  return digits.slice(0, 2) + '/' + digits.slice(2)
}

export function CreatePrescriptionDialog({ open, onClose, onSuccess }: Props) {
  const [customer, setCustomer] = useState<Customer | null>(null)
  const [items, setItems] = useState<ItemForm[]>([emptyItem()])
  const [observations, setObservations] = useState('')
  const [quickAddOpen, setQuickAddOpen] = useState(false)
  const [newCustomerName, setNewCustomerName] = useState('')
  const [newCustomerPhone, setNewCustomerPhone] = useState('')
  const withPin = useWithPin()
  const qc = useQueryClient()

  function reset() {
    setCustomer(null)
    setItems([emptyItem()])
    setObservations('')
    setQuickAddOpen(false)
    setNewCustomerName('')
    setNewCustomerPhone('')
    createMutation.reset()
  }

  function handleClose() {
    reset()
    onClose()
  }

  const createMutation = useMutation({
    mutationFn: () =>
      createPrescription({
        customerId: customer!.id,
        items: items
          .filter((i) => i.product.trim())
          .map((i) => ({
            product: i.product,
            quantity: Number(i.quantity) || 1,
            batch: i.batch,
            expiry: i.expiry,
          })),
        observations: observations.trim() || null,
      }),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ['prescriptions-all'] })
      reset()
      onSuccess(created.id)
    },
  })

  const quickAddMutation = useMutation({
    mutationFn: () =>
      createCustomer({ name: newCustomerName, phoneNumber: newCustomerPhone || undefined }),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ['customers-all'] })
      setCustomer(created)
      setQuickAddOpen(false)
      setNewCustomerName('')
      setNewCustomerPhone('')
    },
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!customer) return
    withPin(() => createMutation.mutate())
  }

  function addItem() { setItems((p) => [...p, emptyItem()]) }
  function removeItem(i: number) { setItems((p) => p.filter((_, idx) => idx !== i)) }
  function updateItem(i: number, field: keyof ItemForm, value: string) {
    setItems((p) => p.map((item, idx) => (idx === i ? { ...item, [field]: value } : item)))
  }

  return (
    <>
      <Dialog
        open={open}
        onOpenChange={(v) => !v && handleClose()}
        title="Nova pendência de receita"
        description="Selecione o cliente e adicione os medicamentos"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Cliente</label>
            <CustomerSearch value={customer} onChange={setCustomer} onQuickAdd={() => setQuickAddOpen(true)} />
          </div>

          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-medium text-gray-700">Medicamentos</label>
              <Button type="button" variant="ghost" size="sm" onClick={addItem}>
                <Plus size={12} /> Adicionar
              </Button>
            </div>
            <div className="space-y-2 max-h-[300px] overflow-y-auto pr-1">
              {items.map((item, i) => (
                <div key={i} className="border border-gray-100 rounded-lg p-2.5 space-y-2 bg-gray-50">
                  <div className="flex gap-2">
                    <Input
                      value={item.product}
                      onChange={(e) => updateItem(i, 'product', e.target.value)}
                      placeholder="Medicamento"
                      className="flex-1"
                      autoComplete="off"
                      maxLength={150}
                      required
                    />
                    <Input
                      type="number"
                      min={1}
                      max={999}
                      step={1}
                      value={item.quantity}
                      onKeyDown={(e) => ['e', 'E', '+', '-', '.', ','].includes(e.key) && e.preventDefault()}
                      onChange={(e) => {
                        const val = e.target.value.replace(/[^0-9]/g, '')
                        updateItem(i, 'quantity', val ? String(Math.min(parseInt(val, 10), 999)) : '')
                      }}
                      placeholder="Qtd"
                      className="w-16"
                      required
                    />
                    {items.length > 1 && (
                      <Button type="button" variant="ghost" size="sm"
                        className="text-red-400 hover:text-red-600 shrink-0" onClick={() => removeItem(i)}>
                        <Trash2 size={12} />
                      </Button>
                    )}
                  </div>
                  <div className="flex gap-2">
                    <Input
                      value={item.batch}
                      onChange={(e) => updateItem(i, 'batch', e.target.value)}
                      placeholder="Lote"
                      className="flex-1"
                      autoComplete="off"
                      maxLength={50}
                      required
                    />
                    <Input
                      value={item.expiry}
                      onChange={(e) => updateItem(i, 'expiry', formatExpiry(e.target.value))}
                      placeholder="MM/yyyy"
                      className="w-28"
                      maxLength={7}
                      required
                      pattern="^(0[1-9]|1[0-2])/\d{4}$"
                      title="Formato: MM/yyyy (ex: 03/2026)"
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Observações <span className="text-gray-400">(opcional)</span>
            </label>
            <Input
              value={observations}
              onChange={(e) => setObservations(e.target.value)}
              placeholder="Observações sobre a pendência..."
              maxLength={500}
              autoComplete="off"
            />
          </div>

          {createMutation.isError && <ErrorMessage error={createMutation.error} />}

          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={handleClose}>Cancelar</Button>
            <Button type="submit" variant="primary"
              disabled={!customer || items.every((i) => !i.product.trim()) || createMutation.isPending}>
              {createMutation.isPending ? 'Criando...' : 'Criar pendência'}
            </Button>
          </div>
        </form>
      </Dialog>

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
    </>
  )
}
