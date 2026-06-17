import { cn } from '@/lib/utils'
import { type SelectHTMLAttributes, forwardRef } from 'react'

type SelectProps = SelectHTMLAttributes<HTMLSelectElement>

export const Select = forwardRef<HTMLSelectElement, SelectProps>(({ className, children, ...props }, ref) => {
  return (
    <select
      ref={ref}
      className={cn(
        // text-base on mobile keeps font ≥16px so iOS doesn't auto-zoom on focus
        'flex h-8 w-full rounded-md border border-gray-200 bg-white px-3 text-base md:text-sm text-gray-900',
        'transition-colors focus:outline-none focus:ring-[3px] focus:ring-brand-500/30 focus:border-brand-500',
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
