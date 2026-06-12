const SUIT_SYMBOL = { H: '♥', D: '♦', S: '♠', C: '♣' }
const RED = ['H', 'D']

export function formatCard(code) {
  if (!code || code === '**') return '??'
  const rank = code.charAt(0)
  const suit = code.charAt(1)
  const rankLabel = rank === 'T' ? '10' : rank
  return `${rankLabel}${SUIT_SYMBOL[suit] || suit}`
}

export function isRed(code) {
  return code && code.length === 2 && RED.includes(code.charAt(1))
}

export default function CardView({ code }) {
  if (!code) return null
  const hidden = code === '**'
  return (
    <span className={`playing-card ${hidden ? 'hidden' : isRed(code) ? 'red' : ''}`}>
      {hidden ? '🂠' : formatCard(code)}
    </span>
  )
}
