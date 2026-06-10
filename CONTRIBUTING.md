# Wkład w projekt

## Workflow Git

- Branch: `feature/<krótki-opis>` (np. `feature/auth-jwt`)
- Commit: `feat(scope): opis` lub `fix(scope): opis`
- Pull Request wymaga: 1 review od członka zespołu, przejście testów
- Merge do `main` po akceptacji

## Konwencje kodu

- Backend: Java 21+, Spring Boot, pakiet `org.example`
- Frontend: React + TypeScript (gdy powstanie)
- Formatowanie zgodne z ustawieniami IDE zespołu
- Komentarze tylko przy nietrywialnej logice biznesowej

## Dokumentacja

Przy każdej zmianie API:

1. Zaktualizować `openapi/openapi.yaml`
2. Zaktualizować status w `README.md` (tabela MVP)
3. Dodać wpis do `CHANGELOG.md`

## Testy

- Nowa funkcjonalność MUST → test jednostkowy lub integracyjny obowiązkowy
- Uruchomienie testów przed każdym PR

## Review checklist

- [ ] Walidacja danych wejściowych po stronie serwera
- [ ] Autoryzacja (JWT) na chronionych endpointach
- [ ] Brak hardcoded secrets
- [ ] Zgodność z OpenAPI