import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Les QR codes générés par l'admin encodent l'UUID de la table dans l'URL
 * (/menu/:slug/:tableId). Tout autre format (numéro de table saisi à la main,
 * lien corrompu…) est bloqué ici, avant que l'utilisateur ne puisse remplir
 * un panier voué à échouer au moment de la commande. Le slug est conservé
 * dans la redirection pour que l'écran bloquant puisse appliquer le thème
 * choisi par l'admin du restaurant.
 */
export const tableIdGuard: CanActivateFn = (route) => {
  const tableId = route.paramMap.get('tableId') ?? '';
  if (UUID_PATTERN.test(tableId)) {
    return true;
  }
  const slug = route.paramMap.get('slug') ?? '';
  return inject(Router).createUrlTree(['/qr-invalide', slug]);
};
