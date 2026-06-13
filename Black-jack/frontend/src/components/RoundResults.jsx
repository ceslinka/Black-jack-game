function outcomeLabel(outcome) {
  switch (outcome) {
    case 'win':
      return 'Wygrana'
    case 'lose':
      return 'Przegrana'
    case 'push':
      return 'Remis'
    default:
      return outcome
  }
}

function usernameFor(seats, userId) {
  const seat = seats?.find((s) => String(s.userId) === String(userId))
  return seat?.username ?? 'Gracz'
}

export default function RoundResults({ results, seats, dealerValue }) {
  if (!results?.length) {
    return null
  }

  return (
    <div className="round-results">
      {dealerValue != null && (
        <p className="dealer-score">
          Krupier: <strong>{dealerValue}</strong>
        </p>
      )}
      <ul className="result-list">
        {results.map((r) => (
          <li key={r.userId} className={`result-item result-${r.outcome}`}>
            <span className="result-name">{usernameFor(seats, r.userId)}</span>
            <span className="result-score">{r.handValue} pkt</span>
            <span className="result-outcome">{outcomeLabel(r.outcome)}</span>
            {r.payout > 0 && <span className="result-payout">+{r.payout} żetonów</span>}
          </li>
        ))}
      </ul>
    </div>
  )
}
