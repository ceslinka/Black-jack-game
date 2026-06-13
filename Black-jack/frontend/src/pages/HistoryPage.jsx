import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'

const OUTCOME_LABELS = {
  win: 'Wygrana',
  lose: 'Przegrana',
  push: 'Remis',
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pl-PL')
}

export default function HistoryPage() {
  const [games, setGames] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    api
      .gameHistory()
      .then((res) => setGames(res.games || []))
      .catch((err) => setError(err.message))
  }, [])

  return (
    <div className="container">
      <div className="header-bar">
        <Link to="/lobby">← Lobby</Link>
        <h1 style={{ margin: 0 }}>Historia</h1>
      </div>

      {error && <p className="error">{error}</p>}

      <h2>Twoje gry</h2>
      {games.length === 0 && <p><em>Brak rozegranych gier.</em></p>}
      <div className="history-list">
        {games.map((g) => (
          <div key={g.roundId} className={`history-card history-${g.outcome}`}>
            <div className="history-row">
              <strong>{g.tableName}</strong>
              <span>{OUTCOME_LABELS[g.outcome] || g.outcome}</span>
            </div>
            <div className="history-row subtle">
              <span>Zakład: {g.betAmount}</span>
              <span>Wypłata: {g.payout}</span>
              <span>Zmiana: {g.netChange >= 0 ? '+' : ''}{g.netChange}</span>
            </div>
            <div className="history-row subtle">
              <span>Saldo przed: {g.balanceBefore}</span>
              <span>Saldo po: {g.balanceAfter}</span>
            </div>
            <div className="history-date">{formatDate(g.settledAt)}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
