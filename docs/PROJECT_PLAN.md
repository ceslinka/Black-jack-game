# Plan projektu — Symulator kasyna (Blackjack)

## 1. Podsumowanie projektu

Projekt obejmuje implementację symulatora kasyna online z jedną grą — Blackjack — dostępnej przez przeglądarkę w czasie rzeczywistym. Celem jest dostarczenie MVP obejmującego lobby ze stołami, konta użytkowników z portfelem, mechanikę obstawiania oraz pełną rundę gry z krupierem i rozliczeniem środków. Projekt realizowany jest metodą documentation-first: najpierw specyfikacja, diagramy i API, potem implementacja iteracyjna.

---

## 2. MVP — funkcjonalności z priorytetami i kryteriami akceptacji

### MUST (M1–M10)

| ID | Funkcjonalność | Kryteria akceptacji |
|----|----------------|---------------------|
| M1 | Rejestracja i logowanie (JWT) | `POST /auth/register` zwraca `201` i `{userId, token}`; `POST /auth/login` zwraca `200` i JWT; token wygasa po 24h; hasło min. 8 znaków |
| M2 | Portfel użytkownika | Nowe konto otrzymuje `1000` żetonów; `GET /wallet` zwraca `{balance, currency}`; saldo ≥ 0 zawsze |
| M3 | Lobby ze stołami | `GET /tables` zwraca listę stołów z polami: `id, name, maxPlayers, currentPlayers, status, minBet, maxBet`; status ∈ `{waiting, in_game, settlement}` |
| M4 | Dołączanie do stołu (WebSocket) | Event `table.join` z `tableId` i `seatIndex`; serwer emituje `table.state` z aktualnymi miejscami; max 6 graczy + 1 krupier |
| M5 | Wymóg krupiera | Gra nie startuje bez co najmniej 1 użytkownika z rolą `dealer` przy stole; event `round.cannot_start` z `reason: "no_dealer"` |
| M6 | Start gry po zapełnieniu | Gdy `occupiedSeats == maxPlayers` i krupier obecny → status stołu `in_game`; event `round.started` z `roundId` |
| M7 | Obstawianie | `POST /tables/{id}/bets` z `{amount}`; walidacja: `minBet ≤ amount ≤ maxBet` i `amount ≤ balance * 0.25`; event `bet.placed` |
| M8 | Logika Blackjack (hit/stand) | Serwer rozdaje 2 karty każdemu; gracz może `hit` lub `stand`; krupier dobiera do 17; eventy `card.dealt`, `player.turn`, `dealer.turn`, `round.result` |
| M9 | Rozliczenie i payout | Blackjack = 3:2, wygrana = 1:1, remis = zwrot; rozliczenie serwerowe; event `round.settled` z `{payouts}` |
| M10 | UI stołu (frontend) | Widoczne karty, saldo, przyciski Hit/Stand aktywne tylko na własnej turze; odświeżenie stanu < 500 ms po evencie WS |

### SHOULD (S1–S6)

| ID | Funkcjonalność | Kryteria akceptacji |
|----|----------------|---------------------|
| S1 | Double down | Dostępne przy 2 kartach; stawka podwojona; jedna dodatkowa karta |
| S2 | Split | Para identycznych rang; 2 ręce, osobne zakłady |
| S3 | Swagger UI | `/api-docs` renderuje OpenAPI 3.0; wszystkie endpointy MUST udokumentowane |
| S4 | Rate limiting | REST: 100 req/min/IP; WS: max 30 msg/min/connection |
| S5 | Historia rund | `GET /tables/{id}/history` zwraca ostatnie 20 rund |
| S6 | Konta krupierów | Endpoint rejestracji dealera lub flaga `isDealer` w profilu |

### COULD (C1–C4)

| ID | Funkcjonalność |
|----|----------------|
| C1 | Chat przy stole |
| C2 | Wiele stołów równoległych |
| C3 | Staging deploy |
| C4 | Statystyki gracza |

---

## 3. Artefakty w repozytorium

| Plik / artefakt | Lokalizacja |
|-----------------|-------------|
| README.md | `/README.md` |
| Plan projektu | `/docs/PROJECT_PLAN.md` |
| OpenAPI | `/openapi/openapi.yaml` |
| Diagramy | `/docs/diagrams/*.mmd` |
| Schemat bazy | `/docs/DATABASE_SCHEMA.md` |
| Backlog | `/docs/BACKLOG.md` |
| Protokół spotkania | `/docs/meetings/ORG_MEETING_01.md` |

---

## 4. Architektura systemu

