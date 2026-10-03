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

test('admin finds served orders under "Terminées" and gets a refund confirmation', async ({ page, request }) => {
  await page.goto(`${adminBaseUrl}/login`);
  await page.getByTestId('login-email').fill('owner@test.com');
  await page.getByTestId('login-password').fill('Secret123!');
  await page.getByTestId('login-submit').click();

  await page.waitForURL('**/orders');

  const order = await createStandaloneOrder(request);
  await completeCheckout(request, order.id, 'pi_history_admin_123');

  await expect(page.getByTestId(`order-card-${order.id}`)).toBeVisible();

  for (const status of ['En préparation', 'Prête']) {
    await page.getByTestId(`order-advance-${order.id}`).click();
    await expect(page.getByTestId(`order-status-${order.id}`)).toHaveText(status);
  }

  // Dernière transition (servie) : la carte quitte « En cours » sans repasser
  // par un état visible, elle est filtrée aussitôt du board actif.
  await page.getByTestId(`order-advance-${order.id}`).click();
  await expect(page.getByTestId(`order-card-${order.id}`)).toBeHidden();

  // …mais reste consultable dans « Terminées » et « Toutes » —
  // l'historique ne disparaît pas.
  await page.getByTestId('tab-served').click();
  await expect(page.getByTestId(`order-status-${order.id}`)).toHaveText('Servie');
  await expect(page.getByTestId(`order-refund-${order.id}`)).toBeVisible();

  await page.getByTestId('tab-all').click();
  await expect(page.getByTestId(`order-card-${order.id}`)).toBeVisible();

  // Le remboursement demande une confirmation explicite, annulable.
  await page.getByTestId(`order-refund-${order.id}`).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText('Rembourser la commande ?');
  await dialog.getByRole('button', { name: 'Annuler' }).click();
  await expect(dialog).toBeHidden();
});
