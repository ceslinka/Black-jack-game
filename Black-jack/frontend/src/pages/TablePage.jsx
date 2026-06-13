import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, getUserId } from '../api'
import { createStompClient, sendHit, sendStand, subscribeErrors, subscribeTable } from '../ws'
import CardView from '../components/CardView'
import IntermissionOverlay from '../components/IntermissionOverlay'
import RoundResults from '../components/RoundResults'

function isMyUserId(seatUserId) {
  return seatUserId && String(seatUserId) === String(getUserId())
}

function seatIndexOf(seat) {
  return seat?.seatIndex ?? seat?.index
}

const HAND_STATUS_LABELS = {
  active: 'W grze',
  stand: 'Pas',
  bust: 'Fura',
  blackjack: 'Blackjack!',
}

export default function TablePage() {
  const { tableId } = useParams()
  const navigate = useNavigate()
  const clientRef = useRef(null)
  const wasSeatedRef = useRef(false)
  const autoBetRoundRef = useRef(null)
  const [wallet, setWallet] = useState(null)
  const [tableState, setTableState] = useState(null)
  const [gameState, setGameState] = useState(null)
  const [playerTurn, setPlayerTurn] = useState(null)
  const [roundSummary, setRoundSummary] = useState(null)
  const [selectedBet, setSelectedBet] = useState(10)
  const [error, setError] = useState('')
  const [wsConnected, setWsConnected] = useState(false)
  const [joining, setJoining] = useState(false)
  const [leaving, setLeaving] = useState(false)
  const [mySeatInfo, setMySeatInfo] = useState(null)
  const [intermissionEndsAt, setIntermissionEndsAt] = useState(null)
  const [actionPending, setActionPending] = useState(false)
  const [statusText, setStatusText] = useState('')

  const failCountRef = useRef(0)

  const refreshAll = useCallback(async () => {
    try {
      const [state, seat, gState] = await Promise.all([
        api.getTableState(tableId),
        api.mySeat(),
        api.getGameState(tableId),
      ])
      failCountRef.current = 0
      setError('')
      setTableState(state)
      setIntermissionEndsAt(state.intermissionEndsAt ?? null)
      if (gState) {
        setGameState(gState)
        if (gState.phase === 'dealer_turn' || gState.phase === 'settled') {
          setPlayerTurn(null)
        }
      } else if (state.status === 'waiting') {
        setGameState(null)
        setRoundSummary(null)
      }
      if (seat && String(seat.tableId) === String(tableId)) {
        setMySeatInfo(seat)
      } else {
        setMySeatInfo(null)
      }
    } catch (err) {
      failCountRef.current += 1
      if (failCountRef.current >= 2) {
        setError(
          err.message === 'Failed to fetch'
            ? 'Brak połączenia z backendem — uruchom Spring Boot na porcie 8080'
            : err.message,
        )
      }
    }
  }, [tableId])

  const handleEvent = useCallback(
    (event) => {
      switch (event.type) {
        case 'table.state':
          setTableState(event)
          setIntermissionEndsAt(event.intermissionEndsAt ?? null)
          break
        case 'game.state':
          setGameState(event)
          if (event.phase === 'dealer_turn') {
            setPlayerTurn(null)
            setStatusText('Tura krupiera…')
          } else if (event.phase === 'player_turn') {
            setStatusText('')
            setActionPending(false)
          } else if (event.phase === 'settled') {
            setPlayerTurn(null)
            setStatusText('')
          }
          break
        case 'round.intermission':
          setIntermissionEndsAt(event.endsAt)
          break
        case 'player.turn':
          setPlayerTurn(event)
          setActionPending(false)
          setStatusText('')
          break
        case 'dealer.turn':
          setPlayerTurn(null)
          setActionPending(false)
          setStatusText('Tura krupiera…')
          setGameState((prev) =>
            prev
              ? { ...prev, dealerCards: event.cards, dealerHidden: false, phase: 'dealer_turn' }
              : prev,
          )
          break
        case 'card.dealt':
          if (event.target?.startsWith('player-')) {
            const seatIdx = Number(event.target.replace('player-', ''))
            setGameState((prev) => {
              if (!prev?.seats) return prev
              return {
                ...prev,
                seats: prev.seats.map((s) =>
                  seatIndexOf(s) === seatIdx ? { ...s, cards: event.cards } : s,
                ),
              }
            })
            setActionPending(false)
          } else if (event.target === 'dealer' || event.target?.startsWith('dealer')) {
            setGameState((prev) =>
              prev ? { ...prev, dealerCards: event.cards, dealerHidden: event.hidden ?? false } : prev,
            )
          }
          break
        case 'round.started':
          setPlayerTurn(null)
          setIntermissionEndsAt(null)
          setRoundSummary(null)
          setStatusText('')
          break
        case 'round.result':
        case 'round.settled':
          setRoundSummary(event)
          setPlayerTurn(null)
          setActionPending(false)
          setStatusText('')
          api.wallet().then(setWallet)
          break
        case 'bet.placed':
          api.wallet().then(setWallet)
          break
        default:
          break
      }
    },
    [],
  )

  useEffect(() => {
    api.wallet().then(setWallet).catch(() => {})
    refreshAll()

    const poll = setInterval(refreshAll, 2000)

    const client = createStompClient({
      onConnect: (c) => {
        clientRef.current = c
        setWsConnected(true)
        setError('')
        subscribeTable(c, tableId, handleEvent)
        subscribeErrors(c, (e) => {
          setError(e.message)
          setActionPending(false)
        })
      },
      onDisconnect: () => setWsConnected(false),
      onError: () => setWsConnected(false),
    })

    return () => {
      clearInterval(poll)
      client.deactivate()
      clientRef.current = null
    }
  }, [tableId, handleEvent, refreshAll])

  useEffect(() => {
    if (!actionPending) return undefined
    const timeout = setTimeout(() => setActionPending(false), 8000)
    return () => clearTimeout(timeout)
  }, [actionPending])

  useEffect(() => {
    if (mySeatInfo && String(mySeatInfo.tableId) === String(tableId)) {
      wasSeatedRef.current = true
    } else if (wasSeatedRef.current && tableState?.status === 'waiting') {
      wasSeatedRef.current = false
      navigate('/lobby', { replace: true })
    }
  }, [mySeatInfo, tableId, tableState?.status, navigate])

  useEffect(() => {
    if (tableState?.minBet) {
      setSelectedBet((prev) => Math.max(tableState.minBet, prev))
    }
  }, [tableState?.minBet])

  const tableSeats = tableState?.seats || []
  const occupiedIndexes = new Set(tableSeats.map(seatIndexOf))
  const displayGame = gameState
  const playerSeats = displayGame?.seats?.length ? displayGame.seats : tableSeats
  const alreadySeated =
    (mySeatInfo && String(mySeatInfo.tableId) === String(tableId)) ||
    playerSeats.some((s) => isMyUserId(s.userId))

  const tableStatus = tableState?.status || 'waiting'
  const intermissionReady = tableStatus === 'settlement' && intermissionEndsAt
  const tableOpen = tableStatus === 'waiting' || tableStatus === 'settlement'
  const bettingPhase = displayGame?.phase === 'betting'
  const canLeave = tableOpen || bettingPhase

  const minBet = tableState?.minBet ?? 10
  const maxBet = tableState?.maxBet ?? 500
  const dealerCards = displayGame?.dealerCards || []

  function mySeat() {
    return displayGame?.seats?.find((s) => isMyUserId(s.userId))
  }

  const myHand = mySeat()

  useEffect(() => {
    if (gameState?.phase !== 'betting' || !gameState?.roundId) return
    if (autoBetRoundRef.current === gameState.roundId) return
    if (!myHand || myHand.bet) return
    if (!selectedBet || selectedBet < minBet) return

    autoBetRoundRef.current = gameState.roundId
    api
      .placeBet(tableId, selectedBet)
      .then((res) => {
        setWallet({ balance: res.newBalance, currency: 'chips' })
        return refreshAll()
      })
      .catch((err) => {
        autoBetRoundRef.current = null
        setError(err.message)
      })
  }, [gameState, myHand, selectedBet, minBet, tableId, refreshAll])

  async function join(seatIndex) {
    setError('')
    setJoining(true)
    try {
      await api.joinTable(tableId, seatIndex)
      await refreshAll()
    } catch (err) {
      setError(err.message)
    } finally {
      setJoining(false)
    }
  }

  async function leave() {
    setError('')
    setLeaving(true)
    try {
      await api.leaveTable(tableId)
      setTableState(null)
      setMySeatInfo(null)
      setGameState(null)
      setPlayerTurn(null)
      setIntermissionEndsAt(null)
      setRoundSummary(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setLeaving(false)
    }
  }

  async function placeBetManual() {
    setError('')
    try {
      const res = await api.placeBet(tableId, Number(selectedBet))
      setWallet({ balance: res.newBalance, currency: 'chips' })
      await refreshAll()
    } catch (err) {
      setError(err.message)
    }
  }

  const isMyTurn =
    playerTurn?.userId &&
    isMyUserId(playerTurn.userId) &&
    !actionPending &&
    displayGame?.phase === 'player_turn'

  function handleHit() {
    if (!myHand?.handId || actionPending) return
    setActionPending(true)
    setStatusText('Dobierasz kartę…')
    try {
      sendHit(clientRef.current, tableId, myHand.handId)
    } catch (err) {
      setActionPending(false)
      setStatusText('')
      setError(err.message)
    }
  }

  function handleStand() {
    if (!myHand?.handId || actionPending) return
    setActionPending(true)
    setStatusText('Pas — czekamy na krupiera…')
    try {
      sendStand(clientRef.current, tableId, myHand.handId)
    } catch (err) {
      setActionPending(false)
      setStatusText('')
      setError(err.message)
    }
  }

  const seatIndex =
    mySeatInfo?.seatIndex ??
    playerSeats.find((s) => isMyUserId(s.userId))?.seatIndex ??
    seatIndexOf(playerSeats.find((s) => isMyUserId(s.userId)))

  const playerCount = tableState?.occupiedPlayerSeats ?? playerSeats.length

  const myResultRow = (roundSummary?.results || roundSummary?.payouts || []).find((r) =>
    isMyUserId(r.userId),
  )
  const dealerResultValue = myResultRow?.dealerValue ?? roundSummary?.results?.[0]?.dealerValue

  return (
    <div className="container">
      <div className="header-bar">
        <Link to="/lobby">← Lobby</Link>
        <span className="badge">{wallet?.balance ?? '…'} żetonów</span>
      </div>

      <h1>Stół</h1>
      <p>
        <small>
          WebSocket: {wsConnected ? '✅ połączony' : '⏳ łączenie…'}
          {alreadySeated && ' · Jesteś przy stole'}
          {!tableOpen && displayGame && ` · Gra: ${displayGame.phase}`}
          {intermissionReady && ' · Przerwa między rundami'}
        </small>
      </p>
      {statusText && <p className="status-banner">{statusText}</p>}
      {error && <p className="error">{error}</p>}

      {!alreadySeated && tableOpen && (
        <div className="card">
          <h3>Dołącz do stołu</h3>
          {[0, 1].map((idx) => (
            <button key={idx} disabled={joining || occupiedIndexes.has(idx)} onClick={() => join(idx)}>
              {joining ? 'Dołączanie…' : occupiedIndexes.has(idx) ? `Miejsce ${idx} — zajęte` : `Miejsce ${idx}`}
            </button>
          ))}
        </div>
      )}

      {!alreadySeated && tableStatus === 'in_game' && (
        <p><small>Stół w trakcie rozdania — dołączysz po zakończeniu rundy.</small></p>
      )}

      {alreadySeated && !intermissionReady && (
        <div className="card">
          <p>
            Twoje miejsce: <strong>{seatIndex ?? '?'}</strong>
          </p>
          {canLeave ? (
            <button className="secondary" disabled={leaving} onClick={leave}>
              {leaving ? 'Wychodzenie…' : 'Opuść stół'}
            </button>
          ) : (
            <p><small>Nie można opuścić stołu w trakcie rozdania.</small></p>
          )}
        </div>
      )}

      {bettingPhase && myHand && !myHand.bet && !intermissionReady && (
        <div className="card">
          <h3>Postaw zakład</h3>
          <input type="number" value={selectedBet} onChange={(e) => setSelectedBet(Number(e.target.value))} min={minBet} />
          <button onClick={placeBetManual}>Obstaw</button>
        </div>
      )}

      {bettingPhase && myHand?.bet && (
        <p><small>Twój zakład: {myHand.bet} żetonów — czekamy na pozostałych graczy…</small></p>
      )}

      {!tableOpen && tableStatus === 'in_game' && !displayGame && (
        <p><small>Ładowanie stanu gry…</small></p>
      )}

      <div className="table-layout card">
        <div className="dealer-area">
          <h3>Krupier</h3>
          <div className="card-hand">
            {dealerCards.map((c, i) => (
              <CardView key={`d-${i}-${c}`} code={c} />
            ))}
          </div>
        </div>

        <div className="players-area">
          <h3>Gracze ({playerCount})</h3>
          <div className="seat-row">
            {playerSeats.map((seat) => {
              const idx = seatIndexOf(seat)
              const cards = seat.cards || []
              const isMe = isMyUserId(seat.userId)
              const isWinner = roundSummary && myResultRow?.outcome === 'win' && isMe
              const isLoser = roundSummary && myResultRow?.outcome === 'lose' && isMe
              const isPush = roundSummary && myResultRow?.outcome === 'push' && isMe

              return (
                <div
                  key={idx}
                  className={`seat-box ${playerTurn?.seatIndex === idx ? 'active-turn' : ''} ${isWinner ? 'winner-highlight' : ''} ${isPush ? 'push-highlight' : ''}`}
                >
                  {isWinner && <p className="winner-badge">Zwycięzca</p>}
                  {isPush && <p className="push-badge">Remis</p>}
                  {isLoser && <p className="push-badge">Przegrana</p>}
                  <strong>
                    {seat.username}
                    {isMe ? ' (Ty)' : ''}
                  </strong>
                  <div className="card-hand">
                    {cards.map((c, i) => (
                      <CardView key={`${idx}-${i}-${c}`} code={c} />
                    ))}
                  </div>
                  {seat.handValue != null && <p>Wartość: {seat.handValue}</p>}
                  {seat.bet != null && <p>Zakład: {seat.bet}</p>}
                  {seat.handStatus && (
                    <p>{HAND_STATUS_LABELS[seat.handStatus] ?? seat.handStatus}</p>
                  )}
                </div>
              )
            })}
            {playerCount === 0 && <p><em>Brak graczy przy stole</em></p>}
          </div>
        </div>
      </div>

      {isMyTurn && myHand?.handId && wsConnected && (
        <div className="card">
          <h3>Twoja tura!</h3>
          <button disabled={actionPending} onClick={handleHit}>Hit</button>
          <button disabled={actionPending} onClick={handleStand}>Stand</button>
        </div>
      )}

      {roundSummary && (
        <RoundResults
          results={roundSummary.results || roundSummary.payouts}
          seats={playerSeats}
          dealerValue={dealerResultValue}
        />
      )}

      {intermissionReady && alreadySeated && (
        <IntermissionOverlay
          endsAt={intermissionEndsAt}
          minBet={minBet}
          maxBet={maxBet}
          walletBalance={wallet?.balance}
          selectedBet={selectedBet}
          onSelectBet={setSelectedBet}
          onLeave={leave}
          leaving={leaving}
        />
      )}
    </div>
  )
}
