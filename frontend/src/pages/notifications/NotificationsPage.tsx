import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { ExternalLink, Send, ChevronDown } from 'lucide-react'
import { listNotifications, resendNotification } from '@/api/notifications'
import { listOrders } from '@/api/orders'
import { PageHeader } from '@/components/layout/PageHeader'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { Spinner } from '@/components/ui/spinner'
import { Pagination } from '@/components/shared/Pagination'
import { Button } from '@/components/ui/button'
import { useWithPin } from '@/context/PinContext'
import { formatDate } from '@/lib/utils'

const PAGE_SIZE = 20

export function NotificationsPage() {
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [orderSearch, setOrderSearch] = useState('')
  const [dropdownOpen, setDropdownOpen] = useState(false)
  const withPin = useWithPin()

  const { data: ordersData, isLoading: ordersLoading } = useQuery({
    queryKey: ['orders-all'],
    queryFn: () => listOrders(0, 500),
  })

  // Ordena decrescente (mais recentes primeiro)
  const orders = [...(ordersData?.content ?? [])].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
  )

  // Só mostra sugestões após o usuário digitar algo
  const filteredOrders = orderSearch.trim()
    ? orders.filter(
        (o) =>
          o.customerName.toLowerCase().includes(orderSearch.toLowerCase()) ||
          formatDate(o.createdAt).includes(orderSearch),
      )
    : []

  const selectedOrder = orders.find((o) => o.id === selectedOrderId)

  const { data: notifData, isLoading: notifLoading, refetch } = useQuery({
    queryKey: ['notifications', selectedOrderId, page],
    queryFn: () => listNotifications(selectedOrderId!, page, PAGE_SIZE),
    enabled: !!selectedOrderId,
  })

  const resendMutation = useMutation({
    mutationFn: resendNotification,
    onSuccess: () => refetch(),
  })

  function handleSelectOrder(id: string) {
    setSelectedOrderId(id)
    setDropdownOpen(false)
    setOrderSearch('')
    setPage(0)
  }

  return (
    <div>
      <PageHeader
        title="Notificações"
        description="Histórico de notificações WhatsApp enviadas por encomenda"
      />

      <div className="p-6 space-y-4">
        <div className="bg-white border border-gray-200 rounded-lg p-4">
          <label className="text-xs font-medium text-gray-700 block mb-2">
            Selecionar encomenda
          </label>
          <div className="relative max-w-lg">
            <button
              type="button"
              onClick={() => setDropdownOpen((v) => !v)}
              className="w-full h-8 px-3 flex items-center justify-between text-sm border border-gray-300 rounded bg-white hover:bg-gray-50 cursor-pointer"
            >
              {selectedOrder ? (
                <span className="text-gray-900 font-medium">
                  {selectedOrder.customerName} — {formatDate(selectedOrder.createdAt)}
                </span>
              ) : (
                <span className="text-gray-400">Escolha uma encomenda...</span>
              )}
              <ChevronDown size={14} className="text-gray-400" />
            </button>

            {dropdownOpen && (
              <div className="absolute z-20 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg">
                <div className="p-2 border-b border-gray-100">
                  <input
                    autoFocus
                    value={orderSearch}
                    onChange={(e) => setOrderSearch(e.target.value)}
                    placeholder="Digite o nome do cliente para buscar..."
                    className="w-full text-sm px-2 py-1 border border-gray-200 rounded focus:outline-none focus:ring-1 focus:ring-gray-300"
                    autoComplete="off"
                  />
                </div>
                <div className="max-h-56 overflow-y-auto">
                  {ordersLoading && (
                    <div className="flex justify-center py-4"><Spinner /></div>
                  )}
                  {orderSearch.trim() && filteredOrders.length === 0 && (
                    <p className="text-sm text-gray-400 px-3 py-3">Nenhuma encomenda encontrada.</p>
                  )}
                  {filteredOrders.map((o) => (
                    <button
                      key={o.id}
                      type="button"
                      onClick={() => handleSelectOrder(o.id)}
                      className="w-full text-left px-3 py-2 text-sm hover:bg-gray-50 flex items-center justify-between"
                    >
                      <span className="font-medium text-gray-900">{o.customerName}</span>
                      <span className="text-xs text-gray-400">{formatDate(o.createdAt)}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        {selectedOrderId && (
          <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
            {notifLoading ? (
              <div className="flex justify-center py-12"><Spinner /></div>
            ) : (
              <>
                <Table>
                  <TableHead>
                    <tr>
                      <Th>Cliente</Th>
                      <Th>Telefone</Th>
                      <Th>Mensagem</Th>
                      <Th>Enviado em</Th>
                      <Th />
                    </tr>
                  </TableHead>
                  <TableBody>
                    {notifData?.content.length === 0 && (
                      <tr>
                        <Td colSpan={5} className="text-center text-gray-400 py-10">
                          Nenhuma notificação para esta encomenda.
                        </Td>
                      </tr>
                    )}
                    {notifData?.content.map((n) => (
                      <Tr key={n.id}>
                        <Td className="font-medium text-gray-900">{n.customerName}</Td>
                        <Td className="text-gray-500">{n.customerPhone}</Td>
                        <Td className="max-w-xs truncate text-gray-600" title={n.message}>
                          {n.message}
                        </Td>
                        <Td className="text-gray-500">{formatDate(n.sentAt)}</Td>
                        <Td>
                          <div className="flex items-center gap-2">
                            <a href={n.link} target="_blank" rel="noopener noreferrer"
                              className="inline-flex items-center gap-1 text-xs text-blue-600 hover:underline">
                              <ExternalLink size={12} /> Abrir
                            </a>
                            <Button variant="ghost" size="sm" className="text-gray-500"
                              onClick={() => withPin(() => resendMutation.mutate(n.id))}
                              disabled={resendMutation.isPending} title="Reenviar">
                              <Send size={12} />
                            </Button>
                          </div>
                        </Td>
                      </Tr>
                    ))}
                  </TableBody>
                </Table>
                {notifData && (
                  <Pagination page={page} totalPages={notifData.totalPages || 1}
                    totalElements={notifData.totalElements} size={PAGE_SIZE} onPageChange={setPage} />
                )}
              </>
            )}
          </div>
        )}

        {!selectedOrderId && (
          <div className="text-center text-sm text-gray-400 py-12">
            Selecione uma encomenda para ver suas notificações.
          </div>
        )}
      </div>
    </div>
  )
}
