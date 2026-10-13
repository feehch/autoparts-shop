-- =====================================================================
-- Схема БД интернет-магазина автозапчастей
-- =====================================================================

-- ---------- Пользователи и роли ----------
CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(30),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE user_roles
(
    user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('CUSTOMER', 'MANAGER', 'ADMIN'))
);

-- ---------- Справочники каталога ----------
CREATE TABLE categories
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(150) NOT NULL,
    description TEXT,
    parent_id   BIGINT REFERENCES categories (id),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);
CREATE INDEX ix_categories_parent ON categories (parent_id);

CREATE TABLE brands
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    country     VARCHAR(100),
    description TEXT
);
CREATE UNIQUE INDEX uq_brands_name ON brands (lower(name));

CREATE TABLE car_makes
(
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    country VARCHAR(100)
);
CREATE UNIQUE INDEX uq_car_makes_name ON car_makes (lower(name));

CREATE TABLE car_models
(
    id        BIGSERIAL PRIMARY KEY,
    make_id   BIGINT       NOT NULL REFERENCES car_makes (id),
    name      VARCHAR(150) NOT NULL,
    year_from INTEGER      NOT NULL,
    year_to   INTEGER,
    CONSTRAINT ck_car_models_years CHECK (year_to IS NULL OR year_to >= year_from)
);
CREATE INDEX ix_car_models_make ON car_models (make_id);

-- ---------- Товары ----------
CREATE TABLE products
(
    id             BIGSERIAL PRIMARY KEY,
    sku            VARCHAR(64)    NOT NULL,
    oem_number     VARCHAR(64),
    name           VARCHAR(255)   NOT NULL,
    description    TEXT,
    price          NUMERIC(12, 2) NOT NULL,
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
    image_url      VARCHAR(500),
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    brand_id       BIGINT         NOT NULL REFERENCES brands (id),
    category_id    BIGINT         NOT NULL REFERENCES categories (id),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version        BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_stock CHECK (stock_quantity >= 0)
);
CREATE INDEX ix_products_category ON products (category_id);
CREATE INDEX ix_products_brand ON products (brand_id);
CREATE INDEX ix_products_name ON products (lower(name));
CREATE INDEX ix_products_oem ON products (lower(oem_number));

-- Совместимость запчасти с моделями автомобилей (многие-ко-многим)
CREATE TABLE product_compatibility
(
    product_id   BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    car_model_id BIGINT NOT NULL REFERENCES car_models (id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, car_model_id)
);
CREATE INDEX ix_product_compatibility_model ON product_compatibility (car_model_id);

-- ---------- Корзина ----------
CREATE TABLE cart_items
(
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id BIGINT      NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity   INTEGER     NOT NULL,
    added_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, product_id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0)
);

-- ---------- Заказы ----------
CREATE TABLE orders
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES users (id),
    status           VARCHAR(20)    NOT NULL,
    total_amount     NUMERIC(12, 2) NOT NULL,
    delivery_address VARCHAR(500)   NOT NULL,
    contact_phone    VARCHAR(30)    NOT NULL,
    comment          VARCHAR(1000),
    payment_method   VARCHAR(30)    NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_orders_status CHECK (status IN ('NEW', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_orders_payment CHECK (payment_method IN ('CASH_ON_DELIVERY', 'CARD_ON_DELIVERY', 'CARD_ONLINE'))
);
CREATE INDEX ix_orders_user ON orders (user_id, created_at DESC);
CREATE INDEX ix_orders_status ON orders (status);

CREATE TABLE order_items
(
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id BIGINT         NOT NULL REFERENCES products (id),
    sku        VARCHAR(64)    NOT NULL,
    name       VARCHAR(255)   NOT NULL,
    price      NUMERIC(12, 2) NOT NULL,
    quantity   INTEGER        NOT NULL,
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_order_items_order ON order_items (order_id);
