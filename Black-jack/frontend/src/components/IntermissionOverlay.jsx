import { useEffect, useState } from 'react'

export function buildBetOptions(minBet, maxBet, walletBalance) {
  const maxAllowed = Math.min(maxBet, Math.floor((walletBalance ?? 0) * 0.25))
  if (maxAllowed < minBet) {
    return [minBet]
  }
  const candidates = [
    minBet,
    minBet * 2,
    minBet * 5,
    25,
    50,
    100,
    Math.floor((minBet + maxAllowed) / 2),
    maxAllowed,
  ]
  const unique = [...new Set(candidates.filter((v) => v >= minBet && v <= maxAllowed))]
  return unique.sort((a, b) => a - b).slice(0, 6)
}

export default function IntermissionOverlay({
  endsAt,
  minBet,
  maxBet,
  walletBalance,
  selectedBet,
  onSelectBet,
  onLeave,
  leaving,
}) {
  const [secondsLeft, setSecondsLeft] = useState(0)
  const betOptions = buildBetOptions(minBet, maxBet, walletBalance)
  const maxAllowed = Math.min(maxBet, Math.floor((walletBalance ?? 0) * 0.25))

  useEffect(() => {
    function tick() {
      if (!endsAt) {
        setSecondsLeft(0)
        return
      }
      const remaining = Math.max(0, Math.ceil((new Date(endsAt).getTime() - Date.now()) / 1000))
      setSecondsLeft(remaining)
    }
    tick()
    const id = setInterval(tick, 250)
    return () => clearInterval(id)
  }, [endsAt])

  return (
    <div className="intermission-overlay">
      <div className="intermission-modal">
        <h2>Następna runda za {secondsLeft} s</h2>
        <p className="intermission-label">Wpisowe na następną rundę</p>
        <div className="bet-tiles">
          {betOptions.map((amount) => (
            <button
              key={amount}
              type="button"
              className={`bet-tile ${selectedBet === amount ? 'selected' : ''}`}
              onClick={() => onSelectBet(amount)}
            >
              {amount}
            </button>
          ))}
        </div>
        <div className="custom-bet-row">
          <label>
            Inna kwota ({minBet}–{maxAllowed}):
            <input
              type="number"
              min={minBet}
              max={maxAllowed}
              value={selectedBet}
              onChange={(e) => onSelectBet(Number(e.target.value))}
            />
          </label>
        </div>
        <p className="intermission-hint">
          Zakład zostanie postawiony automatycznie po rozpoczęciu rundy.
        </p>
        <button type="button" className="secondary intermission-leave" disabled={leaving} onClick={onLeave}>
          {leaving ? 'Wychodzenie…' : 'Opuść grę'}
        </button>
      </div>
    </div>
  )
}
