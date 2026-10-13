# АвтоДеталь — интернет-магазин автозапчастей

Курсовая работа на тему **«Информационная система интернет-магазина автозапчастей легковых автомобилей»**.

Веб-приложение для продажи запчастей к легковым автомобилям: каталог с подбором по марке и модели, корзина,
оформление и обработка заказов, панель управления для сотрудников, JWT-аутентификация и разграничение
доступа по ролям.

| Слой | Технологии |
|------|------------|
| Backend | Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA (Hibernate), Flyway, jjwt, springdoc-openapi; Clean Architecture |
| Frontend | Angular 20 (standalone-компоненты, signals), SCSS, nginx |
| Хранилища | PostgreSQL 16 (основная БД), Redis 7 (кэш, refresh-токены, чёрный список токенов) |
| Инфраструктура | Docker, Docker Compose |

## Быстрый старт

```bash
docker compose up -d --build
```

Через 1–2 минуты:

* магазин: **http://localhost:4200**
* Swagger UI: **http://localhost:8080/swagger-ui.html** (или http://localhost:4200/swagger-ui/index.html)
* проверка работоспособности: http://localhost:8080/actuator/health

Демонстрационные учётные записи:

| Роль | Email | Пароль |
|------|-------|--------|
| Администратор | `admin@autoparts.local` | `Admin123!` |
| Менеджер | `manager@autoparts.local` | `Manager123!` |
| Покупатель | `customer@autoparts.local` | `Customer123!` |

Цены — в белорусских рублях (BYN), телефоны — белорусские (+375). В каталоге 102 запчасти с иллюстрациями.

Проверить API целиком можно смоук-тестом (нужны `curl` и `jq`): `./scripts/smoke-test.sh`,
весь сайт в браузере — e2e-тестами Playwright: `cd frontend && npx playwright test`.

## Возможности

**Покупатель**
* каталог с деревом категорий, фильтрами по бренду, цене и наличию, поиском по названию, артикулу и OEM-номеру;
* подбор запчастей по автомобилю (марка → модель/поколение);
* корзина с контролем остатков, оформление заказа с выбором способа оплаты;
* история заказов, отмена нового заказа, редактирование профиля.

**Менеджер** — ведение каталога (товары, категории, бренды, марки и модели автомобилей, остатки), обработка заказов
по конечному автомату статусов, панель статистики.

**Администратор** — управление пользователями: создание сотрудников, назначение ролей, блокировка.

## Структура репозитория

```
.
├── backend/                 Spring Boot, многомодульный Maven-проект (Clean Architecture)
│   ├── domain/              сущности и бизнес-правила (чистая Java)
│   ├── application/         сценарии использования и порты (чистая Java)
│   ├── infrastructure/      JPA, Redis, JWT, Spring Security, миграции Flyway
│   ├── presentation/        REST-контроллеры, DTO, обработка ошибок, OpenAPI
│   ├── bootstrap/           точка входа, конфигурация, сборка jar
│   └── Dockerfile
├── frontend/                Angular-приложение + Dockerfile + nginx.conf
├── docs/                    документация
├── scripts/                smoke-test.sh (проверка API), generate-product-images.py (иллюстрации товаров)
├── docker-compose.yml
├── .env.example
├── CLAUDE.md                инструкции для Claude Code
└── pom.xml                  агрегатор для открытия проекта в IntelliJ IDEA
```

## Документация

| Документ | Содержание |
|----------|------------|
| [docs/architecture.md](docs/architecture.md) | Архитектура системы, слои Clean Architecture, ключевые сценарии, кэширование |
| [docs/security.md](docs/security.md) | Аутентификация (JWT + refresh-токены в Redis), роли и разграничение доступа |
| [docs/api.md](docs/api.md) | Описание REST API: эндпоинты, DTO, коды ошибок |
| [docs/database.md](docs/database.md) | Схема БД, ER-диаграмма, миграции, индексы |
| [docs/deployment.md](docs/deployment.md) | Развёртывание в Docker, переменные окружения, локальная разработка, тесты |
| [docs/user-guide.md](docs/user-guide.md) | Руководство пользователя для покупателя, менеджера и администратора |
| [frontend/README.md](frontend/README.md) | Структура и запуск фронтенда |

## Разработка

```bash
docker compose up -d postgres redis                                  # инфраструктура
cd backend && mvn -DskipTests package && java -jar bootstrap/target/autoparts-backend.jar
cd frontend && npm install && npm start                              # http://localhost:4200, прокси на :8080
```

Тесты: `cd backend && mvn test` (модульные тесты домена и сценариев), `cd frontend && npm test -- --watch=false`,
e2e: `cd frontend && npx playwright test` (на запущенном стеке).

Подробнее — в [docs/deployment.md](docs/deployment.md).
