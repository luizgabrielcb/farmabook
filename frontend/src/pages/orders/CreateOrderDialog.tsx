import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import { createOrder } from '@/api/orders'
import { createCustomer } from '@/api/customers'
import { Dialog } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { CustomerSearch } from '@/components/shared/CustomerSearch'
import { PhoneInput } from '@/components/shared/PhoneInput'
import { CATEGORY_OPTIONS } from '@/components/shared/CategoryBadge'
import { PriceInput, parsePriceInput } from '@/components/shared/PriceInput'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { useWithPin } from '@/context/PinContext'
import type { Customer, Category } from '@/types'

interface ItemForm { product: string; category: Category; quantity: string; price: string }
const emptyItem = (): ItemForm => ({ product: '', category: 'MEDICAMENTOS', quantity: '', price: '' })

interface Props {
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

export function CreateOrderDialog({ open, onClose, onSuccess }: Props) {
  const [customer, setCustomer] = useState<Customer | null>(null)
  const [items, setItems] = useState<ItemForm[]>([emptyItem()])
  const [observations, setObservations] = useState('')
  const [totalPrice, setTotalPrice] = useState('')
  const [quickAddOpen, setQuickAddOpen] = useState(false)
  const [newCustomerName, setNewCustomerName] = useState('')
  const [newCustomerPhone, setNewCustomerPhone] = useState('')
  const withPin = useWithPin()
  const qc = useQueryClient()

  function reset() {
    setCustomer(null)
    setItems([emptyItem()])
    setObservations('')
    setTotalPrice('')
    setQuickAddOpen(false)
    setNewCustomerName('')
    setNewCustomerPhone('')
    createMutation.reset()
  }

  function handleClose() { reset(); onClose() }

  const createMutation = useMutation({
    mutationFn: () =>
      createOrder({
        customerId: customer!.id,
        items: items
          .filter((i) => i.product.trim())
          .map((i) => ({
            product: i.product,
            category: i.category,
            quantity: i.quantity ? Number(i.quantity) : 1,
            price: parsePriceInput(i.price),
          })),
        observations: observations.trim() || null,
        totalPrice: parsePriceInput(totalPrice),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['orders-all'] })
      reset()
      onSuccess()
    },
  })

  const quickAddMutation = useMutation({
    mutationFn: () => createCustomer({ name: newCustomerName, phoneNumber: newCustomerPhone || undefined }),
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
    setItems((p) => p.map((item, idx) => idx === i ? { ...item, [field]: value } : item))
  }

  return (
    <>
      <Dialog open={open} onOpenChange={(v) => !v && handleClose()}
        title="Nova encomenda" description="Selecione o cliente e adicione os itens">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Cliente</label>
            <CustomerSearch
              value={customer}
              onChange={setCustomer}
              onQuickAdd={() => setQuickAddOpen(true)}
            />
            {customer?.phoneNumber && (
              <p className="text-xs text-gray-400 mt-1">{customer.phoneNumber}</p>
            )}
          </div>

          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-medium text-gray-700">Itens</label>
              <Button type="button" variant="ghost" size="sm" onClick={addItem}>
                <Plus size={12} /> Adicionar
              </Button>
            </div>
            <div className="space-y-2">
              {items.map((item, i) => (
                <div key={i} className="flex gap-2 items-start">
                  <Input
                    value={item.product}
                    onChange={(e) => updateItem(i, 'product', e.target.value)}
                    placeholder="Produto"
                    className="flex-1"
                    autoComplete="off"
                    maxLength={150}
                  />
                  <Select value={item.category}
                    onChange={(e) => updateItem(i, 'category', e.target.value)}
                    className="w-36">
                    {CATEGORY_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                  </Select>
                  <Input
                    type="number"
                    min={1}
                    max={1000}
                    step={1}
                    value={item.quantity}
                    onKeyDown={(e) => ['e', 'E', '+', '-', '.', ','].includes(e.key) && e.preventDefault()}
                    onChange={(e) => updateItem(i, 'quantity', e.target.value)}
                    placeholder="Qtd"
                    className="w-16"
                    required
                  />
                  <PriceInput
                    value={item.price}
                    onChange={(v) => updateItem(i, 'price', v)}
                    placeholder="Preço un."
                    className="w-24"
                  />
                  {items.length > 1 && (
                    <Button type="button" variant="ghost" size="sm"
                      className="text-red-400 hover:text-red-600 shrink-0"
                      onClick={() => removeItem(i)}>
                      <Trash2 size={12} />
                    </Button>
                  )}
                </div>
              ))}
            </div>
          </div>

          <div className="flex gap-3">
            <div className="flex-1">
              <label className="text-xs font-medium text-gray-700 block mb-1">
                Observações <span className="text-gray-400">(opcional)</span>
              </label>
              <Input
                value={observations}
                onChange={(e) => setObservations(e.target.value)}
                placeholder="Observações sobre a encomenda..."
                maxLength={500}
                autoComplete="off"
              />
            </div>
            <div className="w-36">
              <label className="text-xs font-medium text-gray-700 block mb-1">
                Total <span className="text-gray-400">(opcional)</span>
              </label>
              <PriceInput value={totalPrice} onChange={setTotalPrice} />
            </div>
          </div>

          {createMutation.isError && <ErrorMessage error={createMutation.error} />}

          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={handleClose}>Cancelar</Button>
            <Button type="submit" variant="primary"
              disabled={!customer || items.every((i) => !i.product.trim()) || createMutation.isPending}>
              {createMutation.isPending ? 'Criando...' : 'Criar encomenda'}
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
            <Button type="submit" variant="primary" disabled={quickAddMutation.isPending}>
              {quickAddMutation.isPending ? 'Salvando...' : 'Salvar'}
            </Button>
          </div>
        </form>
      </Dialog>
    </>
  )
}
