import { useState, useRef, useEffect } from 'react'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import { Search, Plus } from 'lucide-react'
import { listDistributors, createDistributor } from '@/api/distributors'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { useWithPin } from '@/context/PinContext'
import type { Distributor } from '@/types'

interface Props {
  value: Distributor | null
  onChange: (d: Distributor | null) => void
}

export function DistributorSearch({ value, onChange }: Props) {
  const [query, setQuery] = useState(value?.name ?? '')
  const [open, setOpen] = useState(false)
  const [quickAddOpen, setQuickAddOpen] = useState(false)
  const [newName, setNewName] = useState('')
  const containerRef = useRef<HTMLDivElement>(null)
  const withPin = useWithPin()
  const qc = useQueryClient()

  useEffect(() => {
    setQuery(value?.name ?? '')
  }, [value])

  const { data } = useQuery({
    queryKey: ['distributors'],
    queryFn: () => listDistributors(),
  })

  const filtered = (data?.content ?? []).filter((d) =>
    d.name.toLowerCase().includes(query.toLowerCase()),
  )

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
        setQuery(value?.name ?? '')
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [value])

  const createMutation = useMutation({
    mutationFn: () => createDistributor({ name: newName }),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ['distributors'] })
      onChange(created)
      setQuickAddOpen(false)
      setNewName('')
    },
  })

  function handleSelect(d: Distributor) {
    onChange(d)
    setOpen(false)
  }

  function handleClear() {
    onChange(null)
    setQuery('')
    setOpen(false)
  }

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    setQuery(e.target.value)
    setOpen(true)
    if (!e.target.value) onChange(null)
  }

  function openQuickAdd() {
    setNewName(query)
    setOpen(false)
    setQuickAddOpen(true)
  }

  return (
    <>
      <div ref={containerRef} className="relative">
        <div className="relative">
          <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
          <Input
            value={query}
            onChange={handleInputChange}
            onFocus={() => setOpen(true)}
            placeholder="Buscar distribuidora..."
            className="pl-8 pr-8"
          />
          {value && (
            <button
              type="button"
              onClick={handleClear}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-xs cursor-pointer"
            >
              ✕
            </button>
          )}
        </div>
        {open && query.length > 0 && (
          <div className="absolute z-30 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-48 overflow-y-auto">
            {filtered.length === 0 ? (
              <div className="px-3 py-2 text-sm text-gray-400 flex items-center gap-2">
                Nenhuma distribuidora encontrada.
                <Button type="button" variant="ghost" size="sm" className="text-blue-600" onClick={openQuickAdd}>
                  <Plus size={12} /> Cadastrar
                </Button>
              </div>
            ) : (
              <>
                {filtered.map((d) => (
                  <button
                    key={d.id}
                    type="button"
                    onClick={() => handleSelect(d)}
                    className="w-full text-left px-3 py-2 text-sm hover:bg-gray-50"
                  >
                    <span className="font-medium text-gray-900">{d.name}</span>
                  </button>
                ))}
                <button
                  type="button"
                  onClick={openQuickAdd}
                  className="w-full text-left px-3 py-2 text-sm text-blue-600 hover:bg-blue-50 border-t border-gray-100 flex items-center gap-1"
                >
                  <Plus size={12} /> Cadastrar nova distribuidora
                </button>
              </>
            )}
          </div>
        )}
      </div>

      <Dialog
        open={quickAddOpen}
        onOpenChange={(v) => !v && setQuickAddOpen(false)}
        title="Cadastrar distribuidora"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault()
            withPin(() => createMutation.mutate())
          }}
          className="space-y-3"
        >
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Nome</label>
            <Input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              required
              autoFocus
              autoComplete="off"
              maxLength={100}
            />
          </div>
          {createMutation.isError && <ErrorMessage error={createMutation.error} />}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setQuickAddOpen(false)}>
              Cancelar
            </Button>
            <Button type="submit" variant="primary" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'Salvando...' : 'Salvar'}
            </Button>
          </div>
        </form>
      </Dialog>
    </>
  )
}
