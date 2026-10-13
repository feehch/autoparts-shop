# REST API

Базовый префикс: `/api`. Формат обмена — JSON (UTF-8). Даты — ISO-8601 (`2026-10-13T12:00:00Z`).
Денежные суммы — число с двумя знаками после запятой (`BigDecimal`) в **белорусских рублях (BYN)**.
Номера телефонов — только белорусские. Сервер принимает записи вида `+375 (29) 123-45-67`, `375291234567`,
`8 029 123-45-67` и хранит/возвращает их в виде `+375291234567`. Допустимые коды после 375: 25, 29, 33, 44
(мобильные операторы), 17, 15x, 16x, 21x–23x (городские). Неверный номер → `400`.

Интерактивная документация (Swagger UI): `http://localhost:8080/swagger-ui.html`,
OpenAPI-спецификация: `http://localhost:8080/v3/api-docs`.

## Аутентификация

Используются JWT-токены:

* **access token** — живёт 15 минут, передаётся в заголовке `Authorization: Bearer <token>`;
* **refresh token** — живёт 7 дней и хранится в Redis. При обновлении выдаётся новая пара токенов, а старый refresh-токен отзывается (ротация).

При выходе (`logout`) refresh-токен удаляется из Redis, а идентификатор (`jti`) access-токена попадает в чёрный список до конца срока его жизни.

## Роли

| Роль       | Описание                                                                 |
|------------|--------------------------------------------------------------------------|
| `CUSTOMER` | Покупатель: корзина, оформление и просмотр своих заказов, профиль        |
| `MANAGER`  | Менеджер: всё, что может покупатель, а также управление каталогом и всеми заказами, статистика |
| `ADMIN`    | Администратор: всё, что может менеджер, а также управление пользователями и их ролями |

Роли образуют иерархию `ADMIN > MANAGER > CUSTOMER`. У пользователя может быть несколько ролей.

## Общие структуры

### Page&lt;T&gt;
```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 135, "totalPages": 7 }
```
Параметры пагинации: `page` (с 0, по умолчанию 0) и `size` (по умолчанию 20, максимум 100).

### ApiError
```json
{
  "timestamp": "2026-10-13T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "fieldErrors": [ { "field": "email", "message": "must be a well-formed email address" } ]
}
```
Коды ответов: `400` — ошибка валидации, `401` — не аутентифицирован или токен недействителен,
`403` — недостаточно прав, `404` — объект не найден, `409` — конфликт (дубликат, недопустимый переход статуса,
нехватка товара на складе и т.п.).

### UserDto
```json
{
  "id": 1, "email": "user@mail.ru", "firstName": "Иван", "lastName": "Петров",
  "phone": "+375291234567", "roles": ["CUSTOMER"], "enabled": true,
  "createdAt": "2026-10-13T12:00:00Z"
}
```

---

## Auth — `/api/auth`

| Метод | Путь              | Доступ   | Тело запроса      | Ответ          |
|-------|-------------------|----------|-------------------|----------------|
| POST  | `/register`       | все      | RegisterRequest   | `201` AuthResponse |
| POST  | `/login`          | все      | LoginRequest      | AuthResponse   |
| POST  | `/refresh`        | все      | RefreshRequest    | AuthResponse   |
| POST  | `/logout`         | аутентиф. | RefreshRequest   | `204`          |
| GET   | `/me`             | аутентиф. | —                | UserDto        |

```jsonc
// RegisterRequest
{ "email": "user@mail.ru", "password": "secret123", "firstName": "Иван", "lastName": "Петров", "phone": "+375 (29) 123-45-67" }
// password: 8..100 символов; phone необязателен
// LoginRequest
{ "email": "user@mail.ru", "password": "secret123" }
// RefreshRequest
{ "refreshToken": "..." }
// AuthResponse
{ "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer", "expiresIn": 900, "user": { /* UserDto */ } }
```
Заблокированный пользователь (`enabled=false`) получает `403` при входе.

## Профиль — `/api/profile` (аутентифицированные)

| Метод | Путь         | Тело                     | Ответ   |
|-------|--------------|--------------------------|---------|
| GET   | `/`          | —                        | UserDto |
| PUT   | `/`          | `{ "firstName", "lastName", "phone" }` | UserDto |
| PUT   | `/password`  | `{ "currentPassword", "newPassword" }` | `204`  |

---

## Каталог

### Категории — `/api/categories`

| Метод  | Путь     | Доступ   | Тело / ответ |
|--------|----------|----------|--------------|
| GET    | `/`      | все      | `CategoryDto[]` (плоский список, дерево строится по `parentId`) |
| GET    | `/{id}`  | все      | CategoryDto |
| POST   | `/`      | MANAGER  | CategoryRequest → `201` CategoryDto |
| PUT    | `/{id}`  | MANAGER  | CategoryRequest → CategoryDto |
| DELETE | `/{id}`  | MANAGER  | `204` (`409`, если есть подкатегории или товары) |

