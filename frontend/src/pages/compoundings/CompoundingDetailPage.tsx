import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Trash2, Pencil, MessageCircle } from 'lucide-react'
import {
  getCompounding, deleteCompounding,
  markCompoundingAsOrdered, markCompoundingAsReceived, markCompoundingAsDelivered,
  markCompoundingAsToPay, markCompoundingAsPaid, markCompoundingAsMakeNote, markCompoundingAsNoted,
  updateCompounding, listCompoundingNotifications,
} from '@/api/compoundings'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Spinner } from '@/components/ui/spinner'
import { Dialog } from '@/components/ui/dialog'
import { CompoundingStatusBadge, PaymentStatusBadge } from '@/components/shared/StatusBadge'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { NotificationPopup } from '@/components/shared/NotificationPopup'
import { CustomerSearch } from '@/components/shared/CustomerSearch'
import { PharmacySearch } from '@/components/shared/PharmacySearch'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { formatDate } from '@/lib/utils'
import type { Customer, CompoundingPharmacy, Notification } from '@/types'

export function CompoundingDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const withPin = useWithPin()
  const confirm = useConfirm()
  const [editOpen, setEditOpen] = useState(false)
  const [popup, setPopup] = useState<Notification | null>(null)

  const { data: compounding, isLoading, error } = useQuery({
    queryKey: ['compounding', id],
    queryFn: () => getCompounding(id!),
    enabled: !!id,
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['compounding', id] })

  async function checkForNotification() {
    const fresh = await getCompounding(id!)
    if (fresh.status === 'RECEIVED' && fresh.notifiedAt) {
      const notifs = await listCompoundingNotifications(id!, 0, 1)
      if (notifs.content.length > 0) setPopup(notifs.content[0])
    }
    qc.invalidateQueries({ queryKey: ['compounding', id] })
  }

  const deleteMutation = useMutation({ mutationFn: () => deleteCompounding(id!), onSuccess: () => navigate('/compoundings') })
  const markOrdered = useMutation({ mutationFn: () => markCompoundingAsOrdered(id!), onSuccess: invalidate })
  const markReceived = useMutation({ mutationFn: () => markCompoundingAsReceived(id!), onSuccess: checkForNotification })
  const markDelivered = useMutation({ mutationFn: () => markCompoundingAsDelivered(id!), onSuccess: invalidate })
  const markToPay = useMutation({ mutationFn: () => markCompoundingAsToPay(id!), onSuccess: invalidate })
  const markPaid = useMutation({ mutationFn: () => markCompoundingAsPaid(id!), onSuccess: invalidate })
  const markMakeNote = useMutation({ mutationFn: () => markCompoundingAsMakeNote(id!), onSuccess: invalidate })
  const markNoted = useMutation({ mutationFn: () => markCompoundingAsNoted(id!), onSuccess: invalidate })

  async function handleDelete() {
    if (!await confirm('Excluir esta manipulação? Esta ação não pode ser desfeita.')) return
    withPin(() => deleteMutation.mutate())
  }

  if (isLoading) return <div className="flex justify-center py-20"><Spinner /></div>
  if (error) return <div className="p-6"><ErrorMessage error={error} /></div>
  if (!compounding) return null

  const isDelivered = compounding.status === 'DELIVERED'
  const isPaymentFinal = compounding.paymentStatus === 'PAID' || compounding.paymentStatus === 'NOTED'

  return (
    <div>
      <PageHeader
        title={`Manipulação — ${compounding.customerName}`}
        description={`Criada por ${compounding.createdByName} em ${formatDate(compounding.createdAt)}`}
        actions={
          <div className="flex items-center gap-3 flex-wrap">
            <Button variant="ghost" size="sm" onClick={() => navigate('/compoundings')}>
              <ArrowLeft size={13} /> Voltar
            </Button>

            {/* Status transitions */}
            {!isDelivered && (
              <div className="flex items-center gap-2 border-l border-gray-200 pl-3">
                {compounding.status === 'PENDING' && (
                  <Button variant="secondary" size="sm"
                    onClick={() => withPin(() => markOrdered.mutate())}
                    disabled={markOrdered.isPending}>
                    Marcar pedido
                  </Button>
                )}
                {compounding.status === 'ORDERED' && (
                  <Button variant="secondary" size="sm"
                    onClick={() => withPin(() => markReceived.mutate())}
                    disabled={markReceived.isPending}>
                    Marcar recebido
                  </Button>
                )}
                {compounding.status === 'RECEIVED' && (
                  <Button variant="secondary" size="sm"
                    onClick={() => withPin(() => markDelivered.mutate())}
                    disabled={markDelivered.isPending}>
                    Marcar entregue
                  </Button>
                )}
                <Button variant="secondary" size="sm" onClick={() => setEditOpen(true)}>
                  <Pencil size={13} /> Editar
                </Button>
                <Button variant="danger" size="sm" onClick={handleDelete} disabled={deleteMutation.isPending}>
                  <Trash2 size={13} /> Excluir
                </Button>
              </div>
            )}

            {/* Payment transitions */}
            {!isPaymentFinal && (
              <div className="flex items-center gap-2 border-l border-gray-200 pl-3">
                {compounding.paymentStatus === 'TO_PAY' && (
                  <>
                    <Button variant="secondary" size="sm"
                      onClick={() => withPin(() => markPaid.mutate())}
                      disabled={markPaid.isPending}>
                      Marcar pago
                    </Button>
                    <Button variant="secondary" size="sm"
                      onClick={() => withPin(() => markMakeNote.mutate())}
                      disabled={markMakeNote.isPending}>
                      Fazer nota
                    </Button>
                  </>
                )}
                {compounding.paymentStatus === 'MAKE_NOTE' && (
                  <>
                    <Button variant="secondary" size="sm"
                      onClick={() => withPin(() => markToPay.mutate())}
                      disabled={markToPay.isPending}>
                      A pagar
                    </Button>
                    <Button variant="secondary" size="sm"
                      onClick={() => withPin(() => markPaid.mutate())}
                      disabled={markPaid.isPending}>
                      Marcar pago
                    </Button>
                    <Button variant="secondary" size="sm"
                      onClick={() => withPin(() => markNoted.mutate())}
                      disabled={markNoted.isPending}>
                      Marcar anotado
                    </Button>
                  </>
                )}
              </div>
            )}
          </div>
        }
      />

      <div className="p-6 space-y-4">
        <div className="bg-white border border-gray-200 rounded-lg p-4 grid grid-cols-2 gap-x-8 gap-y-4 text-sm sm:grid-cols-3 lg:grid-cols-4">
          <InfoField label="Status">
            <CompoundingStatusBadge status={compounding.status} />
          </InfoField>
          <InfoField label="Pagamento">
            <PaymentStatusBadge status={compounding.paymentStatus} />
          </InfoField>
          <InfoField label="Cliente">
            <span className="font-medium text-gray-900">{compounding.customerName}</span>
          </InfoField>
          <InfoField label="Farmácia">
            <span className="font-medium text-gray-900">{compounding.pharmacyName}</span>
            <span className="text-gray-400 text-xs ml-1">({compounding.pharmacyCity})</span>
          </InfoField>
          <InfoField label="Quantidade">
            <span className="font-medium text-gray-900">{compounding.quantity}</span>
          </InfoField>
          {compounding.value != null && (
            <InfoField label="Valor">
              <span className="font-medium text-gray-900">
                {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(compounding.value)}
              </span>
            </InfoField>
          )}
          {compounding.observations && (
            <InfoField label="Observações" wide>
              <span className="text-gray-700">{compounding.observations}</span>
            </InfoField>
          )}
          {compounding.notifiedAt && (
            <InfoField label="Notificado em">
              <div className="flex items-center gap-1 text-green-600">
                <MessageCircle size={13} /><span>{formatDate(compounding.notifiedAt)}</span>
              </div>
            </InfoField>
          )}
        </div>

        <div className="bg-white border border-gray-200 rounded-lg p-4 space-y-2 text-sm">
          <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-3">Histórico</p>
          <HistoryRow label="Criado" by={compounding.createdByName} at={compounding.createdAt} />
          {compounding.orderedAt && <HistoryRow label="Pedido" by={compounding.orderedByName} at={compounding.orderedAt} />}
          {compounding.receivedAt && <HistoryRow label="Recebido" by={compounding.receivedByName} at={compounding.receivedAt} />}
          {compounding.deliveredAt && <HistoryRow label="Entregue" by={compounding.deliveredByName} at={compounding.deliveredAt} />}
        </div>
      </div>

      <EditCompoundingDialog
        open={editOpen}
        onClose={() => setEditOpen(false)}
        compounding={{ ...compounding }}
        onSuccess={() => { setEditOpen(false); invalidate() }}
      />

      {popup && <NotificationPopup notification={popup} onClose={() => setPopup(null)} />}
    </div>
  )
}

