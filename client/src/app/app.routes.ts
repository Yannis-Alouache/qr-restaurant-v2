import { Routes } from '@angular/router';
import { tableIdGuard } from './shared/guards/table-id.guard';

export const routes: Routes = [
  {
    // Écran 1 — grille de catégories
    path: 'menu/:slug/:tableId',
    canActivate: [tableIdGuard],
    loadComponent: () =>
      import('./features/menu/pages/categories-screen/categories-screen.component').then(
        m => m.CategoriesScreenComponent,
      ),
  },
  {
    // Écran 2 — choix Formule Menu / Article solo
    path: 'menu/:slug/:tableId/c/:categoryId',
    canActivate: [tableIdGuard],
    loadComponent: () =>
      import('./features/menu/pages/order-type-screen/order-type-screen.component').then(
        m => m.OrderTypeScreenComponent,
      ),
  },
  {
    // Écran 3 — composition de la Formule Menu (3 étapes)
    path: 'menu/:slug/:tableId/c/:categoryId/menu',
    canActivate: [tableIdGuard],
    loadComponent: () =>
      import('./features/menu/pages/menu-stepper-screen/menu-stepper-screen.component').then(
        m => m.MenuStepperScreenComponent,
      ),
  },
  {
    // Écran 4 — articles solo de la catégorie
    path: 'menu/:slug/:tableId/c/:categoryId/solo',
    canActivate: [tableIdGuard],
    loadComponent: () =>
      import('./features/menu/pages/products-screen/products-screen.component').then(
        m => m.ProductsScreenComponent,
      ),
  },
  {
    // Écran 5 — récapitulatif + paiement Stripe
    path: 'checkout/:slug/:tableId',
    canActivate: [tableIdGuard],
    loadComponent: () =>
      import('./features/order/pages/checkout-page/checkout-page.component').then(
        m => m.CheckoutPageComponent,
      ),
  },
  {
    // Écran bloquant — lien sans UUID de table valide (voir tableIdGuard).
    // Le slug permet d'appliquer le thème du restaurant.
    path: 'qr-invalide/:slug',
    loadComponent: () =>
      import('./features/menu/pages/invalid-qr-screen/invalid-qr-screen.component').then(
        m => m.InvalidQrScreenComponent,
      ),
  },
  {
    path: 'order/:orderId/confirmation',
    loadComponent: () =>
      import('./features/order/pages/confirmation-page/confirmation-page.component').then(
        m => m.ConfirmationPageComponent,
      ),
  },
  {
    path: 'order/:orderId/cancelled',
    loadComponent: () =>
      import('./features/order/pages/cancellation-page/cancellation-page.component').then(
        m => m.CancellationPageComponent,
      ),
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: '',
  },
];
