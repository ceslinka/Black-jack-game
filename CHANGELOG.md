# Changelog

Wszystkie istotne zmiany w projekcie dokumentowane w tym pliku.

## [Unreleased]

### Added

- **TableRecoveryService** (kill switch): ciche odzyskiwanie stołu po przerwanym stanie (restart JVM)
- **Kamienie milowe salda** (`table_balance_milestones`) — snapshot po każdej poprawnie zakończonej rundzie
- **Historia gier** gracza i **historia zmian** krupiera (`GET /users/me/game-history`, `/dealer-history`)
- Frontend: strona `/history`, link w lobby
- Pełna implementacja MVP (M1–M10): auth JWT, portfel, lobby, stoły, gra Blackjack, payout
- WebSocket STOMP + SockJS (`/ws`) z synchronizacją stanu stołu w czasie rzeczywistym
- REST: `GET /tables/{id}/state`, `GET /tables/{id}/game-state`, join/leave przez HTTP
- Przerwa między rundami (10 s, status `settlement`) z wyborem wpisowego i auto-startem następnej rundy
- Animacje dobierania kart (gracz i krupier) oraz spersonalizowane podświetlenie zwycięzcy
- Gra od **1 gracza + krupier** (bez wymogu pełnego stołu)
- Konta krupiera (miejsce 6), walidacja miejsc, seed stołów VIP/Classic

### Fixed

- SockJS: endpoint `/ws` z `.withSockJS()` — WebSocket łączy się poprawnie przez Vite proxy
- Synchronizacja graczy bez WS: polling REST co 1–2 s
- Talia kart odtwarzana po restarcie backendu (brak „Deck exhausted” przy drugim zakładzie)
- Karty i wynik rundy widoczne do końca przerwy (stan `settled` w API)
- Podwójne rozdawanie kart — blokada pessimistic lock na rundzie

---

## [0.1.0] — 2026-06-10

### Added

- Dokumentacja początkowa projektu
- README z tabelą MVP
- Plan projektu (`docs/PROJECT_PLAN.md`)
- Schemat bazy danych (`docs/DATABASE_SCHEMA.md`)
- Backlog 4-tygodniowy (`docs/BACKLOG.md`)
- Specyfikacja OpenAPI (`openapi/openapi.yaml`)
- Diagramy Mermaid (`docs/diagrams/`)
- Modele kart Java: `Card`, `Rank`, `Suit`
- CONTRIBUTING.md, szablon issue, protokół spotkania
