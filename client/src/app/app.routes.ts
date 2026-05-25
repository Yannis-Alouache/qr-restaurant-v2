import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'menu/:slug/:tableId',
    loadComponent: () => import('./features/menu/pages/menu-page/menu-page.component').then(m => m.MenuPageComponent),
  },
  {
    path: 'checkout/:slug/:tableId',
    loadComponent: () => import('./features/order/pages/checkout-page/checkout-page.component').then(m => m.CheckoutPageComponent),
  },
  {
    path: 'order/:orderId/confirmation',
    loadComponent: () => import('./features/order/pages/confirmation-page/confirmation-page.component').then(m => m.ConfirmationPageComponent),
  },
  {
    path: 'order/:orderId/cancelled',
    loadComponent: () => import('./features/order/pages/cancellation-page/cancellation-page.component').then(m => m.CancellationPageComponent),
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: '',
  },
];
