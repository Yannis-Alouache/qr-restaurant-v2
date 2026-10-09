import { Component, inject, OnInit, OnDestroy, computed, effect, signal } from '@angular/core';
import {
  OrderService,
  OrderView,
  STATUS_LABELS,
  NEXT_STATUS,
  STATUS_ORDER,
  TERMINAL_STATUSES,
} from '../../core/services/order.service';
import { RestaurantService } from '../../core/services/restaurant.service';
import { WebSocketService, WebSocketMessage } from '../../core/services/websocket.service';
import { KitchenAlertService } from '../../core/services/kitchen-alert.service';
import { ToastService } from '../../core/services/toast.service';
import { ConfirmService } from '../../core/services/confirm.service';
import { ADMIN_ICONS } from '../../core/icons';

type Filter = 'active' | 'served' | 'all';

interface StatusConfig {
  /** Classe d'état posée sur la carte : pilote bande, pastille et teintes via --card-accent. */
  cls: string;
  action: { label: string; cls: string } | null;
}

/** Visual treatment per order status — extends the mockup's 3-status model to the API's 5. */
const STATUS_CONFIG: Record<string, StatusConfig> = {
  nouvelle: { cls: 'st-nouvelle', action: { label: 'Accepter', cls: 'btn-action-primary' } },
  en_preparation: { cls: 'st-preparation', action: { label: 'Prête', cls: 'btn-action-success' } },
  prete: { cls: 'st-prete', action: { label: 'Servie', cls: 'btn-action-success' } },
  servie: { cls: 'st-servie', action: null },
  en_attente_paiement: { cls: 'st-attente', action: null },
  paiement_echoue: { cls: 'st-echec', action: null },
  rembourse: { cls: 'st-rembourse', action: null },
};

/** Miroir de Order.assertRefundable côté domaine : payée non servie ou servie. */
const REFUNDABLE_STATUSES = ['nouvelle', 'servie'];

@Component({
  selector: 'app-orders',
  imports: [...ADMIN_ICONS],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.scss',
})
export class OrdersComponent implements OnInit, OnDestroy {
  private orderService = inject(OrderService);
  private restaurant = inject(RestaurantService);
  private ws = inject(WebSocketService);
  private toast = inject(ToastService);
  private confirm = inject(ConfirmService);

  /** Exposé au template pour l'état du bouton « Son ». */
  readonly alerts = inject(KitchenAlertService);

  private readonly baseTitle = document.title;

  private readonly handleRealtimeOrderUpdate = ({ orderId, status }: WebSocketMessage) => {
    if (!orderId) {
      return;
    }
    // Les détails (table, montant) ne voyagent pas dans le message STOMP :
    // on les récupère sur la liste fraîchement chargée pour la notification.
    const afterLoad = status === 'nouvelle'
      ? (orders: OrderView[]) => {
          const order = orders.find(o => o.id === orderId);
          this.alerts.onNewOrder(order
            ? { tableNumber: order.tableNumber, total: order.total }
            : undefined);
        }
      : undefined;
    this.orderService.loadOrders().subscribe(afterLoad);
  };

  constructor() {
    // Open the realtime channel as soon as the restaurant is known.
    effect(() => {
      if (this.restaurant.restaurant()) {
        this.ws.connect(this.handleRealtimeOrderUpdate);
      }
    });
    // Rattrapage : un événement diffusé pendant l'établissement de la
    // souscription (navigation → STOMP ready) est perdu sans ce refetch.
    effect(() => {
      if (this.ws.connected()) {
        this.orderService.loadOrders().subscribe();
      }
    });
    // Le rappel sonore cesse dès que plus aucune commande « nouvelle » n'attend,
    // et le titre de l'onglet garde le compteur visible en arrière-plan.
    effect(() => {
      const pending = this.counts().nouvelle;
      this.alerts.setPendingNewOrders(pending);
      document.title = pending > 0 ? `(${pending}) ${this.baseTitle}` : this.baseTitle;
    });
  }

