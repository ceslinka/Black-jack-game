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
| M4 | Dołączanie do stołu (REST + WebSocket) | MUST | ✅ |
| M5 | Wymóg krupiera przy stole | MUST | ✅ |
| M6 | Start gry (≥1 gracz + krupier) | MUST | ✅ |
| M7 | Obstawianie zakładów | MUST | ✅ |
| M8 | Logika Blackjack (hit / stand) | MUST | ✅ |
| M9 | Rozliczenie i wypłata (payout) | MUST | ✅ |
| M10 | UI stołu (karty, animacje, przerwa między rundami) | MUST | ✅ |
| S1 | Double down | SHOULD | ⬜ |
| S2 | Split | SHOULD | ⬜ |
| S3 | Swagger UI (`/api-docs`) | SHOULD | ⬜ |
| S4 | Rate limiting | SHOULD | ⬜ |
| S5 | Historia rund | SHOULD | ✅ |
| S6 | Konta krupierów | SHOULD | ✅ |

**Legenda statusu:** ⬜ — nie zrobione · 🟡 — w trakcie · ✅ — gotowe

## Ostatnie zmiany (przed push)

- **Kill switch** — po restarcie backendu (Maven) uszkodzona sesja stołu jest cicho resetowana: saldo wraca do ostatniego kamienia milowego (po poprawnie zakończonej grze), gracze są rozłączani ze stołu, stół wraca do `waiting`
- **Historia gier** — zakładka „Historia” w lobby: gracze widzą wyniki, saldo przed/po; krupier widzi zmiany przy stołach i liczbę rozegranych rund
- Animacje kart, przerwa 10 s między rundami, start od 1 gracza + krupier
- WebSocket + polling REST, naprawione SockJS i synchronizacja

Szczegóły: [CHANGELOG.md](CHANGELOG.md)

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

Stoły mają **do 2 miejsc dla graczy** + krupier. Minimum do gry: **1 gracz + krupier** (drugi gracz opcjonalny).

### Krok 1 — Uruchom wszystko

1. `docker compose up -d`
2. Backend: `mvn spring-boot:run` w `Black-jack/backend`
3. Frontend: `npm run dev` w `Black-jack/frontend`

### Krok 2 — Utwórz konta

| Konto | Rola | Jak utworzyć |
|-------|------|--------------|
| Gracz | player | Rejestracja na http://localhost:5173 |
| Krupier | dealer | Rejestracja z zaznaczonym „Konto krupiera” |

Opcjonalnie drugi gracz (inna przeglądarka / incognito).

Hasło: min. 8 znaków, np. `Secret1!`

### Krok 3 — Dołącz do stołu

1. Gracz → **Lobby** → **Wejdź** na stół (np. „Stół VIP”) → **Miejsce 0**
2. Krupier → ten sam stół → **Usiądź jako krupier**

Gdy jest ≥1 gracz + krupier → startuje runda.

### Krok 4 — Obstawianie i gra

Gracz wybiera kwotę i klika **Obstaw**. Po rozdaniu kart — **Hit** / **Stand**. Krupier dobiera animacyjnie do 17.

### Krok 5 — Koniec rundy

Podświetlenie zwycięzcy (Ty lub krupier), potem **przerwa 10 s** z wyborem zakładu na następną rundę.

## Dokumentacja

| Dokument | Opis |
|----------|------|
| [Plan projektu](docs/PROJECT_PLAN.md) | Specyfikacja MVP |
| [OpenAPI](openapi/openapi.yaml) | REST API |
| [CHANGELOG](CHANGELOG.md) | Historia zmian |
