import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import {
  listCompoundingPharmacies,
  createCompoundingPharmacy,
  updateCompoundingPharmacy,
  deleteCompoundingPharmacy,
} from '@/api/compounding-pharmacies'
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
import type { CompoundingPharmacy } from '@/types'

interface FormState { name: string; city: string }
const emptyForm: FormState = { name: '', city: '' }

export function CompoundingPharmaciesPage() {
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<CompoundingPharmacy | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const withPin = useWithPin()
  const confirm = useConfirm()
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['compounding-pharmacies'],
    queryFn: () => listCompoundingPharmacies(),
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['compounding-pharmacies'] })

  const saveMutation = useMutation({
    mutationFn: () =>
      editing
        ? updateCompoundingPharmacy(editing.id, form)
        : createCompoundingPharmacy(form),
    onSuccess: () => { closeDialog(); invalidate() },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteCompoundingPharmacy,
    onSuccess: invalidate,
  })

  function openCreate() { setEditing(null); setForm(emptyForm); saveMutation.reset(); setDialogOpen(true) }
  function openEdit(p: CompoundingPharmacy) {
    setEditing(p)
    setForm({ name: p.name, city: p.city })
    saveMutation.reset(); setDialogOpen(true)
  }
  function closeDialog() { setDialogOpen(false); setEditing(null); setForm(emptyForm) }

  async function handleDelete(p: CompoundingPharmacy) {
    if (!await confirm(`Excluir a farmácia "${p.name}"?`)) return
    withPin(() => deleteMutation.mutate(p.id))
  }

  return (
    <div>
      <PageHeader
        title="Farmácias de Manipulação"
        description="Farmácias fornecedoras de manipulações"
        actions={
          <Button variant="primary" size="sm" onClick={openCreate}>
            <Plus size={13} /> Nova farmácia
          </Button>
        }
      />

      <div className="p-6">
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          {isLoading ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : (
            <Table>
              <TableHead>
                <tr>
                  <Th>Nome</Th><Th>Cidade</Th><Th>Cadastrada em</Th><Th />
                </tr>
              </TableHead>
              <TableBody>
                {(data?.content ?? []).length === 0 && (
                  <tr>
                    <Td colSpan={4} className="text-center text-gray-400 py-10">
                      Nenhuma farmácia cadastrada.
                    </Td>
                  </tr>
                )}
                {(data?.content ?? []).map((p) => (
                  <Tr key={p.id}>
                    <Td className="font-medium text-gray-900">{p.name}</Td>
                    <Td className="text-gray-500">{p.city}</Td>
                    <Td className="text-gray-500">{formatDateShort(p.createdAt)}</Td>
                    <Td>
                      <div className="flex items-center gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(p)} title="Editar">
                          <Pencil size={12} />
                        </Button>
                        <Button variant="ghost" size="sm"
                          className="text-red-400 hover:text-red-600"
                          onClick={() => handleDelete(p)}
                          title="Excluir">
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
        title={editing ? 'Editar farmácia' : 'Nova farmácia de manipulação'}>
        <form onSubmit={(e) => { e.preventDefault(); withPin(() => saveMutation.mutate()) }} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Nome</label>
            <Input value={form.name}
              onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
              maxLength={150} required autoFocus autoComplete="off" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Cidade</label>
            <Input value={form.city}
              onChange={(e) => setForm((p) => ({ ...p, city: e.target.value }))}
              maxLength={100} required autoComplete="off" />
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