  orders = this.orderService.orders;
  connected = this.ws.connected;
  statusLabels = STATUS_LABELS;
  filter = signal<Filter>('active');

  /** Horloge partagée : rafraîchit les temps d'attente sans recalcul manuel. */
  readonly now = signal(Date.now());
  private readonly clock: ReturnType<typeof setInterval> = setInterval(
    () => this.now.set(Date.now()),
    30_000,
  );

  counts = computed(() => {
    const orders = this.orders();
    return {
      active: orders.filter(o => !TERMINAL_STATUSES.includes(o.status)).length,
      served: orders.filter(o => o.status === 'servie').length,
      all: orders.length,
      nouvelle: orders.filter(o => o.status === 'nouvelle').length,
      preparation: orders.filter(o => o.status === 'en_preparation').length,
      prete: orders.filter(o => o.status === 'prete').length,
    };
  });

  filteredOrders = computed(() => {
    const f = this.filter();
    const list = this.orders().filter(o => {
      if (f === 'active') return !TERMINAL_STATUSES.includes(o.status);
      if (f === 'served') return o.status === 'servie';
      return true;
    });
    return [...list].sort(
      (a, b) => STATUS_ORDER.indexOf(a.status) - STATUS_ORDER.indexOf(b.status),
    );
  });

  ngOnInit(): void {
    this.orderService.loadOrders().subscribe();
  }

  ngOnDestroy(): void {
    clearInterval(this.clock);
    this.ws.disconnect();
    document.title = this.baseTitle;
  }

  async toggleAlerts(): Promise<void> {
    if (this.alerts.enabled()) {
      this.alerts.disable();
      this.toast.show('Alertes cuisine désactivées');
      return;
    }
    const notificationsGranted = await this.alerts.enable();
    this.toast.show(notificationsGranted
      ? 'Alertes cuisine activées : son et notifications'
      : 'Alertes sonores activées (notifications refusées par le navigateur)');
  }

  setFilter(f: Filter): void {
    this.filter.set(f);
  }

  statusConfig(status: string): StatusConfig {
    return STATUS_CONFIG[status] ?? STATUS_CONFIG['servie'];
  }

  advanceStatus(order: OrderView): void {
    const next = NEXT_STATUS[order.status];
    if (!next) return;

    this.orderService.updateStatus(order.id, next).subscribe({
      next: () => {
        this.orderService.loadOrders().subscribe();
        this.toast.show(`Table ${order.tableNumber} → ${STATUS_LABELS[next]}`);
      },
      error: (err) => this.toast.show(err.error?.message ?? err.error?.error ?? 'Erreur'),
    });
  }

  canRefund(order: OrderView): boolean {
    return REFUNDABLE_STATUSES.includes(order.status);
  }

  async refundOrder(order: OrderView): Promise<void> {
    const confirmed = await this.confirm.ask({
      title: 'Rembourser la commande ?',
      message: `Table ${order.tableNumber}, ${this.formatPrice(order.total)}. `
        + 'Le client sera intégralement remboursé, cette action est définitive.',
      confirmLabel: 'Rembourser',
      tone: 'danger',
    });
    if (!confirmed) return;

    this.orderService.refund(order.id).subscribe({
      next: () => {
        this.orderService.loadOrders().subscribe();
        this.toast.show(`Table ${order.tableNumber} remboursée`);
      },
      error: (err) => this.toast.show(err.error?.message ?? err.error?.error ?? 'Erreur'),
    });
  }

  trackById(_: number, o: OrderView): string {
    return o.id;
  }

  formatPrice(price: number): string {
    return price.toFixed(2).replace('.', ',') + ' €';
  }

  /** Temps écoulé depuis la prise de commande, arrondi au service près. */
  formatElapsed(iso: string): string {
    const minutes = Math.max(0, Math.floor((this.now() - new Date(iso).getTime()) / 60_000));
    if (minutes < 1) return "à l'instant";
    if (minutes < 60) return `${minutes} min`;
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest > 0 ? `${hours} h ${String(rest).padStart(2, '0')}` : `${hours} h`;
  }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}
