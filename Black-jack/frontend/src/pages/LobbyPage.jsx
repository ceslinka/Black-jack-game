import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, clearAuth } from '../api'

export default function LobbyPage() {
  const nav = useNavigate()
  const [tables, setTables] = useState([])
  const [wallet, setWallet] = useState(null)
  const [profile, setProfile] = useState(null)
  const [mySeat, setMySeat] = useState(null)
  const [error, setError] = useState('')
  const [leaving, setLeaving] = useState(false)

  useEffect(() => {
    load()
  }, [])

  async function load() {
    try {
      const [t, w, p, seat] = await Promise.all([
        api.tables(),
        api.wallet(),
        api.me(),
        api.mySeat(),
      ])
      setTables(t.tables || [])
      setWallet(w)
      setProfile(p)
      setMySeat(seat)
    } catch (err) {
      setError(err.message)
    }
  }

  async function leaveTable() {
    if (!mySeat) return
    setLeaving(true)
    setError('')
    try {
      await api.leaveTable(mySeat.tableId)
      setMySeat(null)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setLeaving(false)
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
          <Link to="/history" style={{ marginRight: 12 }}>Historia</Link>
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

      {mySeat && (
        <div className="card">
          <p>
            ⚠️ Siedzisz przy stole <strong>{mySeat.tableName}</strong> (miejsce {mySeat.seatIndex})
          </p>
          <Link to={`/table/${mySeat.tableId}`}>
            <button>Wróć do stołu</button>
          </Link>
          <button className="secondary" disabled={leaving} onClick={leaveTable} style={{ marginLeft: 8 }}>
            {leaving ? 'Wychodzenie…' : 'Opuść stół'}
          </button>
        </div>
      )}

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
