# Backlog — plan na 4 tygodnie

## Tydzień 1 — Dokumentacja i fundament

| Zadanie | Rola | h | Done |
|---------|------|---|------|
| Finalizacja OpenAPI + diagramów | Dokumentacja | 8 | Wszystkie pliki w `docs/` i `openapi/` w repo |
| Setup projektu Spring Boot + PostgreSQL | Backend / DevOps | 10 | Aplikacja startuje lokalnie |
| Migracje DB V001 | Backend | 6 | Tabele utworzone |
| Auth: register / login JWT | Backend | 12 | Testy 201/401, token ważny |
| README + CONTRIBUTING | Dokumentacja | 4 | README kompletny |

**Suma:** ~40h

---

## Tydzień 2 — Lobby i WebSocket

| Zadanie | Rola | h | Done |
|---------|------|---|------|
| CRUD stołów + GET lobby | Backend | 8 | Zgodne z OpenAPI |
| WebSocket gateway + auth handshake | Backend | 12 | Połączenie z JWT |
| table.join / table.leave / table.state | Backend | 10 | Eventy zgodne z planem |
| UI lobby (lista stołów) | Frontend | 10 | Renderuje stoły z API |
| Testy WS (join, pełny stół) | Tester | 4 | 5 przypadków |

**Suma:** ~44h

---

## Tydzień 3 — Gra i portfel

| Zadanie | Rola | h | Done |
|---------|------|---|------|
| Wallet service + bet.place | Backend | 10 | Transakcja atomowa |
| Game Engine: deal, hit, stand | Backend | 16 | Testy logiki kart |
| dealerPlay + settle + payout | Backend | 10 | Wypłaty 3:2 i 1:1 |
| UI stołu: karty, Hit/Stand | Frontend | 12 | Sync z WS < 500ms |

**Suma:** ~48h

---

## Tydzień 4 — Jakość i bezpieczeństwo

| Zadanie | Rola | h | Done |
|---------|------|---|------|
| Rate limiting REST + WS | Backend | 6 | 429 przy przekroczeniu |
| Swagger UI `/api-docs` | Dokumentacja | 4 | Wszystkie endpointy MUST |
| CI GitHub Actions | DevOps | 8 | Lint + test + build green |
| Testy e2e: pełna runda | Tester | 10 | Rejestracja → payout |
| Protokół spotkania + bugfix | Wszyscy | 8 | — |

**Suma:** ~36h

---

## Podział ról w zespole

| Osoba | Sugerowana rola |
|-------|-----------------|
| Amelia Szymańska | — |
| Rozalia Mitkowska | — |
| Jcob Czajka | — |
| Piotr Jozefczyk | — |

*Role do ustalenia na spotkaniu organizacyjnym — wpisać w `docs/meetings/ORG_MEETING_01.md`.*

---

## Definition of Done (ogólna)

- [ ] Kod zreviewowany przez drugą osobę z zespołu
- [ ] Testy przechodzą lokalnie
- [ ] OpenAPI zaktualizowane (jeśli zmiana API)
- [ ] Status MVP w README zaktualizowany
- [ ] Brak secretów w commicie
