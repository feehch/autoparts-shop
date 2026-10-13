#!/usr/bin/env bash
# Смоук-тест основных сценариев API через запущенный стек (по умолчанию — через nginx фронтенда).
# Требуются curl и jq. Использование: ./scripts/smoke-test.sh [BASE_URL]
set -euo pipefail

BASE="${1:-http://localhost:4200}"
API="$BASE/api"
PASS=0
FAIL=0

check() { # check <описание> <ожидаемый код> <фактический код>
  if [[ "$2" == "$3" ]]; then PASS=$((PASS + 1)); echo "  ✔ $1 ($3)";
  else FAIL=$((FAIL + 1)); echo "  ✘ $1: ожидался $2, получен $3"; fi
}
code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
login() { curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$1\",\"password\":\"$2\"}"; }

echo "== Каталог (публичный доступ)"
check "список товаров" 200 "$(code "$API/products?size=5")"
check "категории" 200 "$(code "$API/categories")"
check "подбор по марке Toyota" 200 "$(code "$API/products?makeId=1")"
TOTAL=$(curl -s "$API/products?categoryId=3&size=1" | jq .totalElements)
echo "  товаров в «Тормозной системе» с подкатегориями: $TOTAL"

echo "== Аутентификация"
CUSTOMER=$(login customer@autoparts.local 'Customer123!')
CT=$(echo "$CUSTOMER" | jq -r .accessToken)
CR=$(echo "$CUSTOMER" | jq -r .refreshToken)
[[ "$CT" != "null" ]] && check "вход покупателя" ok ok || check "вход покупателя" ok fail
check "неверный пароль" 401 "$(code -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"customer@autoparts.local","password":"wrong-password"}')"
check "/me без токена" 401 "$(code "$API/auth/me")"
check "/me с токеном" 200 "$(code -H "Authorization: Bearer $CT" "$API/auth/me")"

echo "== Роли"
check "покупатель → создание товара" 403 "$(code -X POST -H "Authorization: Bearer $CT" \
  -H 'Content-Type: application/json' -d '{}' "$API/products")"
check "покупатель → список пользователей" 403 "$(code -H "Authorization: Bearer $CT" "$API/users")"
MT=$(login manager@autoparts.local 'Manager123!' | jq -r .accessToken)
check "менеджер → статистика" 200 "$(code -H "Authorization: Bearer $MT" "$API/stats/dashboard")"
check "менеджер → список пользователей" 403 "$(code -H "Authorization: Bearer $MT" "$API/users")"
AT=$(login admin@autoparts.local 'Admin123!' | jq -r .accessToken)
check "администратор → список пользователей" 200 "$(code -H "Authorization: Bearer $AT" "$API/users")"

echo "== Корзина и заказ"
curl -s -X DELETE -H "Authorization: Bearer $CT" "$API/cart" >/dev/null
STOCK_BEFORE=$(curl -s "$API/products/6" | jq .stockQuantity)
check "добавление в корзину" 200 "$(code -X POST -H "Authorization: Bearer $CT" -H 'Content-Type: application/json' \
  -d '{"productId":6,"quantity":2}' "$API/cart/items")"
check "слишком большое количество" 409 "$(code -X POST -H "Authorization: Bearer $CT" -H 'Content-Type: application/json' \
  -d '{"productId":6,"quantity":999}' "$API/cart/items")"
ORDER=$(curl -s -X POST -H "Authorization: Bearer $CT" -H 'Content-Type: application/json' \
  -d '{"deliveryAddress":"г. Москва, ул. Тестовая, 1","contactPhone":"+375291234567","paymentMethod":"CASH_ON_DELIVERY"}' \
  "$API/orders")
OID=$(echo "$ORDER" | jq -r .id)
echo "  оформлен заказ $(echo "$ORDER" | jq -r .number) на сумму $(echo "$ORDER" | jq -r .totalAmount)"
STOCK_AFTER=$(curl -s "$API/products/6" | jq .stockQuantity)
check "остаток списан" "$((STOCK_BEFORE - 2))" "$STOCK_AFTER"
check "корзина очищена" 0 "$(curl -s -H "Authorization: Bearer $CT" "$API/cart" | jq .totalQuantity)"
check "недопустимый переход NEW→DELIVERED" 409 "$(code -X PATCH -H "Authorization: Bearer $MT" \
  -H 'Content-Type: application/json' -d '{"status":"DELIVERED"}' "$API/orders/$OID/status")"
check "менеджер подтверждает заказ" 200 "$(code -X PATCH -H "Authorization: Bearer $MT" \
  -H 'Content-Type: application/json' -d '{"status":"CONFIRMED"}' "$API/orders/$OID/status")"
check "покупатель не может отменить подтверждённый" 409 "$(code -X POST -H "Authorization: Bearer $CT" \
  "$API/orders/$OID/cancel")"
check "менеджер отменяет заказ" 200 "$(code -X PATCH -H "Authorization: Bearer $MT" \
  -H 'Content-Type: application/json' -d '{"status":"CANCELLED"}' "$API/orders/$OID/status")"
check "остаток возвращён" "$STOCK_BEFORE" "$(curl -s "$API/products/6" | jq .stockQuantity)"

echo "== Токены"
NEW=$(curl -s -X POST "$API/auth/refresh" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$CR\"}")
NT=$(echo "$NEW" | jq -r .accessToken)
NR=$(echo "$NEW" | jq -r .refreshToken)
check "повторное использование старого refresh-токена" 401 "$(code -X POST "$API/auth/refresh" \
  -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$CR\"}")"
check "выход" 204 "$(code -X POST -H "Authorization: Bearer $NT" -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$NR\"}" "$API/auth/logout")"
check "access-токен после выхода" 401 "$(code -H "Authorization: Bearer $NT" "$API/auth/me")"

echo
echo "Итого: успешно $PASS, ошибок $FAIL"
[[ $FAIL -eq 0 ]]
