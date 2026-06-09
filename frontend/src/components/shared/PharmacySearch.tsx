import { useState, useRef, useEffect } from 'react'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import { Search, Plus } from 'lucide-react'
import { listCompoundingPharmacies, createCompoundingPharmacy } from '@/api/compounding-pharmacies'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { ErrorMessage } from '@/components/shared/ErrorMessage'
import { useWithPin } from '@/context/PinContext'
import type { CompoundingPharmacy } from '@/types'

interface PharmacySearchProps {
  value: CompoundingPharmacy | null
  onChange: (pharmacy: CompoundingPharmacy | null) => void
}

export function PharmacySearch({ value, onChange }: PharmacySearchProps) {
  const [query, setQuery] = useState(value ? `${value.name} — ${value.city}` : '')
  const [open, setOpen] = useState(false)
  const [quickAddOpen, setQuickAddOpen] = useState(false)
  const [newName, setNewName] = useState('')
  const [newCity, setNewCity] = useState('')
  const containerRef = useRef<HTMLDivElement>(null)
  const withPin = useWithPin()
  const qc = useQueryClient()

  useEffect(() => {
    setQuery(value ? `${value.name} — ${value.city}` : '')
  }, [value])

  const { data } = useQuery({
    queryKey: ['compounding-pharmacies'],
    queryFn: () => listCompoundingPharmacies(),
  })

  const filtered = (data?.content ?? []).filter((p) =>
    `${p.name} ${p.city}`.toLowerCase().includes(query.toLowerCase()),
  )

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
        if (!value) setQuery('')
        else setQuery(`${value.name} — ${value.city}`)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [value])

  const createMutation = useMutation({
    mutationFn: () => createCompoundingPharmacy({ name: newName, city: newCity }),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ['compounding-pharmacies'] })
      onChange(created)
      setQuickAddOpen(false)
      setNewName('')
      setNewCity('')
    },
  })

  function handleSelect(pharmacy: CompoundingPharmacy) {
    onChange(pharmacy)
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
    setNewCity('')
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
            placeholder="Buscar farmácia..."
            className="pl-8 pr-8"
          />
          {value && (
            <button type="button" onClick={handleClear}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-xs cursor-pointer">
              ✕
            </button>
          )}
        </div>

        {open && query.length > 0 && (
          <div className="absolute z-30 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-48 overflow-y-auto">
            {filtered.length === 0 ? (
              <div className="px-3 py-2 text-sm text-gray-400 flex items-center gap-2">
                Nenhuma farmácia encontrada.
                <Button type="button" variant="ghost" size="sm" className="text-blue-600" onClick={openQuickAdd}>
                  <Plus size={12} /> Cadastrar
                </Button>
              </div>
            ) : (
              <>
                {filtered.map((p) => (
                  <button key={p.id} type="button" onClick={() => handleSelect(p)}
                    className="w-full text-left px-3 py-2 text-sm hover:bg-gray-50 flex items-center justify-between">
                    <span className="font-medium text-gray-900">{p.name}</span>
                    <span className="text-xs text-gray-400">{p.city}</span>
                  </button>
                ))}
                <button type="button" onClick={openQuickAdd}
                  className="w-full text-left px-3 py-2 text-sm text-blue-600 hover:bg-blue-50 border-t border-gray-100 flex items-center gap-1">
                  <Plus size={12} /> Cadastrar nova farmácia
                </button>
              </>
            )}
          </div>
        )}
      </div>

      <Dialog open={quickAddOpen} onOpenChange={(v) => !v && setQuickAddOpen(false)} title="Cadastrar farmácia">
        <form onSubmit={(e) => { e.preventDefault(); withPin(() => createMutation.mutate()) }} className="space-y-3">
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Nome</label>
            <Input value={newName} onChange={(e) => setNewName(e.target.value)}
              required autoFocus autoComplete="off" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700 block mb-1">Cidade</label>
            <Input value={newCity} onChange={(e) => setNewCity(e.target.value)}
              required autoComplete="off" />
          </div>
          {createMutation.isError && <ErrorMessage error={createMutation.error} />}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setQuickAddOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="primary" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'Salvando...' : 'Salvar'}
            </Button>
          </div>
        </form>
      </Dialog>
    </>
  )
}
