import { expect, test, type APIRequestContext } from '@playwright/test';
import { adminBaseUrl } from './support/api';

const apiBaseUrl = 'http://localhost:8080';
const mailpitBaseUrl = 'http://localhost:8025';

interface MailpitMessage {
  ID: string;
  To: { Address: string }[];
}

async function fetchResetLink(request: APIRequestContext, email: string): Promise<string> {
  const deadline = Date.now() + 20_000;

  while (Date.now() < deadline) {
    const list = await (await request.get(`${mailpitBaseUrl}/api/v1/messages?limit=25`)).json() as {
      messages?: MailpitMessage[];
    };
    const message = (list.messages ?? []).find(m => m.To.some(t => t.Address === email));

    if (message) {
      const detail = await (await request.get(`${mailpitBaseUrl}/api/v1/message/${message.ID}`)).json() as {
        HTML?: string;
      };
      const match = String(detail.HTML ?? '').match(/https?:\/\/[^"'\s]+\/reset-password\?token=[A-Za-z0-9_-]+/);
      const link = match ? match[0] : '';
      if (link) {
        return link;
      }
    }

    await new Promise(resolve => setTimeout(resolve, 1_000));
  }

  throw new Error(`Email de réinitialisation introuvable dans Mailpit pour ${email}`);
}

test('a forgotten password can be reset from the email link and is effective immediately', async ({ page, request }) => {
  const email = `reset-${Date.now()}@example.com`;

  // Le compte existe en base : la demande de réinitialisation doit déclencher l'email.
  const signup = await request.post(`${apiBaseUrl}/api/auth/signup`, {
    data: { email, password: 'Secret123!' },
  });
  expect(signup.ok()).toBeTruthy();

  await page.goto(`${adminBaseUrl}/login`);
  await page.getByTestId('login-forgot-password').click();
  await page.getByTestId('forgot-email').fill(email);
  await page.getByTestId('forgot-submit').click();
  await expect(page.getByTestId('forgot-success')).toBeVisible();

  const resetLink = await fetchResetLink(request, email);

  await page.goto(resetLink);
  await page.getByTestId('reset-password').fill('NewSecret123!');
  await page.getByTestId('reset-confirm').fill('NewSecret123!');
  await page.getByTestId('reset-submit').click();
  await expect(page.getByTestId('reset-success')).toBeVisible();

  // Effectif immédiatement : le lien proposé mène à la reconnexion, l'ancien
  // mot de passe est refusé, le nouveau est accepté.
  await page.getByTestId('reset-login-link').click();
  await page.getByTestId('login-email').fill(email);
  await page.getByTestId('login-password').fill('Secret123!');
  await page.getByTestId('login-submit').click();
  await expect(page.getByRole('alert')).toContainText('Identifiants invalides');

  await page.getByTestId('login-password').fill('NewSecret123!');
  await page.getByTestId('login-submit').click();
  await page.waitForURL('**/onboarding');
});
