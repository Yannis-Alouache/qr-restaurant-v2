import { Component, inject, OnInit, OnDestroy, computed, effect, signal } from '@angular/core';
import {
  OrderService,
  OrderView,
  STATUS_LABELS,
  NEXT_STATUS,
  STATUS_ORDER,
} from '../../core/services/order.service';
import { RestaurantService } from '../../core/services/restaurant.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { ToastService } from '../../core/services/toast.service';
import { ADMIN_ICONS } from '../../core/icons';

type Filter = 'active' | 'served' | 'all';

interface StatusConfig {
  stripe: string;
  action: { label: string; cls: string } | null;
}

/** Visual treatment per order status — extends the mockup's 3-status model to the API's 4. */
const STATUS_CONFIG: Record<string, StatusConfig> = {
  nouvelle: { stripe: 'var(--accent)', action: { label: 'Accepter', cls: 'btn-action-primary' } },
  en_preparation: { stripe: 'var(--warn)', action: { label: 'Prête', cls: 'btn-action-success' } },
  prete: { stripe: 'var(--success)', action: { label: 'Servie', cls: 'btn-action-success' } },
  servie: { stripe: 'var(--muted)', action: null },
};

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

  private readonly handleRealtimeOrderUpdate = ({ orderId }: { orderId: string }) => {
    if (orderId) {
      this.orderService.loadOrders().subscribe();
    }
  };

  constructor() {
    // Open the realtime channel as soon as the restaurant is known.
    effect(() => {
      if (this.restaurant.restaurant()) {
        this.ws.connect(this.handleRealtimeOrderUpdate);
      }
    });
  }

  orders = this.orderService.orders;
  connected = this.ws.connected;
  statusLabels = STATUS_LABELS;
  filter = signal<Filter>('active');

  counts = computed(() => {
    const orders = this.orders();
    return {
      active: orders.filter(o => o.status !== 'servie').length,
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
      if (f === 'active') return o.status !== 'servie';
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
    this.ws.disconnect();
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

  trackById(_: number, o: OrderView): string {
    return o.id;
  }

  formatPrice(price: number): string {
    return price.toFixed(2).replace('.', ',') + ' €';
  }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}
