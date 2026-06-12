import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, getUserId } from '../api'
import { createStompClient, sendHit, sendJoin, sendStand, subscribeErrors, subscribeTable } from '../ws'
import CardView from '../components/CardView'

export default function TablePage() {
  const { tableId } = useParams()
  const userId = getUserId()
  const clientRef = useRef(null)
  const [wallet, setWallet] = useState(null)
  const [profile, setProfile] = useState(null)
  const [tableState, setTableState] = useState(null)
  const [gameState, setGameState] = useState(null)
  const [playerTurn, setPlayerTurn] = useState(null)
  const [lastResult, setLastResult] = useState(null)
  const [betAmount, setBetAmount] = useState(10)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [joined, setJoined] = useState(false)

  useEffect(() => {
    api.me().then(setProfile)
    api.wallet().then(setWallet)

    const client = createStompClient((c) => {
      clientRef.current = c
      subscribeTable(c, tableId, handleEvent)
      subscribeErrors(c, (e) => setError(e.message))
    })

    return () => client.deactivate()
  }, [tableId])

  function handleEvent(event) {
    setMessage(`Event: ${event.type}`)
    switch (event.type) {
      case 'table.state':
        setTableState(event)
        break
      case 'game.state':
        setGameState(event)
        break
      case 'player.turn':
        setPlayerTurn(event)
        break
      case 'round.started':
        setLastResult(null)
        break
      case 'round.result':
      case 'round.settled':
        setLastResult(event)
        setPlayerTurn(null)
        api.wallet().then(setWallet)
        break
      case 'round.cannot_start':
        setError('Gra nie może wystartować: brak krupiera')
        break
      case 'bet.placed':
        api.wallet().then(setWallet)
        break
      default:
        break
    }
  }

  function join(seatIndex, asDealer) {
    sendJoin(clientRef.current, tableId, seatIndex, asDealer)
    setJoined(true)
  }

  async function placeBet() {
    setError('')
    try {
      const res = await api.placeBet(tableId, Number(betAmount))
      setWallet({ balance: res.newBalance, currency: 'chips' })
    } catch (err) {
      setError(err.message)
    }
  }

  function mySeat() {
    return gameState?.seats?.find((s) => s.userId === userId && !s.dealer)
  }

  const isMyTurn = playerTurn?.userId === userId
  const myHand = mySeat()

  return (
    <div className="container">
      <div className="header-bar">
        <Link to="/lobby">← Lobby</Link>
        <span className="badge">{wallet?.balance ?? '…'} żetonów</span>
      </div>

      <h1>Stół</h1>
      {message && <p><small>{message}</small></p>}
      {error && <p className="error">{error}</p>}

      {!joined && profile && (
        <div className="card">
          <h3>Dołącz do stołu</h3>
          {profile.role === 'dealer' ? (
            <button onClick={() => join(6, true)}>Usiądź jako krupier (miejsce 6)</button>
          ) : (
            <>
              <button onClick={() => join(0, false)}>Miejsce 0</button>
              <button onClick={() => join(1, false)}>Miejsce 1</button>
            </>
          )}
        </div>
      )}

      {gameState?.phase === 'betting' && mySeat() && !mySeat().bet && (
        <div className="card">
          <h3>Postaw zakład</h3>
          <input type="number" value={betAmount} onChange={(e) => setBetAmount(e.target.value)} min={1} />
          <button onClick={placeBet}>Obstaw</button>
        </div>
      )}

      <div className="table-layout card">
        <div className="dealer-area">
          <h3>Krupier</h3>
          <div>
            {(gameState?.dealerCards || []).map((c, i) => (
              <CardView key={i} code={c} />
            ))}
          </div>
        </div>

        <div className="players-area">
          <h3>Gracze</h3>
          <div className="seat-row">
            {(gameState?.seats || tableState?.seats || [])
              .filter((s) => !s.dealer)
              .map((seat) => (
                <div
                  key={seat.seatIndex ?? seat.index}
                  className={`seat-box ${playerTurn?.seatIndex === (seat.seatIndex ?? seat.index) ? 'active-turn' : ''}`}
                >
                  <strong>{seat.username}</strong>
                  <div>
                    {(seat.cards || []).map((c, i) => (
                      <CardView key={i} code={c} />
                    ))}
                  </div>
                  {seat.handValue != null && <p>Wartość: {seat.handValue}</p>}
                  {seat.bet != null && <p>Zakład: {seat.bet}</p>}
                  {seat.handStatus && <p>{seat.handStatus}</p>}
                </div>
              ))}
          </div>
        </div>
      </div>

      {isMyTurn && myHand?.handId && (
        <div className="card">
          <h3>Twoja tura!</h3>
          <button onClick={() => sendHit(clientRef.current, tableId, myHand.handId)}>Hit</button>
          <button onClick={() => sendStand(clientRef.current, tableId, myHand.handId)}>Stand</button>
        </div>
      )}

      {lastResult && (
        <div className="card">
          <h3>Wynik rundy</h3>
          <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(lastResult.results || lastResult.payouts, null, 2)}</pre>
        </div>
      )}
    </div>
  )
}
