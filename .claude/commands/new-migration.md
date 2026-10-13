---
description: Создать новую миграцию Flyway для изменения схемы БД
argument-hint: <краткое описание изменения>
---
Нужно изменить схему БД: $ARGUMENTS

1. Найди максимальный номер в `backend/infrastructure/src/main/resources/db/migration/` и создай `V<N+1>__<snake_case_описание>.sql`.
   Существующие миграции не редактируй.
2. Синхронно обнови JPA-сущность в `infrastructure/persistence/entity` (Hibernate работает в режиме `validate`),
   адаптер репозитория, доменную модель и при необходимости `docs/database.md` (ER-диаграмма, описание таблиц).
3. Проверь: `cd backend && mvn -q -DskipTests package`, затем `docker compose up -d --build backend` и `./scripts/smoke-test.sh`.
