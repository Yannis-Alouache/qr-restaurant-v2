import { Component, effect, inject, input, output, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { CartService } from '../../../features/cart/services/cart.service';
import { CartEntry } from '../../../features/cart/models/cart.model';
import { PricePipe } from '../../pipes/price.pipe';

/** Panier en bottom-sheet — copie pixel de la maquette, fermeture au swipe. */
@Component({
  selector: 'app-cart-drawer',
  templateUrl: './cart-drawer.component.html',
  styleUrl: './cart-drawer.component.scss',
  imports: [PricePipe],
})
export class CartDrawerComponent {
  readonly cart = inject(CartService);
  private readonly document = inject(DOCUMENT);

  open = input(false);
  readonly closed = output<void>();
  readonly pay = output<void>();

  /** Décalage vertical pendant le drag du handle (null = pas de drag). */
  readonly dragOffset = signal<number | null>(null);

  private startY = 0;
  private dragging = false;

  constructor() {
    effect(() => {
      this.document.body.style.overflow = this.open() ? 'hidden' : '';
    });
  }

  onTouchStart(event: TouchEvent): void {
    this.startY = event.touches[0].clientY;
    this.dragging = true;
  }

  onTouchMove(event: TouchEvent): void {
    if (!this.dragging) return;
    const dy = event.touches[0].clientY - this.startY;
    this.dragOffset.set(dy < 0 ? 0 : dy);
  }

  onTouchEnd(): void {
    if (!this.dragging) return;
    this.dragging = false;
    const dy = this.dragOffset() ?? 0;
    this.dragOffset.set(null);
    if (dy > 80) {
      this.closed.emit();
    }
  }

  entryName(entry: CartEntry): string {
    return entry.type === 'standalone' ? entry.name : entry.mainItem.name;
  }

  entryImg(entry: CartEntry): string | null {
    if (entry.type === 'standalone') return entry.imagePath;
    return entry.mainItem.imagePath;
  }

  /** Ligne de détail affichée sous le nom (composition du menu). */
  entryDetail(entry: CartEntry): string | null {
    if (entry.type !== 'combo') return null;
    return `${entry.sideItem.name} + ${entry.drinkItem.name}`;
  }

  entryPrice(entry: CartEntry): number {
    if (entry.type === 'standalone') return entry.price;
    return entry.mainItem.price + entry.sideItem.supplementPrice + entry.drinkItem.supplementPrice;
  }
}
