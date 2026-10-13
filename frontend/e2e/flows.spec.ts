import { expect, Page, test } from '@playwright/test';
import { collectProblems, login, USERS } from './helpers';

/** Функциональные сценарии всех ролей. */

const unique = () => Date.now().toString().slice(-7);

async function apiToken(page: Page, role: keyof typeof USERS): Promise<string> {
  const res = await page.request.post('/api/auth/login', { data: USERS[role] });
  return (await res.json()).accessToken;
}

async function clearCart(page: Page, role: keyof typeof USERS) {
  const token = await apiToken(page, role);
  await page.request.delete('/api/cart', { headers: { Authorization: `Bearer ${token}` } });
}

test.describe('каталог (гость)', () => {
  test('цены в белорусских рублях, фильтры и подбор по автомобилю', async ({ page, isMobile }) => {
    const problems = collectProblems(page);
    await page.goto('/catalog');
    const firstPrice = page.locator('.product-card .price').first();
    await expect(firstPrice).toContainText('Br');
    await expect(page.locator('body')).not.toContainText('₽');

    if (isMobile) await page.getByRole('button', { name: /Фильтры/ }).click();
    const total = async () =>
      Number((await page.getByText(/Найдено:/).innerText()).replace(/\D/g, ''));
    const all = await total();
    expect(all).toBeGreaterThanOrEqual(100);

    // подбор по автомобилю: Belgee X50
    await page.locator('.filters select').first().selectOption({ label: 'Belgee' });
    await page.locator('.filters select').nth(1).selectOption({ label: 'X50 (2023–н.в.)' });
    await expect(page).toHaveURL(/modelId=18/);
    await expect.poll(total).toBeLessThan(all);
    expect(await total()).toBeGreaterThan(5);
    expect(problems).toEqual([]);
  });

  test('поиск из шапки и карточка товара', async ({ page }) => {
    await page.goto('/catalog');
    await page.locator('form.search input').fill('Fenox');
    await page.locator('form.search input').press('Enter');
    await expect(page).toHaveURL(/q=Fenox/);
    await expect(page.locator('.product-card').first()).toContainText(/fenox/i);
    await page.locator('.product-card .product-name').first().click();
    await expect(page).toHaveURL(/\/products\/\d+/);
    await expect(page.locator('body')).toContainText('Применимость');
    await expect(page.locator('img').first()).toBeVisible();
  });

  test('гостя при добавлении в корзину отправляет на вход', async ({ page }) => {
    await page.goto('/products/6');
    await page.getByRole('button', { name: /В корзину/ }).click();
    await expect(page).toHaveURL(/\/login/);
  });

  test('защищённые страницы требуют входа', async ({ page }) => {
    await page.goto('/orders');
    await expect(page).toHaveURL(/\/login/);
  });
});

test.describe('регистрация и профиль', () => {
  test('маска белорусского номера и регистрация', async ({ page }) => {
    const problems = collectProblems(page, [409]);
    const email = `e2e${unique()}@mail.by`;
    await page.goto('/register');
    await page.locator('input[formcontrolname="firstName"]').fill('Алесь');
    await page.locator('input[formcontrolname="lastName"]').fill('Тестовы');
    await page.locator('input[formcontrolname="email"]').fill(email);

    const phone = page.locator('input[formcontrolname="phone"]');
    await phone.click();
    await expect(phone).toHaveValue('+375 (');
    // российский номер не проходит проверку
    await phone.pressSequentially('9991234567');
    await phone.blur();
    await expect(page.getByText('Белорусский номер: +375 (XX) XXX-XX-XX')).toBeVisible();
    await phone.fill('');
    await phone.click();
    await phone.pressSequentially('291234567');
    await expect(phone).toHaveValue('+375 (29) 123-45-67');

    await page.locator('input[formcontrolname="password"]').fill('Password123');
    await page.locator('input[formcontrolname="confirm"]').fill('Password123');
    await page.locator('main button[type="submit"]').click();
    await expect(page).not.toHaveURL(/\/register/);

    await page.goto('/profile');
    await expect(page.locator('input[formcontrolname="phone"]')).toHaveValue('+375 (29) 123-45-67');
    expect(problems).toEqual([]);
  });
});

test.describe('покупатель', () => {
  test.beforeEach(async ({ page }) => {
    await clearCart(page, 'customer');
    await login(page, 'customer');
  });

  test('корзина → оформление → отмена заказа', async ({ page }) => {
    const problems = collectProblems(page);
    await page.goto('/products/45');
    await page.getByRole('button', { name: /В корзину/ }).click();
    await expect(page.locator('header')).toContainText('1');

    await page.goto('/cart');
    await expect(page.locator('body')).toContainText('Br');
    await page.locator('.cart-item button', { hasText: '+' }).first().click();
    await expect(page.locator('.cart-item input').first()).toHaveValue('2');
    await page.getByRole('button', { name: /Оформить/ }).click();

    await expect(page).toHaveURL(/\/checkout/);
    await page
      .locator('textarea[formcontrolname="deliveryAddress"]')
      .fill('г. Минск, ул. Кальварийская, 1');
    await expect(page.locator('input[formcontrolname="contactPhone"]')).toHaveValue(
      /^\+375 \(\d\d\) \d{3}-\d\d-\d\d$/,
    );
    await page.locator('main button[type="submit"]').click();

    await expect(page).toHaveURL(/\/orders\/\d+/);
    await expect(page.locator('body')).toContainText('Новый');
    await expect(page.locator('body')).toContainText('+375');
    page.once('dialog', (d) => d.accept());
    await page.getByRole('button', { name: /Отменить/ }).click();
    await expect(page.locator('body')).toContainText('Отменён');
    expect(problems).toEqual([]);
  });

  test('покупателю закрыт раздел управления', async ({ page }) => {
    await page.goto('/admin');
    await expect(page).toHaveURL(/forbidden/);
  });
});