System składa się z frontendu SPA (React), backendu API (Java Spring Boot), warstwy WebSocket, bazy PostgreSQL oraz opcjonalnego Redis do sesji i rate limitingu. Centralny **Game Engine** zarządza stanem stołów i synchronizuje go z bazą przy rozliczeniach. REST API obsługuje operacje CRUD i transakcje portfela; WebSocket obsługuje zdarzenia gry w czasie rzeczywistym. Autoryzacja oparta o JWT.

### Komponenty

| Komponent | Technologia | Odpowiedzialność |
|-----------|-------------|------------------|
| Frontend SPA | React + Vite | Lobby, UI stołu, klient WS |
| REST API | Spring Boot | Auth, wallet, tables CRUD |
| WebSocket Gateway | Spring WebSocket / STOMP | Eventy gry, broadcast stanu |
| Game Engine | Moduł backendu | Logika Blackjack, FSM stołu |
| Auth Service | JWT + bcrypt | Rejestracja, logowanie, role |
| Wallet Service | PostgreSQL transactions | Zakłady, payout, saldo |
| Database | PostgreSQL 16 | Persystencja użytkowników, rund |
| Docs | Swagger UI + OpenAPI YAML | Dokumentacja REST |

Diagram: [`docs/diagrams/architecture.mmd`](diagrams/architecture.mmd)

---

## 5. API REST

Pełna specyfikacja: [`openapi/openapi.yaml`](../openapi/openapi.yaml)

| Metoda | Ścieżka | Opis | Statusy |
|--------|---------|------|---------|
| POST | `/auth/register` | Rejestracja gracza | 201, 400, 409 |
| POST | `/auth/login` | Logowanie | 200, 401 |
| POST | `/auth/register-dealer` | Rejestracja krupiera | 201, 400 |
| GET | `/users/me` | Profil użytkownika | 200, 401 |
| GET | `/wallet` | Saldo portfela | 200, 401 |
| GET | `/tables` | Lista stołów w lobby | 200 |
| POST | `/tables` | Utworzenie stołu | 201, 400, 401 |
| GET | `/tables/{id}` | Szczegóły stołu | 200, 404 |
| POST | `/tables/{id}/bets` | Postawienie zakładu | 201, 400, 403, 409 |
| GET | `/tables/{id}/history` | Historia rund | 200, 404 |
| POST | `/tables/{id}/join` | Dołączenie do stołu (REST fallback) | 200, 400, 403, 409 |
| DELETE | `/tables/{id}/leave` | Opuszczenie stołu | 200, 403 |
| GET | `/health` | Health check | 200 |

---

## 6. WebSocket — schema wiadomości

### Klient → serwer

| Event | Payload | Walidacja |
|-------|---------|-----------|
| `table.join` | `{tableId, seatIndex, asDealer}` | seatIndex 0–6, JWT w auth |
| `table.leave` | `{tableId}` | user przypisany do stołu |
| `bet.place` | `{tableId, amount}` | faza `betting`, limity salda |
| `action.hit` | `{tableId, handId}` | tura gracza |
| `action.stand` | `{tableId, handId}` | tura gracza |
| `action.double` | `{tableId, handId}` | 2 karty, saldo ≥ bet |
| `action.split` | `{tableId, handId}` | para, saldo ≥ bet |

### Serwer → klient

| Event | Opis |
|-------|------|
| `lobby.update` | Aktualizacja listy stołów |
| `table.state` | Pełny stan stołu |
| `round.cannot_start` | Brak warunków startu (np. brak krupiera) |
| `round.started` | Nowa runda |
| `bet.placed` | Zakład przyjęty |
| `card.dealt` | Karta rozdana |
| `player.turn` | Tura gracza |
| `dealer.turn` | Tura krupiera |
| `round.result` | Wyniki rundy |
| `round.settled` | Rozliczenie portfela |
| `error` | Błąd `{code, message}` |

Diagram sekwencji: [`docs/diagrams/websocket-round-sequence.mmd`](diagrams/websocket-round-sequence.mmd)

---

## 7. Model danych

Szczegóły: [`docs/DATABASE_SCHEMA.md`](DATABASE_SCHEMA.md)

Tabele: `users`, `tables`, `table_seats`, `rounds`, `bets`, `hands`, `transactions`.

---

## 8. Diagramy mechaniki gry

| Diagram | Plik |
|---------|------|
| Flow lobby → stół → runda | [`lobby-table-flow.mmd`](diagrams/lobby-table-flow.mmd) |
| Lifecycle stołu | [`table-lifecycle.mmd`](diagrams/table-lifecycle.mmd) |
| Logika Blackjack | [`blackjack-game-logic.mmd`](diagrams/blackjack-game-logic.mmd) |

