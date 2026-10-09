import { expect, test } from '@playwright/test';
import {
  advanceOrderToPreparation,
  advanceOrderToServed,
  clientBaseUrl,
  completeCheckout,
  configureGoogleReviewUrl,
  createStandaloneOrder,
  tableId,
} from './support/api';

test('customer confirmation page reflects payment and kitchen progress without reload', async ({
  page,
  request,
}) => {
  const order = await createStandaloneOrder(request);

  await page.goto(`${clientBaseUrl}/order/${order.id}/confirmation`);
  await expect(page.getByTestId('confirmation-title')).toHaveText('Paiement en cours de confirmation');

  await completeCheckout(request, order.id, 'pi_browser_client_123');
  await expect(page.getByTestId('confirmation-title')).toHaveText('Commande confirmée !');

  await advanceOrderToPreparation(request, order.id);
  await expect(page.locator('[data-testid="status-step-en_preparation"] svg')).toHaveCount(1);
});

test('served order shows the Google review card configured in the back office', async ({
  page,
  request,
}) => {
  const reviewUrl = 'https://g.page/r/naia-burger-e2e/review';
  await configureGoogleReviewUrl(request, reviewUrl);

  const order = await createStandaloneOrder(request);
  await completeCheckout(request, order.id, 'pi_browser_reviews_123');
  await advanceOrderToServed(request, order.id);

  // Le slug de la table est mémorisé côté navigateur au moment du paiement
  // (ReviewPromptService) : on simule ici l'état du navigateur du client.
  await page.addInitScript(
    (context) => sessionStorage.setItem('menzo:review-prompt', context),
    JSON.stringify({ slug: 'naia-burger', tableId, orderId: order.id, status: 'nouvelle' }),
  );

  await page.goto(`${clientBaseUrl}/order/${order.id}/confirmation`);

  const card = page.getByTestId('google-review-card');
  await expect(card).toBeVisible();
  await expect(card.locator('a.review-button')).toHaveAttribute('href', reviewUrl);
});
