import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, setAuth } from '../api'

export default function LoginPage() {
  const nav = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [registerMode, setRegisterMode] = useState(false)
  const [username, setUsername] = useState('')
  const [error, setError] = useState('')

  async function submit(e) {
    e.preventDefault()
    setError('')
    try {
      const body = { email, password, username }
      const data = registerMode ? await api.register(body) : await api.login({ email, password })
      setAuth(data.token, data.userId)
      nav('/lobby')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="container">
      <h1>🃏 Blackjack Casino</h1>
      <div className="card" style={{ maxWidth: 420 }}>
        <h2>{registerMode ? 'Rejestracja' : 'Logowanie'}</h2>
        <form onSubmit={submit}>
          {registerMode && (
            <p>
              <input placeholder="Username" value={username} onChange={(e) => setUsername(e.target.value)} required />
            </p>
          )}
          <p>
            <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </p>
          <p>
            <input type="password" placeholder="Hasło (min 8)" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </p>
          {error && <p className="error">{error}</p>}
          <button type="submit">{registerMode ? 'Zarejestruj' : 'Zaloguj'}</button>
          <button type="button" className="secondary" onClick={() => setRegisterMode(!registerMode)}>
            {registerMode ? 'Mam konto' : 'Rejestracja'}
          </button>
        </form>
      </div>
    </div>
  )
}

export function RequireAuth({ children }) {
  const token = localStorage.getItem('token')
  if (!token) return <Link to="/">Zaloguj się</Link>
  return children
}