---

## 9. Struktura repozytorium

```
Black-jack-game/
├── README.md
├── CONTRIBUTING.md
├── CHANGELOG.md
├── openapi/
│   └── openapi.yaml
├── docs/
│   ├── PROJECT_PLAN.md
│   ├── DATABASE_SCHEMA.md
│   ├── BACKLOG.md
│   ├── diagrams/
│   └── meetings/
├── Black-jack/
│   └── backend/          # Java — model kart (Card, Rank, Suit)
└── .github/
    └── ISSUE_TEMPLATE/
```

---

## 10. Backlog

Szczegóły: [`docs/BACKLOG.md`](BACKLOG.md)

---

## 11. Checklisty oceny

### Dla prowadzącego

- [ ] README.md kompletny
- [ ] OpenAPI YAML poprawny i zgodny z planem
- [ ] Min. 4 diagramy Mermaid w `docs/diagrams/`
- [ ] WebSocket: udokumentowane eventy
- [ ] Schemat bazy danych w `docs/DATABASE_SCHEMA.md`
- [ ] Protokół spotkania organizacyjnego
- [ ] Każdy członek zespołu rozumie przypisany moduł

### Dla zespołu

- [ ] Wszystkie MUST (M1–M10) — status w README
- [ ] Diagram architektury aktualny
- [ ] Edge-case'y opisane w planie testów
- [ ] Brak secretów w repo

---

## 12. Testy i CI

### Przypadki testowe

| # | Scenariusz | Oczekiwany wynik |
|---|------------|------------------|
| T1 | Rejestracja z hasłem 7 znaków | 400 |
| T2 | Login poprawny | 200 + JWT |
| T3 | Zakład = minBet | 201, saldo -= amount |
| T4 | Zakład > maxBet stołu | 400 |
| T5 | Zakład > 25% salda | 400 |
| T6 | Dwa szybkie zakłady (race) | Jeden 201, drugi 409 |
| T7 | Hit bez JWT | 401 / WS error |
| T8 | Hit na cudzej turze | WS error FORBIDDEN |
| T9 | Start gry bez krupiera | round.cannot_start |
| T10 | Gracz bust (>21) | outcome=lose |
| T11 | Remis z krupierem | zwrot zakładu |
| T12 | Blackjack gracza | payout = bet * 2.5 |
| T13 | Rate limit przekroczony | 429 |
| T14 | WebSocket bez tokena | Połączenie odrzucone |

---

## 13. Bezpieczeństwo i walidacja

| Zagrożenie | Rekomendacja |
|------------|--------------|
| Manipulacja zakładu | Weryfikacja salda z DB (`SELECT FOR UPDATE`) |
| Nieautoryzowana akcja | `userId` z JWT === `seat.userId` |
| Race condition przy zakładzie | Constraint `(round_id, user_id)` + transakcja |
| XSS w username | Regex `^[a-zA-Z0-9_]{3,30}$` |
| SQL injection | Parametryzowane zapytania |
| Flood WS | Max 30 msg/min/connection |
| Flood REST | 100 req/min/IP |

### Reguły walidacji zakładów

```
min_amount = table.min_bet (domyślnie 10)
max_amount = min(table.max_bet, user.balance)
max_percent = 0.25 * user.balance
effective_max = min(max_amount, max_percent)
amount: integer > 0
```

---

## 14. Ryzyka i plany awaryjne

| Ryzyko | Mitigacja |
|--------|-----------|
| Błędy walidacji zakładów | Testy + code review modułu wallet |
| Race conditions przy WS | Mutex per tableId; transakcje DB |
| Opóźnienia frontendu | Uproszczenie UI; wycięcie COULD |
| Zespół nie rozumie kodu | Pair programming; dokumentacja modułów |

**Plan awaryjny:** Wyciąć Split/Double (S1, S2), zachować MUST M1–M10.

---

## 15. Pytania kontrolne (przygotowanie do oceny)

1. Jaki jest centralny punkt informacyjny projektu?
2. Gdzie znajduje się specyfikacja REST API?
3. Jakie diagramy zostały przygotowane i co ilustrują?
4. Jaka jest różnica między REST API a WebSocket?
5. Jakie warunki muszą być spełnione, aby gra się rozpoczęła?
6. Jakie są reguły walidacji zakładu?
7. Jak zabezpieczono przed race condition przy obstawianiu?
8. Jaką wypłatę otrzymuje gracz z Blackjackiem?
9. Co się dzieje gdy przy stole nie ma krupiera?
10. Jakie statusy może przyjmować stół?
