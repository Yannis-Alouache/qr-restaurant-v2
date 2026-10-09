import { expect, test } from '@playwright/test';
import { advanceOrderToServed, clientBaseUrl, completeCheckout, tableId } from './support/api';

// Vrai Stripe Checkout hébergé : impossible sans clé secrète de test réelle.
// Le harnais détecte la clé dans le .env et exporte le flag (voir
// scripts/run-e2e.sh) ; sans elle, le spec s'efface au lieu d'échouer.
test.skip(process.env.E2E_STRIPE_REAL !== 'true', 'clé Stripe test réelle absente');

test('customer journey from the menu to a real Stripe hosted payment', async ({ page, request }) => {
  // Redirection vers Stripe puis saisie du formulaire hébergé : on élargit le
  // délai du test au-delà des 60 s par défaut.
  test.setTimeout(120_000);

  // Écran 1 — grille de catégories, point d'entrée du QR posé sur la table.
  await page.goto(`${clientBaseUrl}/menu/naia-burger/${tableId}`);
  await page.locator('.category-item', { hasText: 'Burgers' }).click();

  // Écran 2 — formule ou article à l'unité.
  await page.locator('.split-card', { hasText: 'Article solo' }).click();

  // Écran 4 — fiche produit, puis ajout au panier : l'ajout ramène à la grille
  // de catégories. On attend cette navigation avant d'ouvrir le panier, sinon
  // le tiroir ouvert serait détruit avec l'écran quitté.
  await page.locator('button.product-item').first().click();
  await page.getByRole('button', { name: 'Ajouter au panier' }).click();
  await page.waitForURL(new RegExp(`/menu/naia-burger/${tableId}$`));

  // Tiroir panier : le client relit son récapitulatif avant de payer.
  await page.getByRole('button', { name: 'Voir panier' }).click();
  const drawer = page.locator('.drawer.open');
  await expect(drawer).toBeVisible();
  await drawer.getByRole('button', { name: 'Aller au paiement' }).click();
  await expect(page).toHaveURL(new RegExp(`/checkout/naia-burger/${tableId}$`));

  // Création de la commande puis ouverture du Stripe Checkout hébergé : la
  // session est réelle (clé de test), créée par l'API comme en production.
  await page.getByRole('button', { name: /Payer/ }).click();
  await page.waitForURL(/checkout\.stripe\.com/, { timeout: 30_000 });

  // Formulaire hébergé par checkout.stripe.com : la langue de la page varie
  // (sélecteurs par id et data-testid Stripe, stables quelle que soit la locale).
  await page.locator('#email').fill('client@e2e-qr-restaurant.test');

  // Sélection du moyen « Carte » : la ligne est recouverte par la zone de clic
  // de l'accordéon Stripe, qui fait échouer la vérification de cible du clic.
  await page.getByText(/^(carte|card)$/i).click({ force: true });
  await page.locator('#cardNumber').fill('4242 4242 4242 4242');
  await page.locator('#cardExpiry').fill('12/34');
  await page.locator('#cardCvc').fill('424');
  await page.locator('#billingName').fill('Client E2E');
  await page.getByTestId('hosted-payment-submit-button').click();

  // Retour sur la success_url de l'app : le paiement a abouti chez Stripe, la
  // commande attend désormais le webhook checkout.session.completed.
  await page.waitForURL(/\/order\/[0-9a-f-]{36}\/confirmation/, { timeout: 30_000 });
  const orderId = page.url().match(/order\/([0-9a-f-]{36})\/confirmation/)![1];
  await expect(page.getByTestId('confirmation-title')).toHaveText('Paiement en cours de confirmation');

  // Livraison de l'événement avec le mécanisme signé des autres specs : seul le
  // transport du webhook est simulé, le paiement, lui, a réellement eu lieu.
  await completeCheckout(request, orderId, `pi_e2e_${orderId}`);
  await expect(page.getByTestId('confirmation-title')).toHaveText('Commande confirmée !');

  // Ménage : laissée à l'état « nouvelle », la commande gonflerait en permanence
  // le badge du titre de l'admin ouvert par les specs parallèles.
  await advanceOrderToServed(request, orderId);
});
