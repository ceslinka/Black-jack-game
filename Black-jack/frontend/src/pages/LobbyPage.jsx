import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, clearAuth } from '../api'

export default function LobbyPage() {
  const nav = useNavigate()
  const [tables, setTables] = useState([])
  const [wallet, setWallet] = useState(null)
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    load()
  }, [])

  async function load() {
    try {
      const [t, w, p] = await Promise.all([api.tables(), api.wallet(), api.me()])
      setTables(t.tables || [])
      setWallet(w)
      setProfile(p)
    } catch (err) {
      setError(err.message)
    }
  }

  function logout() {
    clearAuth()
    nav('/')
  }

  return (
    <div className="container">
      <div className="header-bar">
        <h1>Lobby</h1>
        <div>
          <span className="badge">{wallet?.balance ?? '…'} żetonów</span>
          <button className="secondary" onClick={logout} style={{ marginLeft: 8 }}>
            Wyloguj
          </button>
        </div>
      </div>
      {profile && (
        <p>
          Zalogowany: <strong>{profile.username}</strong> ({profile.role})
        </p>
      )}
      {error && <p className="error">{error}</p>}
      <h2>Stoły</h2>
      {tables.map((t) => (
        <div className="card" key={t.id}>
          <h3>{t.name}</h3>
          <p>
            Gracze: {t.currentPlayers}/{t.maxPlayers} · Zakład: {t.minBet}–{t.maxBet} · Status:{' '}
            <strong>{t.status}</strong>
          </p>
          <Link to={`/table/${t.id}`}>
            <button>Wejdź</button>
          </Link>
        </div>
      ))}
    </div>
  )
}
