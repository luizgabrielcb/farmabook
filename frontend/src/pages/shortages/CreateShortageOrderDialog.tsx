import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import { createShortageOrder } from '@/api/shortageOrders'
import { Dialog } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { DistributorSearch } from '@/components/shared/DistributorSearch'
import { PriceInput, parsePriceInput } from '@/components/shared/PriceInput'
import { CATEGORY_OPTIONS } from '@/components/shared/CategoryBadge'
import { useWithPin } from '@/context/PinContext'
import type { Category, Distributor, ShortageType } from '@/types'

interface ItemForm {
  product: string
  category: Category
  quantity: string
  costPrice: string
}

const emptyItem = (): ItemForm => ({ product: '', category: 'MEDICAMENTOS', quantity: '', costPrice: '' })

interface Props {
  open: boolean
  onOpenChange: (v: boolean) => void
  shortageType: ShortageType
  label: string
}

export function CreateShortageOrderDialog({ open, onOpenChange, shortageType, label }: Props) {
  const [distributor, setDistributor] = useState<Distributor | null>(null)
  const [observations, setObservations] = useState('')
  const [items, setItems] = useState<ItemForm[]>([emptyItem()])
  const withPin = useWithPin()
  const qc = useQueryClient()

  const mutation = useMutation({
    mutationFn: () =>
      createShortageOrder({
        shortageType,
        distributorId: distributor!.id,
        observations: observations.trim() || null,
        items: items.map((i) => ({
          product: i.product,
          category: i.category,
          quantity: i.quantity ? Number(i.quantity) : null,
          costPrice: parsePriceInput(i.costPrice),
        })),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['shortage-orders', shortageType] })
      qc.invalidateQueries({ queryKey: ['shortages-all'] })
      handleClose()
    },
  })

  function handleClose() {
    onOpenChange(false)
    setDistributor(null)
    setObservations('')
    setItems([emptyItem()])
    mutation.reset()
  }

  function updateItem(index: number, field: keyof ItemForm, value: string) {
    setItems((prev) => prev.map((item, i) => (i === index ? { ...item, [field]: value } : item)))
  }

  function addItem() {
    setItems((prev) => [...prev, emptyItem()])
  }

  function removeItem(index: number) {
    setItems((prev) => prev.filter((_, i) => i !== index))
  }

  const canSubmit = !!distributor && items.length > 0 && items.every((i) => i.product.trim())

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    withPin(() => mutation.mutate())
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => !v && handleClose()}
      title={`Novo pedido — ${label}`}
      description="Selecione a distribuidora e adicione os produtos em falta"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="text-xs font-medium text-gray-700 block mb-1">Distribuidora</label>
          <DistributorSearch value={distributor} onChange={setDistributor} />
        </div>

        <div>
          <label className="text-xs font-medium text-gray-700 block mb-1">
            Observações <span className="text-gray-400 font-normal">(opcional)</span>
          </label>
          <Input
            value={observations}
            onChange={(e) => setObservations(e.target.value)}
            placeholder="Observações sobre o pedido..."
            maxLength={500}
            autoComplete="off"
          />
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <label className="text-xs font-medium text-gray-700">Produtos em falta</label>
            <Button type="button" variant="ghost" size="sm" onClick={addItem}>
              <Plus size={12} /> Adicionar produto
            </Button>
          </div>

          <div className="max-h-[320px] overflow-y-auto space-y-2 pr-1">
            {items.map((item, index) => (
              <div key={index} className="flex gap-2 items-start p-3 bg-gray-50 rounded-lg">
                <div className="flex-1 space-y-2">
                  <Input
                    value={item.product}
                    onChange={(e) => updateItem(index, 'product', e.target.value)}
                    placeholder="Nome do produto"
                    maxLength={150}
                    required
                    autoComplete="off"
                  />
                  <div className="flex gap-2">
                    <Select
                      value={item.category}
                      onChange={(e) => updateItem(index, 'category', e.target.value)}
                      className="flex-1"
                    >
                      {CATEGORY_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                      ))}
                    </Select>
                    <Input
                      type="number"
                      min={1}
                      value={item.quantity}
                      onChange={(e) => updateItem(index, 'quantity', e.target.value)}
                      placeholder="Qtd (opc.)"
                      className="w-28"
                    />
                    <PriceInput
                      value={item.costPrice}
                      onChange={(v) => updateItem(index, 'costPrice', v)}
                      placeholder="Custo (opc.)"
                      className="w-32"
                    />
                  </div>
                </div>
                {items.length > 1 && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="text-red-400 hover:text-red-600 mt-1 shrink-0"
                    onClick={() => removeItem(index)}
                  >
                    <Trash2 size={13} />
                  </Button>
                )}
              </div>
            ))}
          </div>
        </div>

        {mutation.isError && <ErrorMessage error={mutation.error} />}

        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="secondary" onClick={handleClose}>Cancelar</Button>
          <Button type="submit" variant="primary" disabled={mutation.isPending || !canSubmit}>
            {mutation.isPending ? 'Salvando...' : 'Criar pedido'}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
