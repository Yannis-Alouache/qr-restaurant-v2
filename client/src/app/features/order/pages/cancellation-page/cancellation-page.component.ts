import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-cancellation-page',
  standalone: true,
  template: `
    <div class="min-h-screen bg-surface px-4 py-8 sm:px-6">
      <div class="mx-auto flex min-h-[calc(100dvh-4rem)] w-full max-w-[32rem] items-center justify-center">
        <div class="w-full rounded-[2rem] border border-outline-variant/60 bg-surface-container-low p-8 text-center shadow-[var(--shadow-soft)]">
          <div class="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-error/10">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ba1a1a" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
          </div>
          <h1 class="mb-2 font-display text-2xl font-bold text-on-surface">Paiement annulé</h1>
          <p class="mb-8 text-sm text-on-surface-variant">
            Votre paiement n'a pas été finalisé. Votre commande est conservée et vous pouvez relancer le paiement.
          </p>
          @if (error()) {
            <p class="mb-6 text-sm text-error">{{ error() }}</p>
          }
          <div class="flex flex-col items-center justify-center gap-3 sm:flex-row">
            <button class="rounded-full bg-primary px-6 py-3 text-sm font-bold text-on-primary active:scale-[0.98] transition-transform"
                  (click)="retryPayment()">
              Réessayer le paiement
            </button>
            <button class="rounded-full border border-outline px-6 py-3 text-sm font-bold text-on-surface active:scale-[0.98] transition-transform"
                  (click)="goBack()">
              Retour au menu
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class CancellationPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);
  private readonly orderId = this.route.snapshot.paramMap.get('orderId');

  error = signal<string | null>(null);

  retryPayment(): void {
    if (!this.orderId) {
      this.error.set('Commande introuvable pour relancer le paiement');
      return;
    }

    this.error.set(null);
    this.orderService.createCheckoutSession(this.orderId).subscribe({
      next: (session) => {
        window.location.href = session.checkoutUrl;
      },
      error: (err) => {
        this.error.set(this.extractErrorMessage(err));
      },
    });
  }

  goBack(): void {
    history.back();
  }

  private extractErrorMessage(error: unknown): string {
    const apiError = error as { error?: { message?: string; error?: string } };
    return apiError.error?.message ?? apiError.error?.error ?? 'Erreur lors de la création du paiement';
  }
}
