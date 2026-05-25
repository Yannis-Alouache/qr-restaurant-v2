import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { OrderStatusRealtimeService } from '../../services/order-status-realtime.service';
import { OrderDetailResponse } from '../../../menu/models/menu.model';
import { PricePipe } from '../../../../shared/pipes/price.pipe';

@Component({
  selector: 'app-confirmation-page',
  standalone: true,
  imports: [PricePipe],
  template: `
    <div class="min-h-screen bg-surface px-4 py-8 sm:px-6">
      <div
        class="mx-auto flex min-h-[calc(100dvh-4rem)] w-full max-w-[36rem] items-center justify-center"
      >
        @if (loading()) {
          <div
            class="w-full rounded-[2rem] border border-outline-variant/60 bg-surface-container-low p-8 text-center shadow-[var(--shadow-soft)]"
          >
            <div
              class="w-10 h-10 border-3 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-4"
            ></div>
            <p class="font-body text-on-surface-variant">Chargement de votre commande...</p>
          </div>
        } @else if (error()) {
          <div
            class="w-full rounded-[2rem] border border-outline-variant/60 bg-surface-container-low p-8 text-center shadow-[var(--shadow-soft)]"
          >
            <div class="text-5xl mb-4">😕</div>
            <h1 class="font-display text-xl font-bold text-on-surface mb-2">
              Commande introuvable
            </h1>
            <p class="font-body text-on-surface-variant text-sm">{{ error() }}</p>
          </div>
        } @else if (order()) {
          <div class="w-full text-center">
            <!-- Success icon -->
            <div
              class="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-primary/10 shadow-lg shadow-primary/10"
            >
              <svg
                width="40"
                height="40"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#a03b00"
                stroke-width="2"
                stroke-linecap="round"
              >
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
            </div>

            <h1 class="font-display text-2xl font-bold text-on-surface mb-2">
              {{ pageTitle() }}
            </h1>
            <p class="font-body text-on-surface-variant text-sm mb-8">
              {{ pageDescription() }}
            </p>

            <!-- Status Timeline -->
            <div
              class="mb-6 rounded-[2rem] border border-outline-variant/50 bg-surface-container-low p-6 text-left shadow-[var(--shadow-soft)]"
            >
              <h3 class="font-display font-bold text-on-surface text-sm mb-4">
                Suivi de votre commande
              </h3>
              <div class="space-y-4">
                @for (status of statusSteps; track status.key) {
                  <div class="flex items-center gap-3">
                    <div
                      class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
                      [class]="
                        isStatusReached(status.key) ? 'bg-primary' : 'bg-surface-container-high'
                      "
                    >
                      @if (isStatusReached(status.key)) {
                        <svg
                          width="14"
                          height="14"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="white"
                          stroke-width="3"
                          stroke-linecap="round"
                        >
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                      }
                    </div>
                    <span
                      class="font-body text-sm"
                      [class]="
                        isStatusReached(status.key)
                          ? 'text-on-surface font-semibold'
                          : 'text-on-surface-variant'
                      "
                    >
                      {{ status.label }}
                    </span>
                  </div>
                }
              </div>
            </div>

            <!-- Order Summary -->
            <div
              class="rounded-[2rem] border border-outline-variant/50 bg-surface-container-low p-6 text-left shadow-[var(--shadow-soft)]"
            >
              <h3 class="font-display font-bold text-on-surface text-sm mb-3">Récapitulatif</h3>
              @for (item of order()!.items; track item.id) {
                <div class="flex justify-between py-1.5">
                  <span class="font-body text-sm text-on-surface"
                    >{{ item.quantity }}x {{ item.name }}</span
                  >
                  <span class="font-display font-bold text-sm text-on-surface">{{
                    item.unitPrice * item.quantity | price
                  }}</span>
                </div>
              }
              <div class="border-t border-outline-variant mt-3 pt-3 flex justify-between">
                <span class="font-display font-bold text-on-surface">Total</span>
                <span class="font-display font-extrabold text-primary">{{
                  order()!.total | price
                }}</span>
              </div>
            </div>

            <p class="font-body text-on-surface-variant text-xs mt-6">
              Cette page se met à jour automatiquement
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
export class ConfirmationPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);
  private readonly orderStatusRealtime = inject(OrderStatusRealtimeService);

  order = signal<OrderDetailResponse | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  readonly statusSteps = [
    { key: 'nouvelle', label: 'Confirmée' },
    { key: 'en_preparation', label: 'En préparation' },
    { key: 'prete', label: 'Prête' },
    { key: 'servie', label: 'Servie' },
  ];

  private orderId = '';

  ngOnInit(): void {
    this.orderId = this.route.snapshot.paramMap.get('orderId') ?? '';
    if (!this.orderId) {
      this.error.set('Identifiant de commande manquant');
      this.loading.set(false);
      return;
    }

    this.loadOrder();
    this.orderStatusRealtime.connect(this.orderId, (status) => {
      this.order.update((currentOrder) =>
        currentOrder ? { ...currentOrder, status } : currentOrder,
      );
    });
  }

  ngOnDestroy(): void {
    this.orderStatusRealtime.disconnect();
  }

  pageTitle(): string {
    const status = this.order()?.status;
    if (status === 'en_attente_paiement') {
      return 'Paiement en cours de confirmation';
    }
    if (status === 'paiement_echoue') {
      return 'Paiement non finalisé';
    }
    return 'Commande confirmée !';
  }

  pageDescription(): string {
    const status = this.order()?.status;
    if (status === 'en_attente_paiement') {
      return 'Nous attendons la confirmation du paiement. Cette page se met à jour automatiquement.';
    }
    if (status === 'paiement_echoue') {
      return 'Le paiement a échoué ou a expiré. Retournez au menu pour relancer le paiement.';
    }
    return 'Votre commande a été enregistrée avec succès';
  }

  isStatusReached(status: string): boolean {
    const current = this.order()?.status ?? '';
    const order = ['nouvelle', 'en_preparation', 'prete', 'servie'];
    return order.indexOf(current) >= order.indexOf(status);
  }

  private loadOrder(): void {
    this.orderService.getOrder(this.orderId).subscribe({
      next: (o) => {
        this.order.set(o);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger la commande');
        this.loading.set(false);
      },
    });
  }
}
