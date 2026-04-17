import { test, expect } from '@playwright/test';

test('redirects protected route to login when unauthenticated', async ({ page }) => {
  await page.goto('/portfolios');
  await expect(page).toHaveURL(/\/login$/);
});

test('login page renders sign-in action', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('button', { name: 'Sign In' })).toBeVisible();
});

test('signup page renders create account action', async ({ page }) => {
  await page.goto('/signup');
  await expect(page.getByRole('button', { name: 'Create Account' })).toBeVisible();
});
