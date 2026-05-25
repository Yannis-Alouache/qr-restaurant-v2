import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OrderResponse,
  OrderDetailResponse,
  CheckoutSessionResponse,
} from '../../menu/models/menu.model';
import { CartEntry } from '../../cart/models/cart.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly apiUrl = '/api/public';

  constructor(private http: HttpClient) {}

  createOrder(
    slug: string,
    tableId: string,
    entries: CartEntry[],
  ): Observable<OrderResponse> {
    const items = entries.flatMap(entry => this.toOrderItems(entry));
    return this.http.post<OrderResponse>(`${this.apiUrl}/orders`, {
      slug,
      tableId,
      items,
    });
  }

  getOrder(orderId: string): Observable<OrderDetailResponse> {
    return this.http.get<OrderDetailResponse>(`${this.apiUrl}/orders/${orderId}`);
  }

  createCheckoutSession(orderId: string): Observable<CheckoutSessionResponse> {
    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/payments/checkout`, {
      orderId,
    });
  }

  private toOrderItems(entry: CartEntry): OrderItemRequest[] {
    if (entry.type === 'standalone') {
      return [{
        menuItemId: entry.itemId,
        quantity: entry.quantity,
        menuGroupId: null,
        menuRole: null,
      }];
    }

    const groupId = entry.cartId;
    return [
      { menuItemId: entry.mainItem.id, quantity: entry.quantity, menuGroupId: groupId, menuRole: 'plat' },
      { menuItemId: entry.sideItem.id, quantity: entry.quantity, menuGroupId: groupId, menuRole: 'accompagnement' },
      { menuItemId: entry.drinkItem.id, quantity: entry.quantity, menuGroupId: groupId, menuRole: 'boisson' },
    ];
  }
}

interface OrderItemRequest {
  menuItemId: string;
  quantity: number;
  menuGroupId: string | null;
  menuRole: string | null;
}
