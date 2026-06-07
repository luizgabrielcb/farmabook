import { AlertCircle } from 'lucide-react'
import { isAxiosError } from 'axios'

interface ErrorMessageProps {
  error: unknown
  className?: string
}

export function ErrorMessage({ error, className }: ErrorMessageProps) {
  const message = isAxiosError(error)
    ? (error.response?.data?.message ?? error.message)
    : 'Ocorreu um erro inesperado.'

  return (
    <div className={`flex items-center gap-2 text-sm text-red-600 ${className ?? ''}`}>
      <AlertCircle size={14} />
      <span>{message}</span>
    </div>
  )
}
