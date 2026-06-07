// Flag module-level — lida pelo Dialog do Radix para bloquear fechamento
let _open = false
export const pinState = {
  get open() { return _open },
  set open(v: boolean) { _open = v },
}
