import { useState, useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Search, X, Plus } from 'lucide-react'
import { listPrescriptions } from '@/api/prescriptions'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { Spinner } from '@/components/ui/spinner'
import { Pagination } from '@/components/shared/Pagination'
import { PrescriptionStatusBadge } from '@/components/shared/StatusBadge'
import { CreatePrescriptionDialog } from './CreatePrescriptionDialog'
import { formatDateShort, parseLocalDate } from '@/lib/utils'
import type { PrescriptionStatus } from '@/types'

const STATUS_OPTIONS: { value: PrescriptionStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'Todos' },
  { value: 'PENDING', label: 'Pendente' },
  { value: 'FINISHED', label: 'Finalizado' },
]

const PAGE_SIZE = 20

export function PrescriptionsPage() {
  return (
    <div>
      <PageHeader title="Receitas" description="Controle de pendências de receitas de clientes" />
      <div className="p-6">
        <PrescriptionsList />
      </div>
    </div>
  )
}

function PrescriptionsList() {
  const navigate = useNavigate()
  const [createOpen, setCreateOpen] = useState(false)
  const [statusFilter, setStatusFilter] = useState<PrescriptionStatus | 'ALL'>('ALL')
  const [customerQuery, setCustomerQuery] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['prescriptions-all'],
    queryFn: () => listPrescriptions(0, 500),
  })

  const filtered = useMemo(() => {
    let items = data?.content ?? []
    if (statusFilter !== 'ALL') items = items.filter((p) => p.status === statusFilter)
    if (customerQuery.trim())
      items = items.filter((p) =>
        p.customerName.toLowerCase().includes(customerQuery.toLowerCase()),
      )
    if (dateFrom) {
      const from = parseLocalDate(dateFrom)
      items = items.filter((p) => new Date(p.createdAt) >= from)
    }
    if (dateTo) {
      const to = parseLocalDate(dateTo)
      to.setHours(23, 59, 59, 999)
      items = items.filter((p) => new Date(p.createdAt) <= to)
    }
    return items
  }, [data, statusFilter, customerQuery, dateFrom, dateTo])

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paged = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)
  const hasFilter = statusFilter !== 'ALL' || customerQuery || dateFrom || dateTo

  function handleCreated(id: string) {
    setCreateOpen(false)
    navigate(`/prescriptions/${id}`)
  }

  return (
    <div>
      <div className="flex justify-end mb-4">
        <Button variant="primary" size="sm" onClick={() => setCreateOpen(true)}>
          <Plus size={13} /> Nova pendência
        </Button>
      </div>

      <div className="space-y-4">
        <div className="bg-white border border-gray-200 rounded-lg p-4 space-y-3">
          <div className="flex items-center gap-2 flex-wrap">
            {STATUS_OPTIONS.map((s) => (
              <button
                key={s.value}
                onClick={() => {
                  setStatusFilter(s.value)
                  setPage(0)
                }}
                className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors cursor-pointer ${
                  statusFilter === s.value
                    ? 'bg-gray-800 text-white border-gray-800'
                    : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'
                }`}
              >
                {s.label}
              </button>
            ))}
          </div>
          <div className="flex gap-3 flex-wrap">
            <div className="relative flex-1 min-w-48">
              <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
              <Input
                value={customerQuery}
                onChange={(e) => {
                  setCustomerQuery(e.target.value)
                  setPage(0)
                }}
                placeholder="Buscar cliente..."
                className="pl-8"
                autoComplete="off"
              />
            </div>
            <div className="flex items-center gap-2">
              <Input
                type="date"
                value={dateFrom}
                onChange={(e) => {
                  setDateFrom(e.target.value)
                  setPage(0)
                }}
                className="w-36"
              />
              <span className="text-gray-400 text-xs">até</span>
              <Input
                type="date"
                value={dateTo}
                onChange={(e) => {
                  setDateTo(e.target.value)
                  setPage(0)
                }}
                className="w-36"
              />
            </div>
            {hasFilter && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setStatusFilter('ALL')
                  setCustomerQuery('')
                  setDateFrom('')
                  setDateTo('')
                  setPage(0)
                }}
              >
                <X size={12} /> Limpar
              </Button>
            )}
          </div>
        </div>

        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          {isLoading ? (
            <div className="flex justify-center py-12">
              <Spinner />
            </div>
          ) : (
            <>
              <Table>
                <TableHead>
                  <tr>
                    <Th>Cliente</Th>
                    <Th>Status</Th>
                    <Th>Medicamentos</Th>
                    <Th>Criado por</Th>
                    <Th>Data</Th>
                  </tr>
                </TableHead>
                <TableBody>
                  {paged.length === 0 && (
                    <tr>
                      <Td colSpan={5} className="text-center text-gray-400 py-10">
                        {hasFilter
                          ? 'Nenhuma pendência encontrada com esses filtros.'
                          : 'Nenhuma pendência de receita registrada.'}
                      </Td>
                    </tr>
                  )}
                  {paged.map((p) => (
                    <Tr
                      key={p.id}
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => navigate(`/prescriptions/${p.id}`)}
                    >
                      <Td>
                        <span
                          className="font-medium text-gray-900 block max-w-[180px] truncate"
                          title={p.customerName}
                        >
                          {p.customerName}
                        </span>
                      </Td>
                      <Td>
                        <PrescriptionStatusBadge status={p.status} />
                      </Td>
                      <Td className="text-gray-500">{p.items?.length ?? 0} medicamento(s)</Td>
                      <Td className="text-gray-500">{p.createdByName}</Td>
                      <Td className="text-gray-500">{formatDateShort(p.createdAt)}</Td>
                    </Tr>
                  ))}
                </TableBody>
              </Table>
              <Pagination
                page={page}
                totalPages={totalPages || 1}
                totalElements={filtered.length}
                size={PAGE_SIZE}
                onPageChange={setPage}
              />
            </>
          )}
        </div>
      </div>

      <CreatePrescriptionDialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSuccess={handleCreated}
      />
    </div>
  )
}
