import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createCompounding } from '@/api/compoundings'
import { createCustomer } from '@/api/customers'
import { Dialog } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { CustomerSearch } from '@/components/shared/CustomerSearch'
import { PharmacySearch } from '@/components/shared/PharmacySearch'
import { PhoneInput } from '@/components/shared/PhoneInput'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { useWithPin } from '@/context/PinContext'
import type { Customer, CompoundingPharmacy } from '@/types'

interface Props {
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

export function CreateCompoundingDialog({ open, onClose, onSuccess }: Props) {
  const [customer, setCustomer] = useState<Customer | null>(null)
  const [pharmacy, setPharmacy] = useState<CompoundingPharmacy | null>(null)
  const [quantity, setQuantity] = useState('')
  const [value, setValue] = useState('')
  const [observations, setObservations] = useState('')
  const [quickAddOpen, setQuickAddOpen] = useState(false)
  const [newCustomerName, setNewCustomerName] = useState('')
  const [newCustomerPhone, setNewCustomerPhone] = useState('')
  const withPin = useWithPin()
  const qc = useQueryClient()

  function reset() {
    setCustomer(null)
    setPharmacy(null)
    setQuantity('')
    setValue('')
    setObservations('')
    setQuickAddOpen(false)
    setNewCustomerName('')
    setNewCustomerPhone('')
    createMutation.reset()
  }

  function handleClose() { reset(); onClose() }

  const createMutation = useMutation({
    mutationFn: () => createCompounding({
      customerId: customer!.id,
      pharmacyId: pharmacy!.id,
      quantity: Number(quantity),
      value: value ? Number(value) : null,
      observations: observations.trim() || null,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['compoundings-all'] })
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
    if (!customer || !pharmacy || !quantity) return
    withPin(() => createMutation.mutate())
  }

  return (
    <>
      <Dialog open={open} onOpenChange={(v) => !v && handleClose()}
        title="Nova manipulação" description="Selecione o cliente e a farmácia">
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Cliente</label>
            <CustomerSearch value={customer} onChange={setCustomer} onQuickAdd={() => setQuickAddOpen(true)} />
            {customer?.phoneNumber && (
              <p className="text-xs text-gray-400 mt-1">{customer.phoneNumber}</p>
            )}
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Farmácia</label>
            <PharmacySearch value={pharmacy} onChange={setPharmacy} />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Quantidade</label>
            <Input type="number" min={1} value={quantity}
              onChange={(e) => setQuantity(e.target.value)} required autoComplete="off" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Valor <span className="text-gray-400">(opcional)</span>
            </label>
            <Input type="number" min={0} max={100000} step="0.01" value={value}
              onChange={(e) => {
                const v = e.target.value
                if (v === '' || /^\d{0,6}(\.\d{0,2})?$/.test(v)) setValue(v)
              }} placeholder="R$ 0,00" autoComplete="off" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">
              Observações <span className="text-gray-400">(opcional)</span>
            </label>
            <Input value={observations} onChange={(e) => setObservations(e.target.value)}
              maxLength={500} placeholder="..." autoComplete="off" />
          </div>
          {createMutation.isError && <ErrorMessage error={createMutation.error} />}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={handleClose}>Cancelar</Button>
            <Button type="submit" variant="primary"
              disabled={!customer || !pharmacy || !quantity || createMutation.isPending}>
              {createMutation.isPending ? 'Criando...' : 'Criar manipulação'}
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
