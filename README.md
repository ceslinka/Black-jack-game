# Black-jack-game

Symulator gry Blackjack w formie kasyna on-line w czasie rzeczywistym.

## Zespół

| Osoba |
|-------|
| Amelia Szymańska |
| Rozalia Mitkowska |
| Jcob Czajka |
| Piotr Jozefczyk |

## Stack

| Warstwa | Technologia |
|---------|-------------|
| Backend | Java 21 + Spring Boot 3 |
| Baza danych | PostgreSQL 16 |
| Frontend | React + Vite |
| Real-time | WebSocket (STOMP + SockJS) |
| API docs | OpenAPI (`openapi/openapi.yaml`) |

## Status MVP

| ID | Funkcja | Priorytet | Status |
|:---|:--------|:---------:|:------:|
| M1 | Rejestracja i logowanie (JWT) | MUST | ✅ |
| M2 | Portfel użytkownika (start: 1000 żetonów) | MUST | ✅ |
| M3 | Lobby ze stołami | MUST | ✅ |
| M4 | Dołączanie do stołu (WebSocket) | MUST | ✅ |
| M5 | Wymóg krupiera przy stole | MUST | ✅ |
| M6 | Start gry po zapełnieniu stołu | MUST | ✅ |
| M7 | Obstawianie zakładów | MUST | ✅ |
| M8 | Logika Blackjack (hit / stand) | MUST | ✅ |
| M9 | Rozliczenie i wypłata (payout) | MUST | ✅ |
| M10 | UI stołu (karty, przyciski) | MUST | ✅ |
| S1 | Double down | SHOULD | ⬜ |
| S2 | Split | SHOULD | ⬜ |
| S3 | Swagger UI (`/api-docs`) | SHOULD | ⬜ |
| S4 | Rate limiting | SHOULD | ⬜ |
| S5 | Historia rund | SHOULD | ⬜ |
| S6 | Konta krupierów | SHOULD | ✅ |

**Legenda statusu:** ⬜ — nie zrobione · 🟡 — w trakcie · ✅ — gotowe

## Struktura repozytorium

```
Black-jack-game/
├── docker-compose.yml
├── openapi/openapi.yaml
├── docs/
├── Black-jack/
│   ├── backend/     # Spring Boot
│   └── frontend/    # React
└── README.md
```

## Uruchomienie lokalne

**Wymagania:** Java 21+, Node.js 18+, Docker Desktop

### 1. Baza danych

```bash
docker compose up -d
```

### 2. Backend

```bash
cd Black-jack/backend
mvn spring-boot:run
```

Albo w IntelliJ: uruchomić klasę `org.example.BlackjackApplication`.

- API: http://localhost:8080/api/v1
- WebSocket: http://localhost:8080/ws

### 3. Frontend

```bash
cd Black-jack/frontend
npm install
npm run dev
```

- Aplikacja: http://localhost:5173

### Testy backendu

```bash
cd Black-jack/backend
mvn test
```

## Jak przetestować grę (krok po kroku)

Stoły mają **2 miejsca dla graczy** + krupier. Do pełnej gry potrzeba **3 kont** (2 graczy + 1 krupier).

### Krok 1 — Uruchom wszystko

1. `docker compose up -d`
2. Backend: `mvn spring-boot:run` w `Black-jack/backend`
3. Frontend: `npm run dev` w `Black-jack/frontend`

### Krok 2 — Utwórz konta (3 okna przeglądarki / tryb incognito)

| Konto | Rola | Jak utworzyć |
|-------|------|--------------|
| Gracz 1 | player | Rejestracja na http://localhost:5173 |
| Gracz 2 | player | Rejestracja (inna przeglądarka/incognito) |
| Krupier | dealer | Rejestracja z zaznaczonym „Konto krupiera” |

Hasło: min. 8 znaków, np. `Secret1!`

### Krok 3 — Dołącz do tego samego stołu

1. Każde konto → **Lobby** → **Wejdź** na ten sam stół (np. „Stół VIP”)
2. Gracz 1 → **Miejsce 0**
3. Gracz 2 → **Miejsce 1**
4. Krupier → **Usiądź jako krupier**

Gdy stół pełny + krupier → automatycznie startuje runda (`round.started`).

### Krok 4 — Obstawianie

Każdy gracz wpisuje kwotę (np. `10`) i klika **Obstaw**.  
Limit: min. zakład stołu ≤ kwota ≤ max. zakład i ≤ 25% salda.

Gdy obaj postawią → rozdawane są karty.

### Krok 5 — Gra

Gracz z aktywną turą (podświetlone miejsce) klika **Hit** lub **Stand**.  
Po turze wszystkich graczy krupier dobiera do 17.

### Krok 6 — Wynik

Na dole stołu pojawia się **Wynik rundy** (win/lose/push + payout).  
Saldo żetonów aktualizuje się automatycznie.

## Dokumentacja

| Dokument | Opis |
|----------|------|
| [Plan projektu](docs/PROJECT_PLAN.md) | Specyfikacja MVP |
| [OpenAPI](openapi/openapi.yaml) | REST API |
| [CHANGELOG](CHANGELOG.md) | Historia zmian |
