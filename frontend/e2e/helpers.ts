import { expect, Page } from '@playwright/test';

export const USERS = {
  admin: { email: 'admin@autoparts.local', password: 'Admin123!' },
  manager: { email: 'manager@autoparts.local', password: 'Manager123!' },
  customer: { email: 'customer@autoparts.local', password: 'Customer123!' },
} as const;

export type RoleKey = keyof typeof USERS;

/** Вход через форму. */
export async function login(page: Page, role: RoleKey): Promise<void> {
  const { email, password } = USERS[role];
  await page.goto('/login');
  await page.locator('input[formcontrolname="email"]').fill(email);
  await page.locator('input[formcontrolname="password"]').fill(password);
  await page.locator('main button[type="submit"]').click();
  await expect(page).not.toHaveURL(/\/login/);
}

/** Собирает ошибки консоли и неуспешные ответы API за время теста. */
export function collectProblems(page: Page, allowedStatuses: number[] = []): string[] {
  const problems: string[] = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') problems.push(`console: ${msg.text()}`);
  });
  page.on('pageerror', (err) => problems.push(`pageerror: ${err.message}`));
  page.on('response', (res) => {
    const status = res.status();
    if (status >= 400 && !allowedStatuses.includes(status)) {
      problems.push(`http ${status}: ${res.request().method()} ${res.url()}`);
    }
  });
  return problems;
}

export interface LayoutIssue {
  kind: 'horizontal-scroll' | 'clipped' | 'overlap' | 'broken-image';
  detail: string;
}

/**
 * Эвристический аудит вёрстки:
 *  - горизонтальная прокрутка страницы;
 *  - интерактивные элементы, обрезанные контейнером с overflow: hidden/auto;
 *  - пересекающиеся между собой кнопки, бейджи и поля в одном контейнере;
 *  - незагрузившиеся изображения.
 */
export async function auditLayout(page: Page): Promise<LayoutIssue[]> {
  await page.waitForLoadState('load');
  await page.waitForTimeout(700);
  return page.evaluate(() => {
    const issues: {
      kind: 'horizontal-scroll' | 'clipped' | 'overlap' | 'broken-image';
      detail: string;
    }[] = [];
    const describe = (el: Element) => {
      const text = (el.textContent ?? '').trim().replace(/\s+/g, ' ').slice(0, 40);
      const cls =
        typeof el.className === 'string'
          ? el.className.split(' ').filter(Boolean).slice(0, 2).join('.')
          : '';
      return `${el.tagName.toLowerCase()}${cls ? '.' + cls : ''}${text ? ` «${text}»` : ''}`;
    };
    const visible = (el: Element) => {
      const r = el.getBoundingClientRect();
      const st = getComputedStyle(el);
      return r.width > 0 && r.height > 0 && st.visibility !== 'hidden' && st.display !== 'none';
    };

    const doc = document.documentElement;
    if (doc.scrollWidth > doc.clientWidth + 1) {
      issues.push({
        kind: 'horizontal-scroll',
        detail: `scrollWidth ${doc.scrollWidth} > ${doc.clientWidth}`,
      });
    }

    const targets = Array.from(
      document.querySelectorAll(
        'button, a.btn, .badge, .chip, input:not([type=hidden]):not([type=checkbox]):not([type=radio]), select',
      ),
    ).filter(visible);

    for (const el of targets) {
      const r = el.getBoundingClientRect();
      let p = el.parentElement;
      while (p && p !== document.body) {
        const st = getComputedStyle(p);
        if (/(hidden|auto|scroll|clip)/.test(st.overflowX + st.overflowY)) {
          const pr = p.getBoundingClientRect();
          // элементы внутри прокручиваемых таблиц и меню не считаем ошибкой
          const scrollable = /(auto|scroll)/.test(st.overflowX) && p.scrollWidth > p.clientWidth;
          if (!scrollable && (r.right > pr.right + 1 || r.left < pr.left - 1)) {
            issues.push({
              kind: 'clipped',
              detail: `${describe(el)} обрезан контейнером ${describe(p).slice(0, 40)}`,
            });
          }
          break;
        }
        p = p.parentElement;
      }
    }

    for (let i = 0; i < targets.length; i++) {
      for (let j = i + 1; j < targets.length; j++) {
        const a = targets[i];
        const b = targets[j];
        if (a.contains(b) || b.contains(a)) continue;
        // намеренное наложение: элемент абсолютно позиционирован внутри общего контейнера (кнопка в поле поиска)
        const overlay = (x: Element) => getComputedStyle(x).position === 'absolute';
        if (a.parentElement === b.parentElement && (overlay(a) || overlay(b))) continue;
        const ra = a.getBoundingClientRect();
        const rb = b.getBoundingClientRect();
        const w = Math.min(ra.right, rb.right) - Math.max(ra.left, rb.left);
        const h = Math.min(ra.bottom, rb.bottom) - Math.max(ra.top, rb.top);
        if (w > 2 && h > 2)
          issues.push({ kind: 'overlap', detail: `${describe(a)} ⟷ ${describe(b)}` });
      }
    }

    for (const img of Array.from(document.images).filter(visible)) {
      if (img.complete && img.naturalWidth === 0)
        issues.push({ kind: 'broken-image', detail: img.src });
    }
    return issues.slice(0, 30);
  });
}
