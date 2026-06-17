import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useToast } from '@/context/ToastContext'
import { Plus, Pencil, Trash2, ToggleLeft, ToggleRight, Search } from 'lucide-react'
import { listUsers, createUser, updateUser, deleteUser, activateUser, deactivateUser } from '@/api/users'
import { changePin } from '@/api/auth'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Table, TableHead, TableBody, Th, Td, Tr } from '@/components/ui/table'
import { CardList, MobileCard, CardActions, IconAction, CardEmpty } from '@/components/ui/mobile-card'
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
interface PinFormState { currentPin: string; newPin: string; confirmPin: string }
const emptyPinForm: PinFormState = { currentPin: '', newPin: '', confirmPin: '' }
const PAGE_SIZE = 20

export function UsersPage() {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<User | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [pinForm, setPinForm] = useState<PinFormState>(emptyPinForm)
  const [pinMismatch, setPinMismatch] = useState(false)
  const withPin = useWithPin()
  const confirm = useConfirm()
  const toast = useToast()
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
    onSuccess: () => { toast.success(editing ? 'Alterações salvas' : 'Usuário criado'); closeDialog(); invalidate() },
  })

  const deleteMutation = useMutation({ mutationFn: deleteUser, onSuccess: () => { toast.success('Usuário excluído'); invalidate() } })
  const toggleMutation = useMutation({
    mutationFn: (u: User) => (u.active ? deactivateUser(u.id) : activateUser(u.id)),
    onSuccess: invalidate,
  })

  const changePinMutation = useMutation({
    mutationFn: () => changePin(pinForm.currentPin, pinForm.newPin),
    onSuccess: () => { toast.success('Senha alterada'); setPinForm(emptyPinForm); setPinMismatch(false) },
  })

  function openCreate() { setEditing(null); setForm(emptyForm); saveMutation.reset(); setDialogOpen(true) }
  function openEdit(u: User) {
    setEditing(u); setForm({ name: u.name, pin: '', role: u.role })
    setPinForm(emptyPinForm); setPinMismatch(false)
    saveMutation.reset(); changePinMutation.reset(); setDialogOpen(true)
  }
  function closeDialog() { setDialogOpen(false); setEditing(null); setForm(emptyForm); setPinForm(emptyPinForm); setPinMismatch(false) }

  function handleChangePin(e: React.FormEvent) {
    e.preventDefault()
    if (pinForm.newPin !== pinForm.confirmPin) { setPinMismatch(true); return }
    setPinMismatch(false)
    changePinMutation.mutate()
  }

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
          <Button variant="primary" size="md" className="px-4" onClick={openCreate}>
            <Plus size={15} /> Novo usuário
          </Button>
        }
      />

      <div className="p-4 sm:p-6 space-y-4">
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
              <div className="hidden md:block">
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
              </div>

              <CardList>
                {paged.length === 0 && <CardEmpty>{query ? 'Nenhum usuário encontrado.' : 'Nenhum usuário cadastrado.'}</CardEmpty>}
                {paged.map((u) => (
                  <MobileCard key={u.id}>
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-semibold text-gray-900 break-words min-w-0 flex-1">{u.name}</span>
                      <Badge variant={u.role === 'ADMIN' ? 'purple' : 'gray'}>{u.role === 'ADMIN' ? 'Admin' : 'Vendedor'}</Badge>
                      <Badge variant={u.active ? 'green' : 'red'}>{u.active ? 'Ativo' : 'Inativo'}</Badge>
                    </div>
                    <div className="text-sm text-gray-400">Cadastrado: <span className="text-gray-700">{formatDateShort(u.createdAt)}</span></div>
                    <CardActions>
                      <IconAction label={u.active ? 'Desativar' : 'Ativar'} className={u.active ? 'text-green-600' : 'text-gray-400'} onClick={() => handleToggle(u)}>
                        {u.active ? <ToggleRight size={20} /> : <ToggleLeft size={20} />}
                      </IconAction>
                      <IconAction label="Editar" onClick={() => openEdit(u)}><Pencil size={17} /></IconAction>
                      <IconAction label="Excluir" className="text-red-500" onClick={() => handleDelete(u)}><Trash2 size={17} /></IconAction>
                    </CardActions>
                  </MobileCard>
                ))}
              </CardList>

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

        {editing && (
          <form onSubmit={handleChangePin} autoComplete="off" className="space-y-3 mt-5 pt-4 border-t border-gray-150">
            <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">Trocar senha</p>
            <div>
              <label className="text-xs font-medium text-gray-700 block mb-1">PIN atual</label>
              <Input
                type="password"
                value={pinForm.currentPin}
                onChange={(e) => setPinForm((p) => ({ ...p, currentPin: e.target.value }))}
                maxLength={4} pattern="\d{1,4}" required placeholder="••••"
                autoComplete="off"
              />
            </div>
            <div className="flex gap-3">
              <div className="flex-1">
                <label className="text-xs font-medium text-gray-700 block mb-1">Novo PIN</label>
                <Input
                  type="password"
                  value={pinForm.newPin}
                  onChange={(e) => setPinForm((p) => ({ ...p, newPin: e.target.value }))}
                  maxLength={4} pattern="\d{1,4}" required placeholder="••••"
                  autoComplete="new-password"
                />
              </div>
              <div className="flex-1">
                <label className="text-xs font-medium text-gray-700 block mb-1">Confirmar novo PIN</label>
                <Input
                  type="password"
                  value={pinForm.confirmPin}
                  onChange={(e) => setPinForm((p) => ({ ...p, confirmPin: e.target.value }))}
                  maxLength={4} pattern="\d{1,4}" required placeholder="••••"
                  autoComplete="new-password"
                />
              </div>
            </div>
            {pinMismatch && <p className="text-sm text-red-600">Os PINs não coincidem.</p>}
            {changePinMutation.isError && <ErrorMessage error={changePinMutation.error} />}
            <div className="flex justify-end pt-2">
              <Button type="submit" variant="secondary" disabled={changePinMutation.isPending}>
                {changePinMutation.isPending ? 'Alterando...' : 'Alterar senha'}
              </Button>
            </div>
          </form>
        )}
      </Dialog>
    </div>
  )
}
