import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { CartService } from '../../../cart/services/cart.service';

@Component({
  selector: 'app-checkout-page',
  standalone: true,
  template: `
    <div class="min-h-screen bg-surface px-4 py-8 sm:px-6">
      <div
        class="mx-auto flex min-h-[calc(100dvh-4rem)] w-full max-w-[32rem] items-center justify-center"
      >
        @if (error()) {
          <div
            class="w-full rounded-[2rem] border border-outline-variant/60 bg-surface-container-low p-8 text-center shadow-[var(--shadow-soft)]"
          >
            <div class="mb-4 text-5xl">😕</div>
            <h1 class="mb-2 font-display text-2xl font-bold text-on-surface">
              Une erreur est survenue
            </h1>
            <p class="mb-6 text-sm text-on-surface-variant">{{ error() }}</p>
            <div class="flex flex-col items-center justify-center gap-3 sm:flex-row">
              @if (createdOrderId()) {
                <button
                  class="rounded-full bg-primary px-6 py-3 text-sm font-bold text-on-primary"
                  (click)="retryPayment()"
                >
                  Réessayer le paiement
                </button>
              }
              <button
                class="rounded-full border border-outline px-6 py-3 text-sm font-bold text-on-surface"
                (click)="goBack()"
              >
                Retour au menu
              </button>
            </div>
          </div>
        } @else {
          <div
            class="w-full rounded-[2rem] border border-outline-variant/60 bg-surface-container-low p-8 text-center shadow-[var(--shadow-soft)]"
          >
            <div
              class="mx-auto mb-5 flex h-20 w-20 items-center justify-center rounded-full bg-primary/10"
            >
              <div
                class="h-10 w-10 rounded-full border-[3px] border-primary border-t-transparent animate-spin"
              ></div>
            </div>
            <h1 class="font-display text-2xl font-bold text-on-surface">
              Redirection vers le paiement
            </h1>
            <p class="mt-2 text-sm text-on-surface-variant">
              Vérification de votre commande et ouverture du paiement sécurisé…
            </p>
          </div>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .animate-spin {
        animation: spin 1s linear infinite;
      }
      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }
    `,
  ],
})
export class CheckoutPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);
  private readonly cart = inject(CartService);

  error = signal<string | null>(null);
  createdOrderId = signal<string | null>(null);

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    const tableId = this.route.snapshot.paramMap.get('tableId');

    if (!slug || !tableId) {
      this.error.set('Informations manquantes');
      return;
    }

    if (this.cart.isEmpty()) {
      this.error.set('Votre panier est vide');
      return;
    }

    const entries = this.cart.cartEntries();

    this.orderService.createOrder(slug, tableId, entries).subscribe({
      next: (order) => {
        this.createdOrderId.set(order.id);
        this.openCheckout(order.id);
      },
      error: (err) => {
        this.error.set(this.extractErrorMessage(err, 'Erreur lors de la création de la commande'));
      },
    });
  }

  retryPayment(): void {
    const orderId = this.createdOrderId();
    if (!orderId) {
      return;
    }
    this.openCheckout(orderId);
  }

  goBack(): void {
    history.back();
  }

  private openCheckout(orderId: string): void {
    this.error.set(null);
    this.orderService.createCheckoutSession(orderId).subscribe({
      next: (session) => {
        this.cart.clear();
        window.location.href = session.checkoutUrl;
      },
      error: (err) => {
        this.error.set(this.extractErrorMessage(err, 'Erreur lors de la création du paiement'));
      },
    });
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const apiError = error as { error?: { message?: string; error?: string } };
    return apiError.error?.message ?? apiError.error?.error ?? fallback;
  }
}
