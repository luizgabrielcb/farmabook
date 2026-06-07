import { cn } from '@/lib/utils'
import { type SelectHTMLAttributes, forwardRef } from 'react'

type SelectProps = SelectHTMLAttributes<HTMLSelectElement>

export const Select = forwardRef<HTMLSelectElement, SelectProps>(({ className, children, ...props }, ref) => {
  return (
    <select
      ref={ref}
      className={cn(
        'flex h-8 w-full rounded border border-gray-300 bg-white px-3 text-sm text-gray-900',
        'focus:outline-none focus:ring-2 focus:ring-gray-400 focus:border-transparent',
        'disabled:opacity-50 disabled:cursor-not-allowed',
        className,
      )}
      {...props}
    >
      {children}
    </select>
  )
})

Select.displayName = 'Select'
