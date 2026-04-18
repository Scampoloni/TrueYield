import { test, expect } from '@playwright/test';

// ── Redirect: all protected routes → /login ──────────────────────────────────

const PROTECTED_ROUTES = ['/', '/portfolios', '/holdings', '/audit', '/account'];

for (const route of PROTECTED_ROUTES) {
  test(`unauthenticated: ${route} redirects to /login`, async ({ page }) => {
    await page.goto(route);
    await expect(page).toHaveURL(/\/login$/);
  });
}

test('unauthenticated: /portfolios sub-route redirects to /login', async ({ page }) => {
  await page.goto('/portfolios/some-id');
  await expect(page).toHaveURL(/\/login$/);
});

test('unauthenticated: /audit sub-route redirects to /login', async ({ page }) => {
  await page.goto('/audit/some-id');
  await expect(page).toHaveURL(/\/login$/);
});

// ── Login page: structure ─────────────────────────────────────────────────────

test('login page: renders email and password inputs', async ({ page }) => {
  await page.goto('/login');
  await expect(page.locator('#email')).toBeVisible();
  await expect(page.locator('#password')).toBeVisible();
});

test('login page: renders Sign In button', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('button', { name: 'Sign In' })).toBeVisible();
});

test('login page: Sign In button is enabled when page loads', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('button', { name: 'Sign In' })).toBeEnabled();
});

test('login page: has link to signup page', async ({ page }) => {
  await page.goto('/login');
  const signupLink = page.getByRole('link', { name: 'Sign up' });
  await expect(signupLink).toBeVisible();
  await expect(signupLink).toHaveAttribute('href', '/signup');
});

test('login page: shows TrueYield brand', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByText('TrueYield')).toBeVisible();
});

// ── Signup page: structure ────────────────────────────────────────────────────

test('signup page: renders create account action', async ({ page }) => {
  await page.goto('/signup');
  await expect(page.getByRole('button', { name: 'Create Account' })).toBeVisible();
});

test('signup page: renders email and password inputs', async ({ page }) => {
  await page.goto('/signup');
  await expect(page.locator('#email')).toBeVisible();
  await expect(page.locator('#password')).toBeVisible();
});

// ── Authenticated flows (require TEST_USER_* env vars) ───────────────────────

const TEST_EMAIL = process.env.E2E_TEST_EMAIL;
const TEST_PASSWORD = process.env.E2E_TEST_PASSWORD;
const hasCredentials = Boolean(TEST_EMAIL && TEST_PASSWORD);

test.describe('authenticated flows', () => {
  test.skip(!hasCredentials, 'Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run authenticated tests');

  test('login with valid credentials redirects to home', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#email', TEST_EMAIL!);
    await page.fill('#password', TEST_PASSWORD!);
    await page.getByRole('button', { name: 'Sign In' }).click();
    await page.waitForURL(/^\//);
    await expect(page).not.toHaveURL(/\/login$/);
  });

  test('after login: portfolios page is accessible', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#email', TEST_EMAIL!);
    await page.fill('#password', TEST_PASSWORD!);
    await page.getByRole('button', { name: 'Sign In' }).click();
    await page.waitForURL(/^\//);
    await page.goto('/portfolios');
    await expect(page).toHaveURL(/\/portfolios$/);
    await expect(page.getByText('Portfolios')).toBeVisible();
  });
});
