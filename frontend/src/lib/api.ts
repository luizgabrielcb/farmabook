import axios from 'axios'

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

let _activePin: string | null = null

export function setActivePin(pin: string | null) {
  _activePin = pin
}

api.interceptors.request.use((config) => {
  if (_activePin && !config.headers['X-Auth-Pin']) {
    config.headers['X-Auth-Pin'] = _activePin
  }
  return config
})
