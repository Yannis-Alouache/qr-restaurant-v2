import { Injectable, signal, computed } from '@angular/core';
import { CartEntry, CartStandaloneEntry, CartComboEntry } from '../models/cart.model';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly entries = signal<CartEntry[]>([]);

  readonly cartEntries = computed(() => this.entries());
  readonly itemCount = computed(() => this.entries().reduce((sum, e) => sum + e.quantity, 0));
  readonly total = computed(() => this.entries().reduce((sum, e) => sum + this.entryPrice(e) * e.quantity, 0));
  readonly isEmpty = computed(() => this.entries().length === 0);

  addStandalone(itemId: string, name: string, price: number, imagePath: string | null): void {
    const existing = this.entries().find(e => e.type === 'standalone' && e.itemId === itemId);
    if (existing) {
      this.updateQuantity(existing.cartId, existing.quantity + 1);
      return;
    }
    const entry: CartStandaloneEntry = {
      type: 'standalone',
      cartId: crypto.randomUUID(),
      itemId, name, price, imagePath, quantity: 1,
    };
    this.entries.update(list => [...list, entry]);
  }

  addCombo(
    mainItem: { id: string; name: string; price: number; imagePath: string | null },
    sideItem: { id: string; name: string; supplementPrice: number; imagePath: string | null },
    drinkItem: { id: string; name: string; supplementPrice: number; imagePath: string | null },
  ): void {
    const entry: CartComboEntry = {
      type: 'combo',
      cartId: crypto.randomUUID(),
      mainItem, sideItem, drinkItem,
      quantity: 1,
    };
    this.entries.update(list => [...list, entry]);
  }

  updateQuantity(cartId: string, quantity: number): void {
    if (quantity <= 0) {
      this.remove(cartId);
      return;
    }
    this.entries.update(list =>
      list.map(e => e.cartId === cartId ? { ...e, quantity } : e)
    );
  }

  remove(cartId: string): void {
    this.entries.update(list => list.filter(e => e.cartId !== cartId));
  }

  clear(): void {
    this.entries.set([]);
  }

  private entryPrice(entry: CartEntry): number {
    if (entry.type === 'standalone') return entry.price;
    return entry.mainItem.price
      + entry.sideItem.supplementPrice
      + entry.drinkItem.supplementPrice;
  }
}