```jsonc
// CategoryDto
{ "id": 1, "name": "Тормозная система", "slug": "brakes", "description": "...", "parentId": null }
// CategoryRequest (slug необязателен — сгенерируется из имени)
{ "name": "Тормозные колодки", "slug": "brake-pads", "description": "...", "parentId": 1 }
```

### Бренды производителей запчастей — `/api/brands`

| Метод  | Путь    | Доступ  | Тело / ответ |
|--------|---------|---------|--------------|
| GET    | `/`     | все     | `BrandDto[]` |
| GET    | `/{id}` | все     | BrandDto |
| POST   | `/`     | MANAGER | BrandRequest → `201` BrandDto |
| PUT    | `/{id}` | MANAGER | BrandRequest → BrandDto |
| DELETE | `/{id}` | MANAGER | `204` (`409`, если есть товары) |

```jsonc
// BrandDto / BrandRequest (без id)
{ "id": 1, "name": "Bosch", "country": "Германия", "description": "..." }
```

### Автомобили (марки и модели) — `/api/cars`

| Метод  | Путь                       | Доступ  | Тело / ответ |
|--------|----------------------------|---------|--------------|
| GET    | `/makes`                   | все     | `CarMakeDto[]` |
| POST   | `/makes`                   | MANAGER | `{ "name", "country" }` → `201` CarMakeDto |
| PUT    | `/makes/{id}`              | MANAGER | `{ "name", "country" }` → CarMakeDto |
| DELETE | `/makes/{id}`              | MANAGER | `204` (`409`, если есть модели) |
| GET    | `/makes/{makeId}/models`   | все     | `CarModelDto[]` |
| GET    | `/models`                  | все     | `CarModelDto[]` (все модели) |
| POST   | `/models`                  | MANAGER | CarModelRequest → `201` CarModelDto |
| PUT    | `/models/{id}`             | MANAGER | CarModelRequest → CarModelDto |
| DELETE | `/models/{id}`             | MANAGER | `204` |

```jsonc
// CarMakeDto
{ "id": 1, "name": "Toyota", "country": "Япония" }
// CarModelDto (yearTo = null — выпускается по сей день)
{ "id": 3, "makeId": 1, "makeName": "Toyota", "name": "Camry XV70", "yearFrom": 2017, "yearTo": null }
// CarModelRequest
{ "makeId": 1, "name": "Camry XV70", "yearFrom": 2017, "yearTo": null }
```

### Товары (запчасти) — `/api/products`

| Метод  | Путь          | Доступ  | Тело / ответ |
|--------|---------------|---------|--------------|
| GET    | `/`           | все     | `Page<ProductSummaryDto>` |
| GET    | `/{id}`       | все     | ProductDto (неактивный товар виден только MANAGER) |
| POST   | `/`           | MANAGER | ProductRequest → `201` ProductDto |
| PUT    | `/{id}`       | MANAGER | ProductRequest → ProductDto |
| PATCH  | `/{id}/stock` | MANAGER | `{ "stockQuantity": 10 }` → ProductDto |
| DELETE | `/{id}`       | MANAGER | `204` (мягкое удаление: `active=false`) |

Параметры поиска `GET /api/products`:

| Параметр          | Описание |
|-------------------|----------|
| `q`               | Поиск по названию, артикулу (`sku`) и OEM-номеру, без учёта регистра |
| `categoryId`      | Категория, включая все её подкатегории |
| `brandId`         | Бренд |
| `makeId`          | Марка автомобиля (товары, совместимые хотя бы с одной моделью марки) |
| `modelId`         | Модель автомобиля |
| `minPrice`, `maxPrice` | Диапазон цены |
| `inStock`         | `true` — только товары в наличии |
| `includeInactive` | `true` — показать и неактивные (только для MANAGER) |
| `sort`            | `name,asc` (по умолчанию), `name,desc`, `price,asc`, `price,desc`, `createdAt,desc` |
| `page`, `size`    | Пагинация |

```jsonc
// ProductSummaryDto
{
  "id": 10, "sku": "BOS-0986494524", "oemNumber": "04465-33450", "name": "Колодки тормозные передние",
  "price": 3490.00, "stockQuantity": 12, "imageUrl": "https://...", "active": true,
  "brandId": 1, "brandName": "Bosch", "categoryId": 5, "categoryName": "Тормозные колодки"
}
// ProductDto = ProductSummaryDto + поля:
{ "description": "...", "compatibleModels": [ /* CarModelDto */ ], "createdAt": "...", "updatedAt": "..." }
// ProductRequest
{
  "sku": "BOS-0986494524", "oemNumber": "04465-33450", "name": "...", "description": "...",
  "price": 3490.00, "stockQuantity": 12, "imageUrl": null, "brandId": 1, "categoryId": 5,
  "compatibleModelIds": [3, 4], "active": true
}
```

---

## Корзина — `/api/cart` (аутентифицированные)

