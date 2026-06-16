import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useToast } from '@/context/ToastContext'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { listDistributors, createDistributor, updateDistributor, deleteDistributor } from '@/api/distributors'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { Spinner } from '@/components/ui/spinner'
import { Dialog } from '@/components/ui/dialog'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { formatDateShort } from '@/lib/utils'
import type { Distributor } from '@/types'

export function DistributorsPage() {
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<Distributor | null>(null)
  const [form, setForm] = useState({ name: '' })
  const withPin = useWithPin()
  const confirm = useConfirm()
  const toast = useToast()
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['distributors'],
    queryFn: () => listDistributors(),
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['distributors'] })

  const saveMutation = useMutation({
    mutationFn: () => editing ? updateDistributor(editing.id, form) : createDistributor(form),
    onSuccess: () => { toast.success(editing ? 'Alterações salvas' : 'Distribuidora cadastrada'); closeDialog(); invalidate() },
  })

  const deleteMutation = useMutation({ mutationFn: deleteDistributor, onSuccess: () => { toast.success('Distribuidora excluída'); invalidate() } })

  function openCreate() {
    setEditing(null); setForm({ name: '' }); saveMutation.reset(); setDialogOpen(true)
  }

  function openEdit(d: Distributor) {
    setEditing(d); setForm({ name: d.name }); saveMutation.reset(); setDialogOpen(true)
  }

  function closeDialog() {
    setDialogOpen(false); setEditing(null); setForm({ name: '' })
  }

  async function handleDelete(d: Distributor) {
    if (!await confirm(`Excluir a distribuidora "${d.name}"?`)) return
    withPin(() => deleteMutation.mutate(d.id))
  }

  return (
    <div>
      <PageHeader title="Distribuidoras" description="Gerencie as distribuidoras cadastradas" />

      <div className="p-6 space-y-4">
        <div className="flex justify-end">
          <Button variant="primary" size="md" className="px-4" onClick={openCreate}>
            <Plus size={15} /> Nova distribuidora
          </Button>
        </div>

        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          {isLoading ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : (
            <Table>
              <TableHead>
                <tr>
                  <Th>Nome</Th>
                  <Th>Cadastrada em</Th>
                  <Th />
                </tr>
              </TableHead>
              <TableBody>
                {(data?.content ?? []).length === 0 && (
                  <tr>
                    <Td colSpan={3} className="text-center text-gray-400 py-10">
                      Nenhuma distribuidora cadastrada.
                    </Td>
                  </tr>
                )}
                {(data?.content ?? []).map((d) => (
                  <Tr key={d.id}>
                    <Td>
                      <span className="font-medium text-gray-900 block max-w-[200px] break-words whitespace-normal" title={d.name}>
                        {d.name}
                      </span>
                    </Td>
                    <Td className="text-gray-500">{formatDateShort(d.createdAt)}</Td>
                    <Td>
                      <div className="flex items-center gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(d)}>
                          <Pencil size={12} />
                        </Button>
                        <Button variant="ghost" size="sm" className="text-red-400 hover:text-red-600"
                          onClick={() => handleDelete(d)}>
                          <Trash2 size={12} />
                        </Button>
                      </div>
                    </Td>
                  </Tr>
                ))}
              </TableBody>
            </Table>
          )}
        </div>
      </div>

      <Dialog open={dialogOpen} onOpenChange={(v) => !v && closeDialog()}
        title={editing ? 'Editar distribuidora' : 'Nova distribuidora'}>
        <form onSubmit={(e) => { e.preventDefault(); withPin(() => saveMutation.mutate()) }}
          className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Nome</label>
            <Input value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
              maxLength={100} required autoFocus autoComplete="off" />
          </div>
          {saveMutation.isError && <ErrorMessage error={saveMutation.error} />}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={closeDialog}>Cancelar</Button>
            <Button type="submit" variant="primary" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Salvando...' : 'Salvar'}
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  )
}
