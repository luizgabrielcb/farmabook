import { cn } from '@/lib/utils'
import type { ReactNode } from 'react'

type Variant = 'gray' | 'blue' | 'yellow' | 'green' | 'red' | 'purple' | 'brand'

const variants: Record<Variant, { surface: string; dot: string }> = {
  gray: { surface: 'bg-gray-100 text-gray-700', dot: 'bg-gray-400' },
  blue: { surface: 'bg-blue-50 text-blue-700', dot: 'bg-blue-600' },
  yellow: { surface: 'bg-yellow-50 text-yellow-700', dot: 'bg-yellow-600' },
  green: { surface: 'bg-green-50 text-green-700', dot: 'bg-green-600' },
  red: { surface: 'bg-red-50 text-red-700', dot: 'bg-red-600' },
  purple: { surface: 'bg-purple-50 text-purple-700', dot: 'bg-purple-600' },
  brand: { surface: 'bg-brand-50 text-brand-700', dot: 'bg-brand-600' },
}

interface BadgeProps {
  variant?: Variant
  children: ReactNode
  className?: string
  /** Leading status dot (default true) */
  dot?: boolean
}

export function Badge({ variant = 'gray', children, className, dot = true }: BadgeProps) {
  const v = variants[variant]
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium whitespace-nowrap',
        v.surface,
        className,
      )}
    >
      {dot && <span className={cn('w-1.5 h-1.5 rounded-full shrink-0', v.dot)} />}
      {children}
    </span>
  )
}
