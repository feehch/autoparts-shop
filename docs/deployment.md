# Развёртывание и разработка

## Требования

| Инструмент | Версия | Для чего |
|------------|--------|----------|
| Docker + Docker Compose v2 | Docker Desktop 4.x | Запуск всего стека |
| JDK | 17+ (проверено на 25) | Локальная сборка бэкенда |
| Maven | 3.9+ | Сборка бэкенда |
| Node.js / npm | 22 / 10 | Локальная разработка фронтенда |
| Angular CLI | 20 | `ng serve`, генерация кода |

## Запуск в Docker

```bash
cp .env.example .env        # необязательно — без .env используются значения по умолчанию
docker compose up -d --build
docker compose ps           # все сервисы должны быть в состоянии healthy / running
```

| Сервис | Образ | Порт на хосте | Адрес |
|--------|-------|---------------|-------|
| `frontend` | `autoparts-frontend` (nginx:alpine) | 4200 | http://localhost:4200 |
| `backend` | `autoparts-backend` (eclipse-temurin:17-jre) | 8080 | http://localhost:8080/swagger-ui.html |
| `postgres` | `postgres:16` | 5433 | `jdbc:postgresql://localhost:5433/autoparts` |
| `redis` | `redis:7` | 6379 | `redis://localhost:6379` |

Порядок запуска обеспечивается healthcheck-ами: бэкенд стартует после готовности PostgreSQL и Redis,
фронтенд — после готовности бэкенда (`/actuator/health`). При первом старте Flyway создаёт схему
и демонстрационный каталог, а приложение создаёт демо-пользователей.

Данные хранятся в именованных томах `autoparts_pgdata` и `autoparts_redisdata`.

```bash
docker compose logs -f backend      # логи бэкенда
docker compose down                 # остановить (данные сохраняются)
docker compose down -v              # остановить и удалить данные (БД будет создана заново)
docker compose up -d --build backend   # пересобрать только бэкенд
```

### Образы

* `backend/Dockerfile` — многоэтапная сборка: `maven:3.9-eclipse-temurin-17` собирает jar (слой зависимостей
  кэшируется отдельно от исходников), итоговый образ `eclipse-temurin:17-jre` запускается от непривилегированного пользователя.
* `frontend/Dockerfile` — `node:22-alpine` собирает Angular-приложение, `nginx:alpine` раздаёт статику и проксирует
  `/api`, `/swagger-ui`, `/v3/api-docs` на `backend:8080`.
* `postgres:16` и `redis:7` используются готовыми образами.

## Переменные окружения

| Переменная | По умолчанию | Описание |
|------------|--------------|----------|
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | `autoparts` | Параметры БД |
| `JWT_SECRET` | dev-значение | Секрет подписи JWT, ≥ 32 символов. **Сменить в любом не-учебном окружении** |
| `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL` | `15m`, `7d` | Время жизни токенов (только для бэкенда) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Разрешённые источники CORS |
| `SEED_DEMO_USERS` | `true` | Создавать демо-пользователей при старте |
| `DB_HOST_PORT`, `REDIS_HOST_PORT`, `BACKEND_HOST_PORT`, `FRONTEND_HOST_PORT` | 5433, 6379, 8080, 4200 | Порты на хосте |

## Локальная разработка

Инфраструктуру удобно держать в Docker, а бэкенд и фронтенд запускать локально:

```bash
docker compose up -d postgres redis

# бэкенд (http://localhost:8080)
cd backend
mvn -DskipTests package
java -jar bootstrap/target/autoparts-backend.jar
# в IntelliJ IDEA: запустить класс feehch.dev.autoparts.AutopartsApplication (модуль bootstrap)

# фронтенд (http://localhost:4200, /api проксируется на :8080 через proxy.conf.json)
cd frontend
npm install
npm start
```

Перед локальным запуском бэкенда остановите контейнер `backend` (`docker compose stop backend`), чтобы освободить порт 8080.

## Тесты

```bash
cd backend && mvn test                 # модульные тесты домена и сценариев использования
cd frontend && npm test -- --watch=false
./scripts/smoke-test.sh                # сквозной смоук-тест API на запущенном стеке (нужны curl и jq)
cd frontend && npx playwright test     # e2e-тесты всего сайта в браузере (нужен запущенный стек и Google Chrome)
```

`smoke-test.sh` проверяет публичный каталог, вход, разграничение доступа по ролям, полный цикл заказа
(списание и возврат остатков, допустимые и недопустимые переходы статусов), ротацию refresh-токенов и выход.

## Возможные проблемы

| Симптом | Решение |
|---------|---------|
| `port is already allocated` | Порт занят другим процессом — измените `*_HOST_PORT` в `.env` |
| Бэкенд не стартует: `Validate failed: Migrations have failed validation` | Изменена уже применённая миграция. Добавьте новую миграцию `V3__...` или пересоздайте БД (`docker compose down -v`) |
| `EACCES` при `npm install` | Права на `~/.npm`: `sudo chown -R $(id -u):$(id -g) ~/.npm` или `npm install --cache /tmp/npm-cache` |
| Фронтенд показывает 502 | Бэкенд ещё запускается или упал — `docker compose logs backend` |

### E2E-тесты (Playwright)

Тесты лежат в `frontend/e2e` и запускаются против поднятого стека (`E2E_BASE_URL`, по умолчанию http://localhost:4200)
в системном Google Chrome — в двух конфигурациях: десктоп 1440×900 и мобильный (Pixel 7).

* `layout-audit.spec.ts` — обход всех страниц сайта под гостем, покупателем, менеджером и администратором:
  ошибки консоли и HTTP, горизонтальная прокрутка, обрезанные и наложенные друг на друга кнопки/поля,
  незагрузившиеся изображения, отсутствие российских рублей. Скриншоты всех страниц сохраняются в
  `frontend/e2e-results/screens/<desktop|mobile>/`.
* `flows.spec.ts` — сценарии: фильтры и подбор по автомобилю, поиск, регистрация с маской белорусского номера,
  корзина → оформление → отмена заказа, обработка заказа менеджером по статусам, создание товара,
  создание сотрудника администратором, запрет доступа к чужим разделам.

HTML-отчёт: `npx playwright show-report e2e-results/report`.

## Изображения товаров

Иллюстрации товаров (SVG) лежат в `frontend/public/images/products/<артикул>.svg` и раздаются nginx;
ссылка хранится в `products.image_url`. Они сгенерированы скриптом `scripts/generate-product-images.py`
(только стандартная библиотека Python; шаблон рисунка на каждую категорию + фирменный цвет бренда).
Для товаров, добавленных менеджером, можно указать любой URL изображения в форме товара.