test.describe('менеджер', () => {
  test.beforeEach(async ({ page }) => login(page, 'manager'));

  test('обработка заказа по статусам', async ({ page }) => {
    // заказ от покупателя через API
    const token = await apiToken(page, 'customer');
    const headers = { Authorization: `Bearer ${token}` };
    await page.request.delete('/api/cart', { headers });
    await page.request.post('/api/cart/items', { headers, data: { productId: 93, quantity: 2 } });
    const order = await (
      await page.request.post('/api/orders', {
        headers,
        data: {
          deliveryAddress: 'г. Гомель, ул. Советская, 5',
          contactPhone: '+375 44 765-43-21',
          paymentMethod: 'CARD_ONLINE',
        },
      })
    ).json();
    expect(order.contactPhone).toBe('+375447654321');

    const problems = collectProblems(page);
    await page.goto(`/admin/orders/${order.id}`);
    for (const [button, status] of [
      ['Подтвердить', 'Подтверждён'],
      ['Отправить', 'Отправлен'],
      ['Отметить доставленным', 'Доставлен'],
    ]) {
      page.once('dialog', (d) => d.accept());
      await page.getByRole('button', { name: new RegExp(button) }).click();
      await expect(page.locator('.badge').first()).toContainText(status);
    }
    expect(problems).toEqual([]);
  });

  test('создание, редактирование и деактивация товара', async ({ page }) => {
    const problems = collectProblems(page);
    const sku = `E2E-${unique()}`;
    try {
      await page.goto('/admin/products/new');
      await page.locator('input[formcontrolname="sku"]').fill(sku);
      await page.locator('input[formcontrolname="name"]').fill('Тестовая свеча зажигания');
      await page.locator('input[formcontrolname="price"]').fill('19.90');
      await page.locator('input[formcontrolname="stockQuantity"]').fill('5');
      await page.locator('select[formcontrolname="brandId"]').selectOption({ label: 'NGK' });
      await page.locator('select[formcontrolname="categoryId"]').selectOption({ index: 1 });
      await page.locator('main button[type="submit"]').click();
      await expect(page).toHaveURL(/\/admin\/products(\/\d+)?$/);

      await page.goto('/admin/products');
      await page.locator('main input[type="search"]').fill(sku);
      await page.locator('main input[type="search"]').press('Enter');
      await expect(page.locator('table')).toContainText(sku);
      await expect(page.locator('table')).toContainText('19,90');
      expect(problems).toEqual([]);
    } finally {
      // уборка: деактивация созданного товара через API
      const headers = { Authorization: `Bearer ${await apiToken(page, 'manager')}` };
      const list = await (
        await page.request.get(`/api/products?q=${sku}&includeInactive=true`, { headers })
      ).json();
      for (const p of list.content) await page.request.delete(`/api/products/${p.id}`, { headers });
    }
  });

  test('менеджеру закрыт раздел пользователей', async ({ page }) => {
    await page.goto('/admin/users');
    await expect(page).toHaveURL(/forbidden/);
  });
});

test.describe('администратор', () => {
  test.beforeEach(async ({ page }) => login(page, 'admin'));

  test('создание сотрудника и назначение ролей', async ({ page }) => {
    const problems = collectProblems(page);
    const email = `manager${unique()}@autoparts.by`;
    await page.goto('/admin/users');
    await page.getByRole('button', { name: '+ Новый пользователь' }).click();
    const form = page
      .locator('form')
      .filter({ has: page.locator('input[formcontrolname="email"]') });
    await form.locator('input[formcontrolname="email"]').fill(email);
    await form.locator('input[formcontrolname="password"]').fill('Manager12345');
    await form.locator('input[formcontrolname="firstName"]').fill('Янка');
    await form.locator('input[formcontrolname="lastName"]').fill('Менеджэр');
    await form.locator('input[formcontrolname="phone"]').click();
    await form.locator('input[formcontrolname="phone"]').pressSequentially('336665544');
    await form.getByText('Менеджер', { exact: true }).click();
    await form.locator('button[type="submit"]').click();

    await page.locator('main input[type="search"]').fill(email);
    await page.locator('main input[type="search"]').press('Enter');
    const row = page.locator('tr', { hasText: email });
    await expect(row).toBeVisible();
    await expect(row).toContainText('+375 (33) 666-55-44');
    await expect(row).toContainText('Менеджер');
    expect(problems).toEqual([]);
  });
});
