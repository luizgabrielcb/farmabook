import { useState } from 'react'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

type Country = 'BR' | 'US' | 'OTHER'

const COUNTRIES: { code: Country; flag: string; label: string; prefix: string; mask: string }[] = [
  { code: 'BR', flag: '🇧🇷', label: 'Brasil', prefix: '+55', mask: '(##) # ####-####' },
  { code: 'US', flag: '🇺🇸', label: 'EUA', prefix: '+1', mask: '(###) ###-####' },
  { code: 'OTHER', flag: '🌐', label: 'Outro', prefix: '', mask: '' },
]

function applyMask(value: string, mask: string): string {
  const digits = value.replace(/\D/g, '')
  let result = ''
  let di = 0
  for (let i = 0; i < mask.length && di < digits.length; i++) {
    if (mask[i] === '#') {
      result += digits[di++]
    } else {
      result += mask[i]
    }
  }
  return result
}

function detectCountry(phone: string | null | undefined): Country {
  if (!phone) return 'BR'
  if (phone.startsWith('+55')) return 'BR'
  if (phone.startsWith('+1')) return 'US'
  if (phone) return 'OTHER'
  return 'BR'
}

interface PhoneInputProps {
  value: string
  onChange: (value: string) => void
  className?: string
}

export function PhoneInput({ value, onChange, className }: PhoneInputProps) {
  const [country, setCountry] = useState<Country>(detectCountry(value))
  const [showDropdown, setShowDropdown] = useState(false)

  const selected = COUNTRIES.find((c) => c.code === country)!

  function handleCountrySelect(code: Country) {
    setCountry(code)
    setShowDropdown(false)
    onChange('')
  }

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const raw = e.target.value
    if (country === 'OTHER') {
      onChange(raw)
      return
    }
    const masked = applyMask(raw, selected.mask)
    const full = selected.prefix + ' ' + masked
    onChange(full.trimEnd())
  }

  const displayValue =
    country === 'OTHER'
      ? value
      : value.startsWith(selected.prefix)
        ? value.slice(selected.prefix.length + 1)
        : value

  return (
    <div className={cn('flex gap-2', className)}>
      <div className="relative">
        <button
          type="button"
          onClick={() => setShowDropdown((v) => !v)}
          className="h-8 px-2 rounded border border-gray-300 bg-white text-sm flex items-center gap-1 hover:bg-gray-50 cursor-pointer whitespace-nowrap"
        >
          <span>{selected.flag}</span>
          <span className="text-gray-500 text-xs">{selected.prefix || 'Outro'}</span>
          <span className="text-gray-400 text-xs">▾</span>
        </button>

        {showDropdown && (
          <div className="absolute z-20 top-full mt-1 left-0 bg-white border border-gray-200 rounded-lg shadow-lg min-w-36">
            {COUNTRIES.map((c) => (
              <button
                key={c.code}
                type="button"
                onClick={() => handleCountrySelect(c.code)}
                className={cn(
                  'w-full text-left px-3 py-2 text-sm flex items-center gap-2 hover:bg-gray-50',
                  country === c.code && 'bg-gray-50 font-medium',
                )}
              >
                <span>{c.flag}</span>
                <span>{c.label}</span>
                {c.prefix && <span className="text-gray-400 text-xs ml-auto">{c.prefix}</span>}
              </button>
            ))}
          </div>
        )}
      </div>

      <Input
        value={displayValue}
        onChange={handleInputChange}
        placeholder={
          country === 'BR'
            ? '(11) 9 9999-9999'
            : country === 'US'
              ? '(555) 123-4567'
              : '+44 20 1234 5678'
        }
        className="flex-1"
      />
    </div>
  )
}
