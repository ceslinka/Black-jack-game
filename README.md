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
| Backend | Java + Spring Boot |
| Baza danych | PostgreSQL |
| Frontend | _TBA_ |
| API docs | Swagger / OpenAPI |
| Real-time | WebSocket |

## Dokumentacja

| Dokument | Opis |
|----------|------|
| [Plan projektu](docs/PROJECT_PLAN.md) | Pełna specyfikacja, MVP, architektura, bezpieczeństwo |
| [Schemat bazy](docs/DATABASE_SCHEMA.md) | Tabele, SQL, diagram ERD |
| [Backlog](docs/BACKLOG.md) | Plan prac na 4 tygodnie |
| [OpenAPI](openapi/openapi.yaml) | Specyfikacja REST API |
| [Diagramy](docs/diagrams/) | Architektura, flow gry, WebSocket |
| [CONTRIBUTING](CONTRIBUTING.md) | Zasady pracy w repozytorium |
| [CHANGELOG](CHANGELOG.md) | Historia zmian |

## Struktura repozytorium

```
Black-jack-game/
├── README.md
├── CONTRIBUTING.md
├── CHANGELOG.md
├── openapi/openapi.yaml
├── docs/
│   ├── PROJECT_PLAN.md
│   ├── DATABASE_SCHEMA.md
│   ├── BACKLOG.md
│   ├── diagrams/
│   └── meetings/
├── Black-jack/backend/     # Java — model kart
└── .github/ISSUE_TEMPLATE/
```

## Status MVP

| ID | Funkcja | Priorytet | Status |
|:---|:--------|:---------:|:------:|
| M1 | Rejestracja i logowanie (JWT) | MUST | ⬜ |
| M2 | Portfel użytkownika (start: 1000 żetonów) | MUST | ⬜ |
| M3 | Lobby ze stołami | MUST | ⬜ |
| M4 | Dołączanie do stołu (WebSocket) | MUST | ⬜ |
| M5 | Wymóg krupiera przy stole | MUST | ⬜ |
| M6 | Start gry po zapełnieniu stołu | MUST | ⬜ |
| M7 | Obstawianie zakładów | MUST | ⬜ |
| M8 | Logika Blackjack (hit / stand) | MUST | ⬜ |
| M9 | Rozliczenie i wypłata (payout) | MUST | ⬜ |
| M10 | UI stołu (karty, przyciski) | MUST | ⬜ |
| S1 | Double down | SHOULD | ⬜ |
| S2 | Split | SHOULD | ⬜ |
| S3 | Swagger UI (`/api-docs`) | SHOULD | ⬜ |
| S4 | Rate limiting | SHOULD | ⬜ |
| S5 | Historia rund | SHOULD | ⬜ |
| S6 | Konta krupierów | SHOULD | ⬜ |

**Legenda statusu:** ⬜ — nie zrobione · 🟡 — w trakcie · ✅ — gotowe

## Uruchomienie lokalne

_Instructie zostaną uzupełnione po skonfigurowaniu Spring Boot i PostgreSQL (tydzień 1 backlogu)._
