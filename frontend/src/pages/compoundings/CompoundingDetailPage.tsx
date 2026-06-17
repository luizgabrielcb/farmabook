import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Trash2, MessageCircle, Settings2 } from 'lucide-react'
import {
  getCompounding, deleteCompounding,
  markCompoundingAsOrdered, markCompoundingAsReceived, markCompoundingAsDelivered,
  markCompoundingAsToPay, markCompoundingAsPaid, markCompoundingAsMakeNote, markCompoundingAsNoted,
  updateCompounding, listCompoundingNotifications,
} from '@/api/compoundings'
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
import { useToast } from '@/context/ToastContext'
import { formatDate } from '@/lib/utils'
import type { Customer, CompoundingPharmacy, Notification } from '@/types'

type DetailTab = 'detalhes' | 'pagamento' | 'historico'

const currencyFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function CompoundingDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const withPin = useWithPin()
  const confirm = useConfirm()
  const toast = useToast()
  const [tab, setTab] = useState<DetailTab>('detalhes')
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

  async function handleNotify() {
    const notifs = await listCompoundingNotifications(id!, 0, 1)
    if (notifs.content.length > 0) setPopup(notifs.content[0])
  }

  const deleteMutation = useMutation({ mutationFn: () => deleteCompounding(id!), onSuccess: () => { toast.success('Manipulação excluída'); navigate('/compoundings') } })
  const markOrdered = useMutation({ mutationFn: () => markCompoundingAsOrdered(id!), onSuccess: () => { toast.success('Status atualizado'); invalidate() } })
  const markReceived = useMutation({ mutationFn: () => markCompoundingAsReceived(id!), onSuccess: () => { toast.success('Status atualizado'); checkForNotification() } })
  const markDelivered = useMutation({ mutationFn: () => markCompoundingAsDelivered(id!), onSuccess: () => { toast.success('Manipulação entregue'); invalidate() } })

  async function handleMarkDelivered() {
    if (!await confirm('Marcar esta manipulação como entregue? Esta ação não pode ser desfeita.')) return
    withPin(() => markDelivered.mutate())
  }
  const markToPay = useMutation({ mutationFn: () => markCompoundingAsToPay(id!), onSuccess: () => { toast.success('Pagamento atualizado'); invalidate() } })
  const markPaid = useMutation({ mutationFn: () => markCompoundingAsPaid(id!), onSuccess: () => { toast.success('Pagamento atualizado'); invalidate() } })
  const markMakeNote = useMutation({ mutationFn: () => markCompoundingAsMakeNote(id!), onSuccess: () => { toast.success('Pagamento atualizado'); invalidate() } })
  const markNoted = useMutation({ mutationFn: () => markCompoundingAsNoted(id!), onSuccess: () => { toast.success('Pagamento atualizado'); invalidate() } })

  async function handleDelete() {
    if (!await confirm('Excluir esta manipulação? Esta ação não pode ser desfeita.')) return
    withPin(() => deleteMutation.mutate())
  }

  if (isLoading) return <div className="flex justify-center py-20"><Spinner /></div>
  if (error) return <div className="p-6"><ErrorMessage error={error} /></div>
  if (!compounding) return null

  const isDelivered = compounding.status === 'DELIVERED'
  const isPaymentFinal = compounding.paymentStatus === 'PAID' || compounding.paymentStatus === 'NOTED'

  const tabs: { value: DetailTab; label: string }[] = [
    { value: 'detalhes', label: 'Detalhes' },
    { value: 'pagamento', label: 'Pagamento' },
    { value: 'historico', label: 'Histórico' },
  ]

  return (
    <div className="p-4 sm:p-6">
      <button
        onClick={() => navigate('/compoundings')}
        className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 mb-4 cursor-pointer transition-colors"
      >
        <ArrowLeft size={15} /> Manipulações
      </button>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_300px] gap-4 items-start">
        {/* MAIN CARD */}
        <div className="bg-white border border-gray-150 rounded-2xl shadow-sm overflow-hidden">
          {/* header */}
          <div className="p-5 border-b border-gray-150">
            <div className="flex items-start gap-3">
              <p className="flex-1 min-w-0 font-bold text-gray-900 text-[17px]">{compounding.customerName}</p>
              <CompoundingStatusBadge status={compounding.status} />
            </div>
            <p className="text-xs text-gray-400 mt-1">
              Criada por {compounding.createdByName} em {formatDate(compounding.createdAt)}
            </p>
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
              </button>
            ))}
          </div>

          {/* DETALHES TAB */}
          {tab === 'detalhes' && (
            <div className="p-5 grid grid-cols-2 gap-x-8 gap-y-4 text-sm sm:grid-cols-3">
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
              {compounding.observations && (
                <InfoField label="Observações" wide>
                  <span className="text-gray-700 break-words">{compounding.observations}</span>
                </InfoField>
              )}
              {compounding.notifiedAt && (
                <InfoField label="Notificado em">
                  <div className="flex items-center gap-1 text-[#1da851]">
                    <MessageCircle size={13} /><span>{formatDate(compounding.notifiedAt)}</span>
                  </div>
                </InfoField>
              )}
            </div>
          )}

          {/* PAGAMENTO TAB */}
          {tab === 'pagamento' && (
            <div>
              <div className="flex items-center justify-between px-5 py-4 border-b border-gray-150">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm text-gray-500">Status de pagamento:</span>
                  <PaymentStatusBadge status={compounding.paymentStatus} />
                  {compounding.paymentChangedByName && (
                    <span className="text-[11px] text-gray-400">
                      por {compounding.paymentChangedByName} · {formatDate(compounding.paymentChangedAt)}
                    </span>
                  )}
                </div>
                {compounding.value != null && (
                  <div className="text-right">
                    <span className="block text-xs font-semibold uppercase tracking-wide text-gray-400">Valor total</span>
                    <span className="font-mono font-bold text-lg text-gray-900">{currencyFmt.format(compounding.value)}</span>
                  </div>
                )}
              </div>
              {!isPaymentFinal && (
                <div className="px-5 py-4 flex flex-wrap gap-2">
                  {compounding.paymentStatus === 'TO_PAY' && (
                    <>
                      <Button variant="secondary" size="sm"
                        onClick={() => withPin(() => markPaid.mutate())} disabled={markPaid.isPending}>
                        Marcar pago
                      </Button>
                      <Button variant="secondary" size="sm"
                        onClick={() => withPin(() => markMakeNote.mutate())} disabled={markMakeNote.isPending}>
                        Fazer nota
                      </Button>
                    </>
                  )}
                  {compounding.paymentStatus === 'MAKE_NOTE' && (
                    <>
                      <Button variant="secondary" size="sm"
                        onClick={() => withPin(() => markToPay.mutate())} disabled={markToPay.isPending}>
                        A pagar
                      </Button>
                      <Button variant="secondary" size="sm"
                        onClick={() => withPin(() => markPaid.mutate())} disabled={markPaid.isPending}>
                        Marcar pago
                      </Button>
                      <Button variant="secondary" size="sm"
                        onClick={() => withPin(() => markNoted.mutate())} disabled={markNoted.isPending}>
                        Marcar anotado
                      </Button>
                    </>
                  )}
                </div>
              )}
              {isPaymentFinal && (
                <div className="px-5 py-6 text-sm text-gray-500 text-center">Pagamento finalizado.</div>
              )}
            </div>
          )}

          {/* HISTÓRICO TAB */}
          {tab === 'historico' && (
            <div className="p-5 space-y-2 text-sm">
              <HistoryRow label="Criado" by={compounding.createdByName} at={compounding.createdAt} />
              {compounding.orderedAt && <HistoryRow label="Pedido" by={compounding.orderedByName} at={compounding.orderedAt} />}
              {compounding.receivedAt && <HistoryRow label="Recebido" by={compounding.receivedByName} at={compounding.receivedAt} />}
              {compounding.deliveredAt && <HistoryRow label="Entregue" by={compounding.deliveredByName} at={compounding.deliveredAt} />}
            </div>
          )}
        </div>

        {/* SIDEBAR — AÇÕES RÁPIDAS */}
        <div className="bg-white border border-gray-150 rounded-2xl shadow-sm p-4">
          <p className="text-sm font-semibold text-gray-900 mb-3">Ações rápidas</p>
          <div className="flex flex-col gap-2">
            {!isDelivered ? (
              <>
                <Button variant="secondary" size="md" className="w-full"
                  disabled={compounding.status !== 'PENDING' || markOrdered.isPending}
                  onClick={() => withPin(() => markOrdered.mutate())}>
                  Marcar pedido
                </Button>
                <Button variant="secondary" size="md" className="w-full"
                  disabled={compounding.status !== 'ORDERED' || markReceived.isPending}
                  onClick={() => withPin(() => markReceived.mutate())}>
                  Marcar recebido
                </Button>
                <Button variant="secondary" size="md" className="w-full"
                  disabled={compounding.status !== 'RECEIVED' || markDelivered.isPending}
                  onClick={handleMarkDelivered}>
                  Marcar entregue
                </Button>
              </>
            ) : (
              <div className="text-center text-sm font-medium text-brand-700 bg-brand-50 rounded-md py-2">Manipulação concluída</div>
            )}

            <div className="h-px bg-gray-100 my-1" />

            <Button variant="whatsapp" size="md" className="w-full" disabled={!compounding.notifiedAt} onClick={handleNotify}>
              <MessageCircle size={14} /> Notificar cliente
            </Button>

            {!isDelivered && (
              <>
                <Button variant="secondary" size="md" className="w-full" onClick={() => setEditOpen(true)}>
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

      <EditCompoundingDialog
        open={editOpen}
        onClose={() => setEditOpen(false)}
        compounding={{ ...compounding }}
        onSuccess={() => { toast.success('Alterações salvas'); setEditOpen(false); invalidate() }}
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
    <div className="flex items-center gap-3">
      <span className="w-24 text-gray-500 shrink-0">{label}</span>
      <span className="text-gray-900 font-medium">{by ?? '—'}</span>
      <span className="text-gray-500 text-[13px]">{formatDate(at)}</span>
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
          <Input type="number" min={1} max={999} step={1} value={quantity}
            onKeyDown={(e) => ['e', 'E', '+', '-', '.', ','].includes(e.key) && e.preventDefault()}
            onChange={(e) => {
              const val = e.target.value.replace(/[^0-9]/g, '')
              if (val && parseInt(val, 10) > 999) return
              setQuantity(val)
            }} required autoComplete="off" />
        </div>
        <div>
          <label className="text-xs font-medium text-gray-700 block mb-1">
            Valor total <span className="text-gray-400">(opcional)</span>
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