| Метод  | Путь                  | Тело                         | Ответ   |
|--------|-----------------------|------------------------------|---------|
| GET    | `/`                   | —                            | CartDto |
| POST   | `/items`              | `{ "productId", "quantity" }` (добавляет к имеющемуся количеству) | CartDto |
| PUT    | `/items/{productId}`  | `{ "quantity" }` (устанавливает количество, ≥ 1) | CartDto |
| DELETE | `/items/{productId}`  | —                            | CartDto |
| DELETE | `/`                   | —                            | `204`   |

```jsonc
// CartDto
{
  "items": [
    { "productId": 10, "sku": "BOS-0986494524", "name": "...", "imageUrl": null,
      "price": 3490.00, "quantity": 2, "lineTotal": 6980.00, "stockQuantity": 12, "available": true }
  ],
  "totalQuantity": 2,
  "totalAmount": 6980.00
}
```
`available=false`, если товар деактивирован или на складе меньше, чем в корзине. Количество больше остатка
добавить нельзя (`409`).

## Заказы — `/api/orders`

| Метод | Путь             | Доступ                       | Тело / ответ |
|-------|------------------|------------------------------|--------------|
| POST  | `/`              | аутентиф.                    | PlaceOrderRequest → `201` OrderDto (заказ из текущей корзины, корзина очищается, остатки списываются) |
| GET   | `/my`            | аутентиф.                    | `Page<OrderDto>` — свои заказы, новые сверху |
| GET   | `/{id}`          | владелец или MANAGER         | OrderDto |
| POST  | `/{id}/cancel`   | владелец (статус `NEW`)      | OrderDto |
| GET   | `/`              | MANAGER                      | `Page<OrderDto>`; параметры `status`, `q` (номер заказа / email клиента), `page`, `size` |
| PATCH | `/{id}/status`   | MANAGER                      | `{ "status": "CONFIRMED" }` → OrderDto |

Статусы и допустимые переходы:

```
NEW ──► CONFIRMED ──► SHIPPED ──► DELIVERED
 │          │
 └──────────┴──► CANCELLED   (при отмене остатки возвращаются на склад)
```

Способы оплаты: `CASH_ON_DELIVERY`, `CARD_ON_DELIVERY`, `CARD_ONLINE`.

```jsonc
// PlaceOrderRequest
{ "deliveryAddress": "г. Минск, пр. Независимости, д. 1", "contactPhone": "+375291234567",
  "comment": "Позвонить заранее", "paymentMethod": "CASH_ON_DELIVERY" }
// OrderDto
{
  "id": 7, "number": "AP-000007", "userId": 3, "customerEmail": "user@mail.ru", "customerName": "Иван Петров",
  "status": "NEW",
  "items": [ { "productId": 10, "sku": "BOS-0986494524", "name": "...", "price": 3490.00, "quantity": 2, "lineTotal": 6980.00 } ],
  "totalAmount": 6980.00, "deliveryAddress": "...", "contactPhone": "...", "comment": "...",
  "paymentMethod": "CASH_ON_DELIVERY", "createdAt": "...", "updatedAt": "..."
}
```

---

## Пользователи и роли — `/api/users` (ADMIN)

| Метод | Путь              | Тело / ответ |
|-------|-------------------|--------------|
| GET   | `/`               | `Page<UserDto>`; параметры `q` (email / имя), `role`, `page`, `size` |
| GET   | `/{id}`           | UserDto |
| POST  | `/`               | CreateUserRequest → `201` UserDto |
| PUT   | `/{id}/roles`     | `{ "roles": ["MANAGER", "CUSTOMER"] }` → UserDto |
| PATCH | `/{id}/enabled`   | `{ "enabled": false }` → UserDto |
| GET   | `/roles`          | `RoleDto[]` — `[{ "name": "ADMIN", "description": "Администратор" }, ...]` |

```jsonc
// CreateUserRequest
{ "email": "...", "password": "...", "firstName": "...", "lastName": "...", "phone": null, "roles": ["MANAGER"] }
```
Администратор не может снять с себя роль `ADMIN` или заблокировать сам себя (`409`). Список ролей не может быть пустым.

## Статистика — `/api/stats` (MANAGER)

`GET /api/stats/dashboard`:
```json
{
  "productsCount": 120, "activeProductsCount": 118, "lowStockCount": 7,
  "ordersCount": 54, "ordersByStatus": { "NEW": 5, "CONFIRMED": 3, "SHIPPED": 2, "DELIVERED": 40, "CANCELLED": 4 },
  "revenue": 412345.00, "customersCount": 31
}
```
`lowStockCount` — активные товары с остатком меньше 5; `revenue` — сумма доставленных заказов.

## Демонстрационные учётные записи

| Email                     | Пароль        | Роли                       |
|---------------------------|---------------|----------------------------|
| `admin@autoparts.local`   | `Admin123!`   | ADMIN, MANAGER, CUSTOMER   |
| `manager@autoparts.local` | `Manager123!` | MANAGER, CUSTOMER          |
| `customer@autoparts.local`| `Customer123!`| CUSTOMER                   |
