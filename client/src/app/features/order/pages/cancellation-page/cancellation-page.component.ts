import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-cancellation-page',
  standalone: true,
  templateUrl: './cancellation-page.component.html',
  styleUrl: './cancellation-page.component.scss',
})
export class CancellationPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);
  private readonly orderId = this.route.snapshot.paramMap.get('orderId');

  error = signal<string | null>(null);
  retrying = signal(false);

  retryPayment(): void {
    if (!this.orderId) {
      this.error.set('Commande introuvable pour relancer le paiement');
      return;
    }

    this.error.set(null);
    this.retrying.set(true);
    this.orderService.createCheckoutSession(this.orderId).subscribe({
      next: (session) => {
        window.location.href = session.checkoutUrl;
      },
      error: (err) => {
        this.retrying.set(false);
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
