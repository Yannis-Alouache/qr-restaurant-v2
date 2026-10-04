import { Injectable, signal, computed } from '@angular/core';
import { CartEntry, CartStandaloneEntry, CartComboEntry } from '../models/cart.model';

const STORAGE_KEY = 'qr-restaurant-cart';

@Injectable({ providedIn: 'root' })
export class CartService {
  // sessionStorage : survit au rafraîchissement de la page, mais disparait avec
  // l'onglet — le client suivant à la même table ne doit pas hériter du panier
  // du précédent (localStorage le lui laisserait).
  private readonly entries = signal<CartEntry[]>(restoreEntries());

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
    this.persist();
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
    this.persist();
  }

  updateQuantity(cartId: string, quantity: number): void {
    if (quantity <= 0) {
      this.remove(cartId);
      return;
    }
    this.entries.update(list =>
      list.map(e => e.cartId === cartId ? { ...e, quantity } : e)
    );
    this.persist();
  }

  remove(cartId: string): void {
    this.entries.update(list => list.filter(e => e.cartId !== cartId));
    this.persist();
  }

  clear(): void {
    this.entries.set([]);
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(STORAGE_KEY);
    }
  }

  private persist(): void {
    if (typeof sessionStorage === 'undefined') return;
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(this.entries()));
  }

  private entryPrice(entry: CartEntry): number {
    if (entry.type === 'standalone') return entry.price;
    return entry.mainItem.price
      + entry.sideItem.supplementPrice
      + entry.drinkItem.supplementPrice;
  }
}

/**
 * Le payload stocké peut dater d'une version antérieure du modèle ou être
 * corrompu : on ne restaure que les entrées dont la forme est reconnue, et on
 * repart d'un panier vide plutôt que de planter les computed (total, count).
 */
function restoreEntries(): CartEntry[] {
  if (typeof sessionStorage === 'undefined') return [];

  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) return [];

  try {
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(isCartEntry);
  } catch {
    return [];
  }
}

function isCartEntry(value: unknown): value is CartEntry {
  return isCartStandaloneEntry(value) || isCartComboEntry(value);
}

function isCartStandaloneEntry(value: unknown): value is CartStandaloneEntry {
  const entry = asPartial<CartStandaloneEntry>(value);
  return entry !== null
    && entry.type === 'standalone'
    && typeof entry.cartId === 'string'
    && typeof entry.itemId === 'string'
    && typeof entry.name === 'string'
    && typeof entry.price === 'number'
    && (typeof entry.imagePath === 'string' || entry.imagePath === null)
    && isValidQuantity(entry.quantity);
}

function isCartComboEntry(value: unknown): value is CartComboEntry {
  const entry = asPartial<CartComboEntry>(value);
  return entry !== null
    && entry.type === 'combo'
    && typeof entry.cartId === 'string'
    && isNamedItem(entry.mainItem)
    && isSupplementItem(entry.sideItem)
    && isSupplementItem(entry.drinkItem)
    && isValidQuantity(entry.quantity);
}

function isNamedItem(value: unknown): value is CartComboEntry['mainItem'] {
  const item = asPartial<CartComboEntry['mainItem']>(value);
  return item !== null
    && typeof item.id === 'string'
    && typeof item.name === 'string'
    && typeof item.price === 'number'
    && (typeof item.imagePath === 'string' || item.imagePath === null);
}

function isSupplementItem(value: unknown): value is CartComboEntry['sideItem'] {
  const item = asPartial<CartComboEntry['sideItem']>(value);
  return item !== null
    && typeof item.id === 'string'
    && typeof item.name === 'string'
    && typeof item.supplementPrice === 'number'
    && (typeof item.imagePath === 'string' || item.imagePath === null);
}

function isValidQuantity(quantity: unknown): quantity is number {
  return typeof quantity === 'number' && Number.isInteger(quantity) && quantity > 0;
}

function asPartial<T>(value: unknown): Partial<T> | null {
  return typeof value === 'object' && value !== null ? value as Partial<T> : null;
}
