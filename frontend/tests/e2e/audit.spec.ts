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

// ── Audit dashboard ───────────────────────────────────────────────────────────

test.describe('audit dashboard', () => {
  test.skip(!hasCredentials, 'Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests');

  test.beforeEach(async ({ page }) => {
    await loginAs(page, TEST_EMAIL!, TEST_PASSWORD!);
    await page.goto('/audit');
  });

  test('renders Audit Dashboard heading', async ({ page }) => {
    await expect(page.getByText('Audit Dashboard')).toBeVisible();
  });

  test('renders Pending Review metric card', async ({ page }) => {
    await expect(page.getByText('Pending Review')).toBeVisible();
  });

  test('renders Under Review metric card', async ({ page }) => {
    await expect(page.getByText('Under Review')).toBeVisible();
  });

  test('renders Approved metric card', async ({ page }) => {
    await expect(page.getByText('Approved')).toBeVisible();
  });

  test('renders filter tabs', async ({ page }) => {
    await expect(page.getByRole('button', { name: /All/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /Pending/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /Under Review/ })).toBeVisible();
  });

  test('renders search input', async ({ page }) => {
    await expect(page.getByPlaceholder(/Search by report ID/i)).toBeVisible();
  });

  test('filter tab "Pending" is clickable and stays on page', async ({ page }) => {
    await page.getByRole('button', { name: /Pending/ }).first().click();
    await expect(page).toHaveURL(/\/audit$/);
  });
});

// ── Account page ──────────────────────────────────────────────────────────────

test.describe('account page', () => {
  test.skip(!hasCredentials, 'Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests');

  test('account page renders after login', async ({ page }) => {
    await loginAs(page, TEST_EMAIL!, TEST_PASSWORD!);
    await page.goto('/account');
    await expect(page).toHaveURL(/\/account$/);
  });
});
