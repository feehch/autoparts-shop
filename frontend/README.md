# Frontend — «АвтоДеталь»

Клиентская часть информационной системы интернет-магазина автозапчастей легковых автомобилей.
Angular 20 (standalone-компоненты, signals, новый control flow `@if`/`@for`, ленивые маршруты,
реактивные формы), собственные стили на SCSS без UI-библиотек.

## Структура

```
src/app
├── app.ts / app.config.ts / app.routes.ts   корневой компонент, провайдеры, маршруты
├── core/                                     ядро приложения
│   ├── models/        TypeScript-интерфейсы DTO (зеркало docs/api.md)
│   ├── services/      сервисы по областям API: auth, catalog, product, cart, order, user, stats, notification
│   ├── interceptors/  authInterceptor (Bearer + refresh при 401), errorInterceptor (toast с ApiError.message)
│   ├── guards/        authGuard, roleGuard (data.roles, иерархия ADMIN > MANAGER > CUSTOMER), guestGuard
│   └── layout/        шапка сайта
├── shared/                                   переиспользуемое
│   ├── components/    пагинация, уведомления, бейдж статуса, ошибки полей, изображение товара
│   ├── pipes/         rub, orderStatus, roleName, paymentMethod, years
│   └── utils/         валидаторы, перенос fieldErrors в форму
└── features/                                 страницы
    ├── catalog/       каталог с фильтрами (URL query params), карточка товара
    ├── cart/          корзина, оформление заказа
    ├── orders/        мои заказы, детали заказа
    ├── auth/          вход, регистрация
    ├── profile/       профиль и смена пароля
    ├── admin/         раздел управления (MANAGER+): обзор, товары, категории, бренды,
    │                  автомобили, заказы, пользователи (только ADMIN)
    └── errors/        страницы 404 и 403
```

Токены (access + refresh) и текущий пользователь хранятся в `localStorage`. При ответе `401`
интерцептор один раз обновляет токены через `/api/auth/refresh` (параллельные запросы ждут
один общий refresh) и повторяет запрос; при неудаче — выход и переход на `/login`.

## Запуск в режиме разработки

Требуется Node.js 22 и запущенный backend на `http://localhost:8080`.

```bash
npm install
npm start          # ng serve с proxy.conf.json → http://localhost:4200
```

Запросы `/api`, `/swagger-ui`, `/v3` проксируются на `http://localhost:8080` (см. `proxy.conf.json`).

## Сборка

```bash
npm run build      # результат: dist/frontend/browser
npm test           # unit-тесты (Karma, нужен Chrome)
```

## Docker

```bash
docker build -t autoparts-frontend .
docker run -p 80:80 autoparts-frontend
```

Образ собирается в два этапа: `node:22-alpine` (`npm ci` + `npm run build`) и `nginx:alpine`,
который раздаёт статику (SPA-fallback на `index.html`, gzip, долгое кеширование файлов с хешем)
и проксирует `/api/`, `/swagger-ui`, `/v3/api-docs` на `http://backend:8080` — имя сервиса
backend в docker-compose.

## Демонстрационные учётные записи

| Email                      | Пароль         | Роли                     |
|----------------------------|----------------|--------------------------|
| `admin@autoparts.local`    | `Admin123!`    | ADMIN, MANAGER, CUSTOMER |
| `manager@autoparts.local`  | `Manager123!`  | MANAGER, CUSTOMER        |
| `customer@autoparts.local` | `Customer123!` | CUSTOMER                 |

## E2E-тесты

```bash
npm run e2e        # Playwright, системный Google Chrome; стек должен быть запущен (docker compose up)
```

Тесты в `e2e/`: аудит вёрстки всех страниц под каждой ролью (десктоп и мобильный, скриншоты в
`e2e-results/screens`) и функциональные сценарии. Подробнее — `docs/deployment.md`.
