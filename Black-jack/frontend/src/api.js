const API = '/api/v1'

export function getToken() {
  return localStorage.getItem('token')
}

export function setAuth(token, userId) {
  localStorage.setItem('token', token)
  localStorage.setItem('userId', userId)
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
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`)
  return data
}

export const api = {
  register: (body) => request('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  registerDealer: (body) => request('/auth/register-dealer', { method: 'POST', body: JSON.stringify(body) }),
  login: (body) => request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  wallet: () => request('/wallet'),
  me: () => request('/users/me'),
  tables: () => request('/tables'),
  placeBet: (tableId, amount) =>
    request(`/tables/${tableId}/bets`, { method: 'POST', body: JSON.stringify({ amount }) }),
}
