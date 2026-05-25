import { APIRequestContext, expect } from '@playwright/test';
import { createHmac } from 'node:crypto';

export const adminBaseUrl = 'http://localhost:4200';
export const clientBaseUrl = 'http://localhost:4300';
const apiBaseUrl = 'http://localhost:8080';
const webhookSecret = 'whsec_test';
const stripeApiVersion = '2026-04-22.dahlia';
const seedOwnerEmail = 'owner@test.com';
const seedOwnerPassword = 'Secret123!';
const tableId = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01';
const brownieId = 'e0eebc99-0001-4ef8-bb6d-6bb9bd380a03';

export async function createStandaloneOrder(request: APIRequestContext) {
  const response = await request.post(`${apiBaseUrl}/api/public/orders`, {
    data: {
      slug: 'naia-burger',
      tableId,
      items: [
        {
          menuItemId: brownieId,
          quantity: 1,
        },
      ],
    },
  });

  expect(response.ok()).toBeTruthy();
  return (await response.json()) as { id: string };
}

export async function completeCheckout(request: APIRequestContext, orderId: string, paymentIntentId: string) {
  const payload = JSON.stringify({
    id: 'evt_checkout_completed_browser',
    object: 'event',
    api_version: stripeApiVersion,
    type: 'checkout.session.completed',
    data: {
      object: {
        id: `cs_browser_${orderId}`,
        object: 'checkout.session',
        payment_intent: paymentIntentId,
        metadata: {
          order_id: orderId,
        },
      },
    },
  });

  const response = await request.post(`${apiBaseUrl}/api/webhooks/stripe`, {
    headers: {
      'Content-Type': 'application/json',
      'Stripe-Signature': stripeSignature(payload),
    },
    data: payload,
  });

  expect(response.ok()).toBeTruthy();
}

export async function advanceOrderToPreparation(request: APIRequestContext, orderId: string) {
  const token = await loginSeedOwner(request);
  const response = await request.patch(`${apiBaseUrl}/api/admin/orders/${orderId}/status`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    data: {
      status: 'en_preparation',
    },
  });

  expect(response.status()).toBe(204);
}

async function loginSeedOwner(request: APIRequestContext) {
  const response = await request.post(`${apiBaseUrl}/api/auth/login`, {
    data: {
      email: seedOwnerEmail,
      password: seedOwnerPassword,
    },
  });

  expect(response.ok()).toBeTruthy();
  const body = (await response.json()) as { token: string };
  return body.token;
}

function stripeSignature(payload: string) {
  const timestamp = Math.floor(Date.now() / 1000);
  const signature = createHmac('sha256', webhookSecret)
    .update(`${timestamp}.${payload}`)
    .digest('hex');
  return `t=${timestamp},v1=${signature}`;
}
