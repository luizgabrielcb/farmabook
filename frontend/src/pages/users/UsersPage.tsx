import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, ToggleLeft, ToggleRight, Search } from 'lucide-react'
import { listUsers, createUser, updateUser, deleteUser, activateUser, deactivateUser } from '@/api/users'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { Spinner } from '@/components/ui/spinner'
import { Pagination } from '@/components/shared/Pagination'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { Dialog } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { useWithPin } from '@/context/PinContext'
import { useConfirm } from '@/context/ConfirmContext'
import { formatDateShort } from '@/lib/utils'
import type { User, UserRole } from '@/types'

interface FormState { name: string; pin: string; role: UserRole }
const emptyForm: FormState = { name: '', pin: '', role: 'SELLER' }
const PAGE_SIZE = 20

export function UsersPage() {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<User | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const withPin = useWithPin()
  const confirm = useConfirm()
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['users-all'],
    queryFn: () => listUsers(0, 500),
  })

  const filtered = useMemo(() => {
    const items = data?.content ?? []
    if (!query.trim()) return items
    return items.filter((u) => u.name.toLowerCase().includes(query.toLowerCase()))
  }, [data, query])

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paged = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  const invalidate = () => qc.invalidateQueries({ queryKey: ['users-all'] })

  const saveMutation = useMutation({
    mutationFn: () =>
      editing
        ? updateUser(editing.id, { name: form.name, role: form.role })
        : createUser({ name: form.name, pin: form.pin, role: form.role }),
    onSuccess: () => { closeDialog(); invalidate() },
  })

  const deleteMutation = useMutation({ mutationFn: deleteUser, onSuccess: invalidate })
  const toggleMutation = useMutation({
    mutationFn: (u: User) => (u.active ? deactivateUser(u.id) : activateUser(u.id)),
    onSuccess: invalidate,
  })

  function openCreate() { setEditing(null); setForm(emptyForm); saveMutation.reset(); setDialogOpen(true) }
  function openEdit(u: User) {
    setEditing(u); setForm({ name: u.name, pin: '', role: u.role })
    saveMutation.reset(); setDialogOpen(true)
  }
  function closeDialog() { setDialogOpen(false); setEditing(null); setForm(emptyForm) }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (editing) {
      withPin(() => saveMutation.mutate())
    } else {
      saveMutation.mutate()
    }
  }

  async function handleDelete(u: User) {
    if (!await confirm(`Excluir o usuário "${u.name}"?`)) return
    withPin(() => deleteMutation.mutate(u.id))
  }

  async function handleToggle(u: User) {
    const msg = u.active ? `Desativar "${u.name}"?` : `Ativar "${u.name}"?`
    if (!await confirm(msg)) return
    withPin(() => toggleMutation.mutate(u))
  }

  return (
    <div>
      <PageHeader
        title="Usuários"
        description="Gerencie os usuários do sistema"
        actions={
          <Button variant="primary" size="sm" onClick={openCreate}>
            <Plus size={13} /> Novo usuário
          </Button>
        }
      />

      <div className="p-6 space-y-4">
        <div className="bg-white border border-gray-200 rounded-lg p-3">
          <div className="relative max-w-sm">
            <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
            <Input value={query} onChange={(e) => { setQuery(e.target.value); setPage(0) }}
              placeholder="Buscar por nome..." className="pl-8" />
          </div>
        </div>

        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          {isLoading ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : (
            <>
              <Table>
                <TableHead>
                  <tr><Th>Nome</Th><Th>Perfil</Th><Th>Status</Th><Th>Cadastrado em</Th><Th /></tr>
                </TableHead>
                <TableBody>
                  {paged.length === 0 && (
                    <tr>
                      <Td colSpan={5} className="text-center text-gray-400 py-10">
                        {query ? 'Nenhum usuário encontrado.' : 'Nenhum usuário cadastrado.'}
                      </Td>
                    </tr>
                  )}
                  {paged.map((u) => (
                    <Tr key={u.id}>
                      <Td className="font-medium text-gray-900">{u.name}</Td>
                      <Td>
                        <Badge variant={u.role === 'ADMIN' ? 'purple' : 'gray'}>
                          {u.role === 'ADMIN' ? 'Admin' : 'Vendedor'}
                        </Badge>
                      </Td>
                      <Td>
                        <Badge variant={u.active ? 'green' : 'red'}>
                          {u.active ? 'Ativo' : 'Inativo'}
                        </Badge>
                      </Td>
                      <Td className="text-gray-500">{formatDateShort(u.createdAt)}</Td>
                      <Td>
                        <div className="flex items-center gap-1">
                          <Button variant="ghost" size="sm"
                            className={u.active ? 'text-green-600' : 'text-gray-400'}
                            onClick={() => handleToggle(u)}
                            title={u.active ? 'Desativar' : 'Ativar'}>
                            {u.active ? <ToggleRight size={14} /> : <ToggleLeft size={14} />}
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => openEdit(u)}>
                            <Pencil size={12} />
                          </Button>
                          <Button variant="ghost" size="sm" className="text-red-400 hover:text-red-600"
                            onClick={() => handleDelete(u)}>
                            <Trash2 size={12} />
                          </Button>
                        </div>
                      </Td>
                    </Tr>
                  ))}
                </TableBody>
              </Table>
              <Pagination page={page} totalPages={totalPages || 1}
                totalElements={filtered.length} size={PAGE_SIZE} onPageChange={setPage} />
            </>
          )}
        </div>
      </div>

      <Dialog open={dialogOpen} onOpenChange={(v) => !v && closeDialog()}
        title={editing ? 'Editar usuário' : 'Novo usuário'}>
        <form onSubmit={handleSubmit} autoComplete="off" className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Nome</label>
            <Input value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
              maxLength={100} required autoFocus autoComplete="off" />
          </div>
          {!editing && (
            <div>
              <label className="text-xs font-medium text-gray-700 block mb-1">PIN (1–4 dígitos)</label>
              <Input
                type="password"
                value={form.pin}
                onChange={(e) => setForm((p) => ({ ...p, pin: e.target.value }))}
                maxLength={4} pattern="\d{1,4}" required placeholder="••••"
                autoComplete="new-password"
              />
            </div>
          )}
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Perfil</label>
            <Select value={form.role} onChange={(e) => setForm((p) => ({ ...p, role: e.target.value as UserRole }))}>
              <option value="SELLER">Vendedor</option>
              <option value="ADMIN">Admin</option>
            </Select>
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
