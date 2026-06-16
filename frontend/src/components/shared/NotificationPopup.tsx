import { MessageCircle, X, ExternalLink } from 'lucide-react'
import type { Notification } from '@/types'

function formatPhone(raw: string): string {
  const digits = raw.replace(/\D/g, '')

  // Brasil: +55 (DDD) 9 XXXX-XXXX  → 55 + 2 DDD + 9 dígitos = 13 dígitos
  if (digits.startsWith('55') && digits.length === 13) {
    const ddd = digits.slice(2, 4)
    const nine = digits.slice(4, 5)
    const part1 = digits.slice(5, 9)
    const part2 = digits.slice(9, 13)
    return `+55 (${ddd}) ${nine} ${part1}-${part2}`
  }

  // Brasil sem nono dígito: 55 + 2 DDD + 8 dígitos = 12 dígitos
  if (digits.startsWith('55') && digits.length === 12) {
    const ddd = digits.slice(2, 4)
    const part1 = digits.slice(4, 8)
    const part2 = digits.slice(8, 12)
    return `+55 (${ddd}) ${part1}-${part2}`
  }

  // EUA: +1 (XXX) XXX-XXXX → 1 + 10 dígitos = 11 dígitos
  if (digits.startsWith('1') && digits.length === 11) {
    const area = digits.slice(1, 4)
    const part1 = digits.slice(4, 7)
    const part2 = digits.slice(7, 11)
    return `+1 (${area}) ${part1}-${part2}`
  }

  // Fallback: retorna com + na frente
  return raw.startsWith('+') ? raw : `+${raw}`
}

interface NotificationPopupProps {
  notification: Notification
  onClose: () => void
}

export function NotificationPopup({ notification, onClose }: NotificationPopupProps) {
  const formattedPhone = formatPhone(notification.customerPhone)

  return (
    <div className="fixed bottom-6 right-6 z-50 w-72 bg-white border border-gray-150 rounded-2xl shadow-[0_12px_26px_-6px_rgba(20,26,32,0.18)] p-4">
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2 text-[#1da851]">
          <MessageCircle size={15} />
          <span className="text-sm font-semibold">Pedido recebido!</span>
        </div>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600 cursor-pointer">
          <X size={14} />
        </button>
      </div>

      <p className="text-sm font-medium text-gray-900 mb-0.5">
        Notificar {notification.customerName} via WhatsApp!
      </p>
      <p className="text-xs text-gray-500 mb-3 font-mono">{formattedPhone}</p>

      <a
        href={notification.link}
        target="_blank"
        rel="noopener noreferrer"
        onClick={onClose}
        className="w-full"
      >
        <button className="w-full h-8 rounded-md bg-[#25d366] hover:bg-[#1da851] text-white text-sm font-medium flex items-center justify-center gap-1.5 transition-colors cursor-pointer">
          <ExternalLink size={13} /> Abrir WhatsApp
        </button>
      </a>
    </div>
  )
}
