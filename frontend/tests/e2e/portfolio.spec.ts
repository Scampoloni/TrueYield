import { test, expect, type Page } from '@playwright/test';

// ── Helpers ───────────────────────────────────────────────────────────────────

const TEST_EMAIL = process.env.E2E_TEST_EMAIL;
const TEST_PASSWORD = process.env.E2E_TEST_PASSWORD;
const hasCredentials = Boolean(TEST_EMAIL && TEST_PASSWORD);

async function loginAs(page: Page, email: string, password: string) {
  await page.goto('/login');
  await page.fill('#email', email);
  await page.fill('#password', password);
  await page.getByRole('button', { name: 'Sign In' }).click();
  await page.waitForURL(/^(?!.*\/login)/);
}

// ── Portfolio create page: accessible without rendering backend ───────────────
// These tests require auth but test UI structure, not backend integration.

test.describe('portfolio create form', () => {
  test.skip(!hasCredentials, 'Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests');

  test.beforeEach(async ({ page }) => {
    await loginAs(page, TEST_EMAIL!, TEST_PASSWORD!);
    await page.goto('/portfolios/create');
  });

  test('renders portfolio name and description inputs', async ({ page }) => {
    await expect(page.locator('#name')).toBeVisible();
    await expect(page.locator('#description')).toBeVisible();
  });

  test('renders Create Portfolio submit button', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Create Portfolio' })).toBeVisible();
  });

  test('shows validation error when name is empty', async ({ page }) => {
    await page.getByRole('button', { name: 'Create Portfolio' }).click();
    await expect(page.getByText('Portfolio name is required.')).toBeVisible();
  });

  test('shows validation error when name is whitespace only', async ({ page }) => {
    await page.fill('#name', '   ');
    await page.getByRole('button', { name: 'Create Portfolio' }).click();
    await expect(page.getByText('Portfolio name is required.')).toBeVisible();
  });

  test('cancel button navigates back to portfolios list', async ({ page }) => {
    await page.getByRole('link', { name: '← Cancel' }).click();
    await expect(page).toHaveURL(/\/portfolios$/);
  });
});

// ── Portfolio list page ───────────────────────────────────────────────────────

test.describe('portfolios list page', () => {
  test.skip(!hasCredentials, 'Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests');

  test.beforeEach(async ({ page }) => {
    await loginAs(page, TEST_EMAIL!, TEST_PASSWORD!);
    await page.goto('/portfolios');
  });

  test('renders Portfolios page heading', async ({ page }) => {
    await expect(page.getByText('Portfolios')).toBeVisible();
  });

  test('fund-manager role sees New Portfolio button', async ({ page }) => {
    // Only assert visible — whether it appears depends on role assignment
    const isFundManager = TEST_EMAIL?.includes('manager') ||
                          process.env.E2E_TEST_ROLE === 'fund-manager';
    if (isFundManager) {
      await expect(page.getByRole('link', { name: /New Portfolio/i })).toBeVisible();
    }
  });
});

// ── Holdings page ─────────────────────────────────────────────────────────────

test.describe('holdings page', () => {
  test.skip(!hasCredentials, 'Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests');

  test('holdings page renders after login', async ({ page }) => {
    await loginAs(page, TEST_EMAIL!, TEST_PASSWORD!);
    await page.goto('/holdings');
    await expect(page).toHaveURL(/\/holdings$/);
    await expect(page.getByText('Holdings')).toBeVisible();
  });
});
