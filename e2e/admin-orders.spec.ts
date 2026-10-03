import { expect, test } from '@playwright/test';
import { adminBaseUrl, completeCheckout, createStandaloneOrder } from './support/api';

test('admin receives a paid order in real time and can advance it', async ({ page, request }) => {
  await page.goto(`${adminBaseUrl}/login`);
  await page.getByTestId('login-email').fill('owner@test.com');
  await page.getByTestId('login-password').fill('Secret123!');
  await page.getByTestId('login-submit').click();

  // Le login aboutit directement sur la liste des commandes (restaurant déjà créé).
  await page.waitForURL('**/orders');

  const order = await createStandaloneOrder(request);
  await completeCheckout(request, order.id, 'pi_browser_admin_123');

  await expect(page.getByTestId(`order-card-${order.id}`)).toBeVisible();
  await expect(page.getByTestId(`order-status-${order.id}`)).toHaveText('Nouvelle');

  await page.getByTestId(`order-advance-${order.id}`).click();

  await expect(page.getByTestId(`order-status-${order.id}`)).toHaveText('En préparation');
});
