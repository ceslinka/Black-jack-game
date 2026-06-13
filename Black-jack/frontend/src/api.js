const API = '/api/v1'

export function getToken() {
  return localStorage.getItem('token')
}

export function setAuth(token, userId) {
  localStorage.setItem('token', token)
  localStorage.setItem('userId', String(userId))
}

export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
}

export function getUserId() {
  return localStorage.getItem('userId')
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(`${API}${path}`, { ...options, headers })
  if (res.status === 204) return null
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`)
  return data
}

export const api = {
  register: (body) => request('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  login: (body) => request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  wallet: () => request('/wallet'),
  me: () => request('/users/me'),
  mySeat: () => request('/users/me/seat'),
  tables: () => request('/tables'),
  getTableState: (tableId) => request(`/tables/${tableId}/state`),
  getGameState: (tableId) => request(`/tables/${tableId}/game-state`),
  joinTable: (tableId, seatIndex) =>
    request(`/tables/${tableId}/join`, {
      method: 'POST',
      body: JSON.stringify({ seatIndex }),
    }),
  leaveTable: (tableId) => request(`/tables/${tableId}/leave`, { method: 'DELETE' }),
  placeBet: (tableId, amount) =>
    request(`/tables/${tableId}/bets`, { method: 'POST', body: JSON.stringify({ amount }) }),
  gameHistory: () => request('/users/me/game-history'),
}
