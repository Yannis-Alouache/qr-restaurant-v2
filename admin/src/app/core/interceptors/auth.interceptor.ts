import { HttpInterceptorFn } from '@angular/common/http';

/**
 * The JWT travels in an httpOnly cookie (see api PR #10), so the browser must
 * send credentials on every API call. We don't touch the Authorization header.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('/api/')) {
    return next(req.clone({ withCredentials: true }));
  }
  return next(req);
};
