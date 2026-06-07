import { Badge } from '@/components/ui/badge'
import type { Category } from '@/types'

const categoryMap: Record<Category, { label: string; variant: 'gray' | 'blue' | 'yellow' | 'green' | 'red' | 'purple' }> = {
  MEDICAMENTOS: { label: 'Medicamentos', variant: 'blue' },
  PERFUMARIA: { label: 'Perfumaria', variant: 'red' },
  SUPLEMENTOS: { label: 'Suplementos', variant: 'green' },
  PRODUTOS_NATURAIS: { label: 'Produtos Naturais', variant: 'yellow' },
  OUTROS: { label: 'Outros', variant: 'gray' },
}

export function CategoryBadge({ category }: { category: Category }) {
  const map = categoryMap[category]
  return <Badge variant={map.variant}>{map.label}</Badge>
}

export const CATEGORY_OPTIONS: { value: Category; label: string }[] = [
  { value: 'MEDICAMENTOS', label: 'Medicamentos' },
  { value: 'PERFUMARIA', label: 'Perfumaria' },
  { value: 'SUPLEMENTOS', label: 'Suplementos' },
  { value: 'PRODUTOS_NATURAIS', label: 'Produtos Naturais' },
  { value: 'OUTROS', label: 'Outros' },
]
