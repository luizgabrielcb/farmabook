import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { validatePin } from '@/api/auth'
import { useAuth } from '@/context/AuthContext'
import { Button } from '@/components/ui/button'
import { ErrorMessage } from '@/components/shared/ErrorMessage'

export function LoginPage() {
  const [pin, setPin] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()

  const mutation = useMutation({
    mutationFn: () => validatePin(pin),
    onSuccess: (user) => {
      login(user, pin)
      navigate('/orders')
    },
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (pin.length > 0) mutation.mutate()
  }

  function handleKey(digit: string) {
    if (pin.length < 4) setPin((p) => p + digit)
  }

  function handleDelete() {
    setPin((p) => p.slice(0, -1))
  }

  const digits = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', '⌫']

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="bg-white border border-gray-200 rounded-lg shadow-sm w-72 p-8">
        <div className="text-center mb-6">
          <h1 className="text-lg font-semibold text-gray-900">FarmaBook</h1>
          <p className="text-sm text-gray-500 mt-1">Digite seu PIN para entrar</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="flex justify-center gap-2 mb-6">
            {[0, 1, 2, 3].map((i) => (
              <div
                key={i}
                className="w-9 h-9 rounded border border-gray-300 flex items-center justify-center text-lg font-medium text-gray-900 bg-gray-50"
              >
                {pin[i] ? '•' : ''}
              </div>
            ))}
          </div>

          <div className="grid grid-cols-3 gap-2 mb-4">
            {digits.map((d, i) => {
              if (d === '') return <div key={i} />
              const isDel = d === '⌫'
              return (
                <button
                  key={i}
                  type={isDel ? 'button' : 'button'}
                  onClick={() => (isDel ? handleDelete() : handleKey(d))}
                  className="h-10 rounded border border-gray-200 text-sm font-medium text-gray-700 hover:bg-gray-50 active:bg-gray-100 transition-colors cursor-pointer"
                >
                  {d}
                </button>
              )
            })}
          </div>

          {mutation.isError && (
            <div className="mb-3">
              <ErrorMessage error={mutation.error} />
            </div>
          )}

          <Button
            type="submit"
            variant="primary"
            className="w-full"
            disabled={pin.length === 0 || mutation.isPending}
          >
            {mutation.isPending ? 'Entrando...' : 'Entrar'}
          </Button>
        </form>
      </div>
    </div>
  )
}
