import { expect, test } from '@playwright/test';
import {
  advanceOrderToPreparation,
  clientBaseUrl,
  completeCheckout,
  createStandaloneOrder,
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
