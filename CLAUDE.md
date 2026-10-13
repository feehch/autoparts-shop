# CLAUDE.md

Курсовая работа: «Информационная система интернет-магазина автозапчастей легковых автомобилей».
Стек: Spring Boot 3.5 (Java 17, Clean Architecture) + Angular 20 + PostgreSQL 16 + Redis 7, всё в Docker Compose.
Язык интерфейса, документации, комментариев в коде и сообщений об ошибках — **русский**.
Магазин белорусский: валюта — **BYN** (пайп `byn`, формат «1 234,50 Br»), телефоны — **только +375**
(домен: `PhoneNumber.normalize`, фронт: `shared/utils/phone.ts` + директива `appPhoneMask`), адреса в примерах — белорусские.

## Команды

```bash
docker compose up -d --build            # весь стек: http://localhost:4200, API :8080
docker compose up -d --build backend    # пересобрать только бэкенд
./scripts/smoke-test.sh                 # сквозная проверка API на запущенном стеке (curl + jq)
cd frontend && npx playwright test      # e2e всего сайта (системный Chrome), скриншоты: frontend/e2e-results/screens

cd backend && mvn test                  # модульные тесты (domain, application)
cd backend && mvn -DskipTests package   # jar: bootstrap/target/autoparts-backend.jar

cd frontend && npm run build            # production-сборка
cd frontend && npm start                # dev-сервер, /api проксируется на :8080
```

## Архитектура бэкенда (важно соблюдать)

Модули Maven = слои. Зависимости только внутрь: `bootstrap → presentation, infrastructure → application → domain`.

* `domain` — чистая Java, **никаких** Spring/JPA/Lombok. Бизнес-правила живут в сущностях (`Order.changeStatus`,
  `Product.reserve`, `Role.impliedRoles`). Нарушение правила → `DomainException` / `ConflictException` / `NotFoundException`.
* `application` — чистая Java. Сценарии (`*Service`) + порты в `application.port.out`. Изменяющие операции
  оборачиваются в `unitOfWork.inTransaction(...)`. Возвращают record-представления `*View`.
  Новые сценарии регистрируются как бины в `bootstrap/.../UseCaseConfig.java`.
* `infrastructure` — адаптеры портов: JPA-сущности `*Entity` (Lombok разрешён только здесь), `*RepositoryAdapter`
  с `@Cacheable/@CacheEvict` (имена кэшей — `CacheNames`), JWT, Redis, `SecurityConfig`, миграции Flyway.
* `presentation` — контроллеры, request-DTO с Bean Validation (`dto/*Requests`, метод `toCommand()`),
  `GlobalExceptionHandler` (формат `ApiError`). Роли проверяются и в `SecurityConfig` (URL), и `@PreAuthorize`.

Схема БД меняется **только новой миграцией** `backend/infrastructure/src/main/resources/db/migration/V<N>__*.sql`
(Hibernate в режиме `validate`). Уже применённые миграции не редактировать.

## Контракт API

`docs/api.md` — источник истины для фронтенда и бэкенда. При изменении эндпоинта или DTO обновить одновременно:
контроллер/DTO, `frontend/src/app/core/models/api.models.ts` + сервис, `docs/api.md`, при необходимости `scripts/smoke-test.sh`.

## Роли

`CUSTOMER` < `MANAGER` < `ADMIN` (иерархия). Демо-пользователи создаются `DemoUsersSeeder`:
`admin@autoparts.local / Admin123!`, `manager@autoparts.local / Manager123!`, `customer@autoparts.local / Customer123!`.

## Особенности окружения разработчика

* На хосте порт 5432 занят локальным PostgreSQL — Postgres из Docker проброшен на **5433**.
* `~/.npm` принадлежит root: `npm install --cache <временный каталог>`.
* Локально JDK 25, проект компилируется с `release 17`; в Docker — Temurin 17.
* Docker Desktop иногда нужно запустить вручную: `open -a Docker`.

## Документация

`README.md` и `docs/` (architecture, security, api, database, deployment, user-guide) поддерживать в актуальном
состоянии при изменении функциональности.
