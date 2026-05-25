import { Component, inject, OnInit, OnDestroy, computed, effect } from '@angular/core';
import {
  OrderService,
  OrderView,
  STATUS_LABELS,
  NEXT_STATUS,
  STATUS_ORDER,
} from '../../core/services/order.service';
import { RestaurantService } from '../../core/services/restaurant.service';
import { WebSocketService } from '../../core/services/websocket.service';

@Component({
  selector: 'app-orders',
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.scss',
})
export class OrdersComponent implements OnInit, OnDestroy {
  private orderService = inject(OrderService);
  private restaurant = inject(RestaurantService);
  private ws = inject(WebSocketService);
  private readonly handleRealtimeOrderUpdate = ({ orderId }: { orderId: string }) => {
    if (orderId) {
      this.orderService.loadOrders().subscribe();
    }
  };
  private readonly syncRealtimeConnection = effect(() => {
    if (!this.restaurant.restaurant()) {
      return;
    }

    this.ws.connect(this.handleRealtimeOrderUpdate);
  });

  orders = this.orderService.orders;
  connected = this.ws.connected;

  activeOrders = computed(() =>
    this.orders()
      .filter((o) => STATUS_ORDER.includes(o.status) && o.status !== 'servie')
      .sort((a, b) => STATUS_ORDER.indexOf(a.status) - STATUS_ORDER.indexOf(b.status)),
  );

  ordersByTable = computed(() => {
    const grouped = new Map<number, OrderView[]>();
    for (const order of this.activeOrders()) {
      const table = order.tableNumber;
      if (!grouped.has(table)) grouped.set(table, []);
      grouped.get(table)!.push(order);
    }
    return grouped;
  });

  statusLabels = STATUS_LABELS;
  nextStatus = NEXT_STATUS;

  ngOnInit(): void {
    this.orderService.loadOrders().subscribe();
  }

  ngOnDestroy(): void {
    this.ws.disconnect();
  }

  advanceStatus(orderId: string, currentStatus: string): void {
    const next = NEXT_STATUS[currentStatus];
    if (!next) return;

    this.orderService.updateStatus(orderId, next).subscribe({
      next: () => this.orderService.loadOrders().subscribe(),
      error: (err) => alert(err.error?.error ?? 'Erreur'),
    });
  }

  formatPrice(price: number): string {
    return price.toFixed(2).replace('.', ',') + ' €';
  }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'nouvelle':
        return 'bg-sunset-orange text-canvas-white';
      case 'en_preparation':
        return 'bg-data-gold text-canvas-white';
      case 'prete':
        return 'bg-dark-shale text-canvas-white';
      default:
        return 'bg-slate-mist text-dark-shale';
    }
  }
}