function InfoField({ label, children, wide }: { label: string; children: React.ReactNode; wide?: boolean }) {
  return (
    <div className={wide ? 'col-span-2' : ''}>
      <span className="text-gray-500 text-xs uppercase tracking-wide">{label}</span>
      <div className="mt-1">{children}</div>
    </div>
  )
}

function HistoryRow({ label, by, at }: { label: string; by: string | null | undefined; at: string | null | undefined }) {
  if (!at) return null
  return (
    <div className="flex items-center gap-2 text-sm">
      <span className="w-20 text-gray-400 shrink-0">{label}</span>
      <span className="text-gray-700">{by ?? '—'}</span>
      <span className="text-gray-400 text-xs">{formatDate(at)}</span>
    </div>
  )
}

interface EditProps {
  open: boolean
  onClose: () => void
  onSuccess: () => void
  compounding: {
    id: string
    customerId: string
    customerName: string
    pharmacyId: string
    pharmacyName: string
    pharmacyCity: string
    quantity: number
    value: number | null
    observations: string | null
  }
}

function EditCompoundingDialog({ open, onClose, onSuccess, compounding }: EditProps) {
  const [customer, setCustomer] = useState<Customer | null>({ id: compounding.customerId, name: compounding.customerName } as Customer)
  const [pharmacy, setPharmacy] = useState<CompoundingPharmacy | null>({
    id: compounding.pharmacyId,
    name: compounding.pharmacyName,
    city: compounding.pharmacyCity,
    createdAt: '',
  })
  const [quantity, setQuantity] = useState(String(compounding.quantity))
  const [value, setValue] = useState(compounding.value != null ? String(compounding.value) : '')
  const [observations, setObservations] = useState(compounding.observations ?? '')
  const withPin = useWithPin()

  const updateMutation = useMutation({
    mutationFn: () => updateCompounding(compounding.id, {
      customerId: customer!.id,
      pharmacyId: pharmacy!.id,
      quantity: Number(quantity),
      value: value ? Number(value) : null,
      observations: observations.trim() || null,
    }),
    onSuccess,
  })

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()} title="Editar manipulação">
      <form onSubmit={(e) => { e.preventDefault(); withPin(() => updateMutation.mutate()) }} className="space-y-3">
        <div>
          <label className="text-xs font-medium text-gray-700 block mb-1">Cliente</label>
          <CustomerSearch value={customer} onChange={setCustomer} />
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
        {updateMutation.isError && <ErrorMessage error={updateMutation.error} />}
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" variant="primary" disabled={!customer || !pharmacy || !quantity || updateMutation.isPending}>
            {updateMutation.isPending ? 'Salvando...' : 'Salvar'}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
