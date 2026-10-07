import { expect, test } from '@playwright/test';
import { clientBaseUrl } from './support/api';

test('an unknown URL lands on the 404 screen instead of a blank page', async ({ page }) => {
  await page.goto(`${clientBaseUrl}/this-page-does-not-exist`);

  await expect(page).toHaveURL(/\/404$/);
  await expect(page.getByTestId('not-found-title')).toHaveText('Page introuvable');
});

test('the root URL, which has no home screen, redirects to the 404 screen', async ({ page }) => {
  await page.goto(`${clientBaseUrl}/`);

  await expect(page).toHaveURL(/\/404$/);
  await expect(page.getByTestId('not-found-title')).toHaveText('Page introuvable');
});
