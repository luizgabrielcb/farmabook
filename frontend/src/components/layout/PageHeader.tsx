import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  description?: string
  actions?: ReactNode
}

export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between px-4 py-3 sm:px-7 sm:py-4 bg-white border-b border-gray-150">
      <div className="min-w-0">
        <h1 className="text-base sm:text-lg font-extrabold tracking-tight text-gray-900">{title}</h1>
        {description && <p className="text-xs sm:text-sm text-gray-500 mt-0.5">{description}</p>}
      </div>
      {actions && <div className="flex items-center gap-2 flex-wrap shrink-0">{actions}</div>}
    </div>
  )
}
