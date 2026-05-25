import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface OrderView {
  id: string;
  tableNumber: number;
  status: string;
  total: number;
  createdAt: string;
  items: OrderItemView[];
}

export interface OrderItemView {
  id: string;
  menuItemId: string;
  name: string;
  quantity: number;
  unitPrice: number;
  menuGroupId: string | null;
  menuRole: string | null;
}

export const STATUS_LABELS: Record<string, string> = {
  nouvelle: 'Nouvelle',
  en_preparation: 'En préparation',
  prete: 'Prête',
  servie: 'Servie',
  en_attente_paiement: 'En attente',
  paiement_echoue: 'Échouée'
};

export const STATUS_ORDER = ['nouvelle', 'en_preparation', 'prete', 'servie'];
export const NEXT_STATUS: Record<string, string> = {
  nouvelle: 'en_preparation',
  en_preparation: 'prete',
  prete: 'servie'
};

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  orders = signal<OrderView[]>([]);

  loadOrders(): Observable<OrderView[]> {
    return this.http.get<OrderView[]>('/api/admin/orders').pipe(
      tap(o => this.orders.set(o))
    );
  }

  updateStatus(orderId: string, status: string): Observable<void> {
    return this.http.patch<void>(`/api/admin/orders/${orderId}/status`, { status });
  }
}
