import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Requires a valid session (httpOnly cookie). Verifies via /api/auth/me when unknown. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.resolveSession().pipe(map(ok => (ok ? true : router.parseUrl('/login'))));
};

/** Guest-only pages (login, signup). Bounces authed users to the dashboard. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.resolveSession().pipe(map(ok => (ok ? router.parseUrl('/orders') : true)));
};
