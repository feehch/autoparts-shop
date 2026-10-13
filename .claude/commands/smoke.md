---
description: Пересобрать стек в Docker и прогнать смоук-тест API
---
1. Выполни `docker compose up -d --build` и дождись, пока `autoparts-backend-1` станет `healthy`
   (`docker inspect -f '{{.State.Health.Status}}' autoparts-backend-1`).
2. Запусти `./scripts/smoke-test.sh`.
3. Если есть ошибки — покажи `docker compose logs --tail=100 backend`, найди причину и предложи исправление.
Кратко отчитайся по-русски: сколько проверок прошло, что упало.
