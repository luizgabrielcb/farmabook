import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

/**
 * Mobile-only card primitives. On screens < md these replace the data tables:
 * each table row becomes a stacked card so the most important fields stay
 * visible without horizontal scrolling. The desktop `<Table>` is hidden with
 * `hidden md:block` and these are hidden with `md:hidden`, so the two never
 * render at once and the desktop layout is untouched.
 */

export function CardList({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('md:hidden divide-y divide-gray-100', className)}>{children}</div>
}

export function MobileCard({
  children,
  onClick,
  className,
}: {
  children: ReactNode
  onClick?: () => void
  className?: string
}) {
  return (
    <div
      onClick={onClick}
      className={cn('px-4 py-3.5 space-y-2.5', onClick && 'cursor-pointer active:bg-gray-50', className)}
    >
      {children}
    </div>
  )
}

/** A label/value row inside a card. */
export function CardField({
  label,
  children,
  className,
}: {
  label: string
  children: ReactNode
  className?: string
}) {
  return (
    <div className={cn('flex items-center justify-between gap-3 text-sm', className)}>
      <span className="text-gray-400 shrink-0">{label}</span>
      <span className="text-gray-700 text-right min-w-0 break-words">{children}</span>
    </div>
  )
}

/** Action row with ≥44px touch targets, pinned to the bottom of a card. */
export function CardActions({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cn('flex items-center justify-end gap-1 pt-1.5 mt-0.5 border-t border-gray-100', className)}>
      {children}
    </div>
  )
}

/** Square icon button sized for thumbs (44×44). */
export function IconAction({
  onClick,
  label,
  className,
  children,
  disabled,
}: {
  onClick?: () => void
  label: string
  className?: string
  children: ReactNode
  disabled?: boolean
}) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      disabled={disabled}
      onClick={(e) => { e.stopPropagation(); onClick?.() }}
      className={cn(
        'grid place-items-center h-11 w-11 rounded-lg text-gray-500 active:bg-gray-100 disabled:opacity-40 cursor-pointer',
        className,
      )}
    >
      {children}
    </button>
  )
}

export function CardEmpty({ children }: { children: ReactNode }) {
  return <p className="md:hidden text-center text-gray-400 py-10 text-sm">{children}</p>
}
