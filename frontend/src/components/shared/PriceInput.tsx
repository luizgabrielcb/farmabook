import { Input } from '@/components/ui/input'

interface Props {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  className?: string
}

export function PriceInput({ value, onChange, placeholder = '0,00', className }: Props) {
  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const raw = e.target.value
    // Allow empty, or: up to 6 integer digits + optional separator (. or ,) + up to 2 decimal digits
    if (/^(\d{0,6}([.,]\d{0,2})?)?$/.test(raw)) {
      const numeric = Number(raw.replace(',', '.'))
      if (raw === '' || numeric <= 100000) {
        onChange(raw)
      }
    }
  }

  return (
    <Input
      type="text"
      inputMode="decimal"
      value={value}
      onChange={handleChange}
      placeholder={placeholder}
      className={className}
      autoComplete="off"
    />
  )
}

/** Convert PriceInput string value to number or null for API submission */
export function parsePriceInput(value: string): number | null {
  if (value === '') return null
  const num = Number(value.replace(',', '.'))
  return isNaN(num) ? null : num
}
