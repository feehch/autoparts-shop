import { defineConfig, devices } from '@playwright/test';

/**
 * E2E-тесты всего сайта. Запускаются против уже поднятого стека (docker compose up),
 * браузер — установленный в системе Google Chrome (скачивание браузеров Playwright не требуется).
 *
 *   npx playwright test                 # все тесты
 *   E2E_BASE_URL=http://localhost:4200 npx playwright test e2e/flows.spec.ts
 */
export default defineConfig({
  testDir: './e2e',
  outputDir: './e2e-results/artifacts',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { outputFolder: './e2e-results/report', open: 'never' }]],
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    channel: 'chrome',
    locale: 'ru-RU',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], channel: 'chrome', viewport: { width: 1440, height: 900 } } },
    { name: 'mobile', use: { ...devices['Pixel 7'], channel: 'chrome' } },
  ],
});
