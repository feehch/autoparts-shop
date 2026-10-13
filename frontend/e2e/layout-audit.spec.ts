import { expect, test } from '@playwright/test';
import { auditLayout, collectProblems, login, RoleKey, USERS } from './helpers';

/**
 * Обход всех страниц сайта под каждой ролью: ошибки консоли и HTTP, аудит вёрстки, скриншоты
 * (e2e-results/screens/<проект>/...). Скриншоты удобно просматривать после изменений стилей.
 */

interface PageCase {
  name: string;
  path: string | ((ctx: { orderId: number }) => string);
  allowed?: number[];
}

const PAGES: Record<RoleKey | 'guest', PageCase[]> = {
  guest: [
    { name: 'catalog', path: '/catalog' },
    { name: 'catalog-filtered', path: '/catalog?makeId=8&inStock=true&sort=price,asc' },
    { name: 'catalog-search', path: '/catalog?q=колодки' },
    { name: 'catalog-category', path: '/catalog?categoryId=3' },
    { name: 'product', path: '/products/15' },
    { name: 'product-out-of-stock', path: '/products/23' },
    { name: 'login', path: '/login' },
    { name: 'register', path: '/register' },
    { name: 'not-found', path: '/no-such-page' },
  ],
  customer: [
    { name: 'cart', path: '/cart' },
    { name: 'checkout', path: '/checkout' },
    { name: 'orders', path: '/orders' },
    { name: 'order', path: ({ orderId }) => `/orders/${orderId}` },
    { name: 'profile', path: '/profile' },
    { name: 'forbidden', path: '/admin', allowed: [403] },
  ],
  manager: [
    { name: 'dashboard', path: '/admin' },
    { name: 'products', path: '/admin/products' },
    { name: 'product-new', path: '/admin/products/new' },
    { name: 'product-edit', path: '/admin/products/1' },
    { name: 'categories', path: '/admin/categories' },
    { name: 'brands', path: '/admin/brands' },
    { name: 'cars', path: '/admin/cars' },
    { name: 'orders', path: '/admin/orders' },
    { name: 'order', path: ({ orderId }) => `/admin/orders/${orderId}` },
  ],
  admin: [{ name: 'users', path: '/admin/users' }],
};

let orderId = 0;

test.beforeAll(async ({ request }) => {
  // у покупателя должны быть товар в корзине и хотя бы один заказ
  const auth = await (await request.post('/api/auth/login', { data: USERS.customer })).json();
  const headers = { Authorization: `Bearer ${auth.accessToken}` };
  const orders = await (await request.get('/api/orders/my', { headers })).json();
  if (orders.content.length === 0) {
    await request.post('/api/cart/items', { headers, data: { productId: 6, quantity: 1 } });
    await request.post('/api/orders', {
      headers,
      data: {
        deliveryAddress: 'г. Минск, пр. Независимости, 4',
        contactPhone: '+375291234567',
        paymentMethod: 'CASH_ON_DELIVERY',
      },
    });
  }
  orderId = (await (await request.get('/api/orders/my', { headers })).json()).content[0].id;
  await request.post('/api/cart/items', { headers, data: { productId: 7, quantity: 1 } });
});

for (const [role, cases] of Object.entries(PAGES) as [RoleKey | 'guest', PageCase[]][]) {
  test.describe(`аудит страниц: ${role}`, () => {
    test.beforeEach(async ({ page }) => {
      if (role !== 'guest') await login(page, role);
    });

    for (const c of cases) {
      test(c.name, async ({ page }, info) => {
        const problems = collectProblems(page, c.allowed ?? []);
        const path = typeof c.path === 'function' ? c.path({ orderId }) : c.path;
        await page.goto(path);
        const issues = await auditLayout(page);
        await page.screenshot({
          path: `e2e-results/screens/${info.project.name}/${role}-${c.name}.png`,
          fullPage: true,
        });
        expect.soft(problems, 'ошибки консоли / HTTP').toEqual([]);
        expect.soft(issues, 'проблемы вёрстки').toEqual([]);
        // на сайте не должно остаться российских рублей
        expect.soft(await page.locator('body').innerText()).not.toMatch(/₽|руб\./);
      });
    }
  });
}
