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
  templateUrl: './confirmation-page.component.html',
  styleUrl: './confirmation-page.component.scss',
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
  private pollTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.orderId = this.route.snapshot.paramMap.get('orderId') ?? '';
    if (!this.orderId) {
      this.error.set('Identifiant de commande manquant');
      this.loading.set(false);
      return;
    }

    this.loadOrder();
    this.orderStatusRealtime.connect(
      this.orderId,
      (status) => {
        this.order.update((currentOrder) =>
          currentOrder ? { ...currentOrder, status } : currentOrder,
        );
      },
      // Rattrape toute transition émise avant l'établissement du WebSocket.
      () => this.loadOrder(),
    );
  }

  ngOnDestroy(): void {
    this.stopPaymentPolling();
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
        if (o.status === 'en_attente_paiement') {
          this.startPaymentPolling();
        } else {
          this.stopPaymentPolling();
        }
      },
      error: () => {
        this.error.set('Impossible de charger la commande');
        this.loading.set(false);
      },
    });
  }

  /**
   * Filet de sécurité pendant l'attente du webhook de paiement : si le
   * WebSocket manque la transition (connexion lente ou silencieusement
   * morte), un rafraîchissement périodique rattrape l'état réel.
   */
  private startPaymentPolling(): void {
    if (this.pollTimer !== null) return;
    this.pollTimer = setInterval(() => {
      if (this.order()?.status === 'en_attente_paiement') {
        this.loadOrder();
      } else {
        this.stopPaymentPolling();
      }
    }, 4000);
  }

  private stopPaymentPolling(): void {
    if (this.pollTimer !== null) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }
}
