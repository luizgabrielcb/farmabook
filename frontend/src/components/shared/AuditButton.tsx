import * as Popover from '@radix-ui/react-popover'
import { History } from 'lucide-react'
import { cn } from '@/lib/utils'

export interface AuditRow { label: string; value: string }

export function AuditButton({
  rows,
  title = 'Auditoria',
  triggerClassName,
  iconSize = 14,
}: {
  rows: AuditRow[]
  title?: string
  triggerClassName?: string
  iconSize?: number
}) {
  return (
    <Popover.Root>
      <Popover.Trigger asChild>
        <button
          type="button"
          className={cn(
            'text-gray-400 hover:text-gray-700 cursor-pointer p-1 rounded transition-colors',
            triggerClassName,
          )}
          title="Ver histórico"
          onClick={(e) => e.stopPropagation()}
        >
          <History size={iconSize} />
        </button>
      </Popover.Trigger>
      <Popover.Portal>
        <Popover.Content
          sideOffset={6}
          align="end"
          onClick={(e) => e.stopPropagation()}
          className="z-[60] w-60 rounded-xl border border-gray-150 bg-white p-3 text-xs shadow-[0_12px_26px_-6px_rgba(20,26,32,0.18)]"
        >
          <p className="mb-2 text-[10px] font-semibold uppercase tracking-wide text-gray-400">{title}</p>
          <div className="space-y-1.5">
            {rows.map((r) => (
              <div key={r.label} className="flex justify-between gap-3">
                <span className="text-gray-400 shrink-0">{r.label}</span>
                <span className="text-right text-gray-700">{r.value}</span>
              </div>
            ))}
          </div>
          <Popover.Arrow className="fill-white" />
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  )
}
