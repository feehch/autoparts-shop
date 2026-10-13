---
description: Добавить или изменить эндпоинт REST API согласованно на бэкенде и фронтенде
argument-hint: <описание эндпоинта>
---
Изменение API: $ARGUMENTS

Соблюдай Clean Architecture из CLAUDE.md:
1. Доменные правила — в `domain`, сценарий — в `application` (`*Service`, порты в `port.out`), бин — в `UseCaseConfig`.
2. Контроллер и request-DTO с валидацией — в `presentation`; права — в `SecurityConfig` и `@PreAuthorize`.
3. Обнови `docs/api.md`, затем `frontend/src/app/core/models/api.models.ts` и соответствующий сервис/страницу.
4. Добавь модульный тест сценария и, если уместно, проверку в `scripts/smoke-test.sh`.
5. Проверь сборку: `cd backend && mvn -q test`, `cd frontend && npm run build`.
