import { useState, useRef, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Search, Plus } from 'lucide-react'
import { listCustomers } from '@/api/customers'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import type { Customer } from '@/types'

interface CustomerSearchProps {
  value: Customer | null
  onChange: (customer: Customer | null) => void
  onQuickAdd?: (query: string) => void
  placeholder?: string
}

export function CustomerSearch({ value, onChange, onQuickAdd, placeholder }: CustomerSearchProps) {
  const [query, setQuery] = useState(value?.name ?? '')
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  // Sincroniza quando value muda externamente (ex: após cadastro rápido)
  useEffect(() => {
    setQuery(value?.name ?? '')
  }, [value])

  const { data } = useQuery({
    queryKey: ['customers-all'],
    queryFn: () => listCustomers(0, 500),
  })

  const filtered = (data?.content ?? []).filter((c) =>
    c.name.toLowerCase().includes(query.toLowerCase()),
  )

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
        if (!value) setQuery('')
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [value])

  function handleSelect(customer: Customer) {
    onChange(customer)
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

  return (
    <div ref={containerRef} className="relative">
      <div className="relative">
        <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
        <Input
          value={query}
          onChange={handleInputChange}
          onFocus={() => setOpen(true)}
          placeholder={placeholder ?? 'Buscar cliente...'}
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
              Nenhum cliente encontrado.
              {onQuickAdd && (
                <Button type="button" variant="ghost" size="sm" className="text-blue-600" onClick={() => onQuickAdd(query)}>
                  <Plus size={12} /> Cadastrar
                </Button>
              )}
            </div>
          ) : (
            <>
              {filtered.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  onClick={() => handleSelect(c)}
                  className="w-full text-left px-3 py-2 text-sm hover:bg-gray-50 flex items-center justify-between"
                >
                  <span className="font-medium text-gray-900">{c.name}</span>
                  {c.phoneNumber && (
                    <span className="text-xs text-gray-400">{c.phoneNumber}</span>
                  )}
                </button>
              ))}
              {onQuickAdd && (
                <button
                  type="button"
                  onClick={() => onQuickAdd(query)}
                  className="w-full text-left px-3 py-2 text-sm text-blue-600 hover:bg-blue-50 border-t border-gray-100 flex items-center gap-1"
                >
                  <Plus size={12} /> Cadastrar novo cliente
                </button>
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}
