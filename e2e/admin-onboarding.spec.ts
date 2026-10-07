import { expect, test } from '@playwright/test';
import { adminBaseUrl } from './support/api';

test('a restaurateur signs up, configures their restaurant and reaches the menu', async ({ page }) => {
  // Compte jetable : l'email est unique par run (unicité côté API), la base e2e
  // persistant entre les runs locaux.
  const email = `e2e.onboarding.${Date.now()}@test.com`;

  await page.goto(`${adminBaseUrl}/signup`);
  await page.locator('#email').fill(email);
  await page.locator('#password').fill('Password123!');
  await page.locator('#confirm-password').fill('Password123!');
  await page.getByRole('button', { name: 'Créer mon compte' }).click();

  // L'inscription enchaîne sur l'étape 2 : configuration du restaurant.
  await page.waitForURL('**/onboarding');
  await expect(page.getByRole('heading', { name: 'Créer votre restaurant' })).toBeVisible();
  await page.locator('#restaurant-name').fill('E2E Bistrot');
  await page.getByRole('button', { name: 'Continuer' }).click();

  // Étape 3 : le restaurant existe, la gestion du menu s'ouvre — la session
  // (cookie JWT) et le compte ont survécu à toute la chaîne signup → API → BDD.
  await page.waitForURL('**/menu');
  await expect(page.getByRole('heading', { name: 'Créer le menu' })).toBeVisible();
});
