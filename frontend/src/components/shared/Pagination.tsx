import { Button } from '@/components/ui/button'
import { ChevronLeft, ChevronRight } from 'lucide-react'

interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  size: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, totalElements, size, onPageChange }: PaginationProps) {
  const from = totalElements === 0 ? 0 : page * size + 1
  const to = Math.min((page + 1) * size, totalElements)

  return (
    <div className="flex items-center justify-between px-4 py-2.5 border-t border-gray-200 bg-white text-sm text-gray-500">
      <span>
        {from}–{to} de {totalElements}
      </span>
      <div className="flex items-center gap-1">
        <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft size={14} />
        </Button>
        <span className="px-2 text-gray-700 font-medium">
          {page + 1} / {totalPages || 1}
        </span>
        <Button
          variant="ghost"
          size="sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          <ChevronRight size={14} />
        </Button>
      </div>
    </div>
  )
}
