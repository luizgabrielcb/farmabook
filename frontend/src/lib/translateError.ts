// Traduz mensagens de erro vindas do backend (em inglês) para português.
// Cobre as mensagens de negócio mais comuns; se nenhuma regra casar, devolve
// a mensagem original para não esconder informação.

const ENTITIES: Record<string, string> = {
  Order: 'Encomenda',
  Customer: 'Cliente',
  Distributor: 'Distribuidora',
  Prescription: 'Receita',
  Compounding: 'Manipulação',
  CompoundingPharmacy: 'Farmácia de manipulação',
  Notification: 'Notificação',
  User: 'Usuário',
  Item: 'Item',
  Shortage: 'Falta',
  'Shortage order': 'Pedido de falta',
}

const EXACT: Record<string, string> = {
  'Invalid credentials': 'Credenciais inválidas.',
  'Name already in use': 'Nome já está em uso.',
  'Choose a different PIN': 'Escolha um PIN diferente.',
  'New PIN must be different from current PIN': 'O novo PIN deve ser diferente do atual.',
  'Notification not found': 'Notificação não encontrada.',
  'order must have at least one item': 'A encomenda deve ter ao menos um item.',
  'prescription must have at least one item': 'A receita deve ter ao menos um item.',
  'at least one item is required': 'Adicione ao menos um item.',
}

type Rule = [RegExp, (m: RegExpMatchArray) => string]

const RULES: Rule[] = [
  [
    /^The phone number '(.+?)' is already in use by customer '(.+?)'\.?$/i,
    (m) => `O telefone '${m[1]}' já está em uso pelo cliente '${m[2]}'.`,
  ],
  [
    /^The phone number '(.+?)' is already in use\.?$/i,
    (m) => `O telefone '${m[1]}' já está em uso.`,
  ],
  // "<Entity> with name '...' already exists"
  [
    /^(.+?) with name '(.+?)' already exists\.?$/i,
    (m) => `${translateEntity(m[1])} com o nome '${m[2]}' já existe.`,
  ],
  // "<Entity> with id '...' not found" / "with name '...' not found"
  [
    /^(.+?) with (id|name) '(.+?)' not found\.?$/i,
    (m) => `${translateEntity(m[1])} não encontrado(a).`,
  ],
  [
    /^(.+?) with id '.+?' is not PENDING and cannot be (modified|deleted)\.?$/i,
    (m) => `${translateEntity(m[1])} não está pendente e não pode ser ${m[2].toLowerCase() === 'deleted' ? 'excluído(a)' : 'alterado(a)'}.`,
  ],
  [
    /^(.+?) with id '.+?' is already ordered\.?$/i,
    (m) => `${translateEntity(m[1])} já foi marcado(a) como pedido.`,
  ],
  [
    /^(.+?) with id '.+?' has DELIVERED items and cannot be deleted\.?$/i,
    (m) => `${translateEntity(m[1])} possui itens entregues e não pode ser excluído(a).`,
  ],
]

function translateEntity(raw: string): string {
  const key = Object.keys(ENTITIES).find((k) => k.toLowerCase() === raw.trim().toLowerCase())
  return key ? ENTITIES[key] : raw.trim()
}

export function translateError(message: string): string {
  if (!message) return message
  const trimmed = message.trim()
  if (EXACT[trimmed]) return EXACT[trimmed]
  for (const [re, fn] of RULES) {
    const match = trimmed.match(re)
    if (match) return fn(match)
  }
  return message
}
