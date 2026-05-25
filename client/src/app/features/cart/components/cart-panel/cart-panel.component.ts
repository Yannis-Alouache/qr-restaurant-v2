import { Component, output, inject } from '@angular/core';
import { CartService } from '../../services/cart.service';
import { CartEntry } from '../../models/cart.model';
import { PricePipe } from '../../../../shared/pipes/price.pipe';

@Component({
  selector: 'app-cart-panel',
  standalone: true,
  imports: [PricePipe],
  template: `
    <div class="fixed inset-0 z-50 flex items-end justify-center" (click)="close.emit()">
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
      <div class="relative flex max-h-[85vh] w-full max-w-[42rem] flex-col overflow-hidden rounded-t-[2rem] border border-outline-variant/60 bg-surface-container-low shadow-[var(--shadow-strong)] animate-slide-up safe-bottom-pad"
           (click)="$event.stopPropagation()">
        <div class="mx-auto mt-3 h-1.5 w-14 rounded-full bg-outline-variant"></div>

        <!-- Header -->
        <div class="flex items-center justify-between border-b border-outline-variant/70 px-5 pb-3 pt-4 sm:px-6">
          <h2 class="font-display text-lg font-bold text-on-surface">
            Votre panier
            <span class="text-on-surface-variant font-body text-sm ml-1">({{ cart.itemCount() }})</span>
          </h2>
          <button class="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center text-on-surface-variant"
                  (click)="close.emit()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <!-- Items -->
        <div class="flex-1 overflow-y-auto space-y-4 px-5 py-5 sm:px-6">
          @if (cart.isEmpty()) {
            <div class="text-center py-8">
              <p class="text-4xl mb-3">🛒</p>
              <p class="font-body text-on-surface-variant">Votre panier est vide</p>
            </div>
          } @else {
            @for (entry of cart.cartEntries(); track entry.cartId) {
              <div class="flex items-start gap-3">
                <!-- Image -->
                <div class="w-12 h-12 rounded-xl bg-surface-container flex-shrink-0 overflow-hidden">
                  @if (entry.type === 'standalone' && entry.imagePath) {
                    <img [src]="entry.imagePath" [alt]="entry.name" class="w-full h-full object-cover" />
                  } @else if (entry.type === 'combo' && entry.mainItem.imagePath) {
                    <img [src]="entry.mainItem.imagePath" [alt]="entry.mainItem.name" class="w-full h-full object-cover" />
                  } @else {
                    <div class="w-full h-full flex items-center justify-center text-lg">🍽️</div>
                  }
                </div>

                <!-- Details -->
                <div class="flex-1 min-w-0">
                  <p class="font-display font-semibold text-on-surface text-sm">{{ entryName(entry) }}</p>
                  @if (entry.type === 'combo') {
                    <p class="font-body text-on-surface-variant text-xs mt-0.5">
                      {{ entry.sideItem.name }} + {{ entry.drinkItem.name }}
                    </p>
                  }
                  <p class="font-display font-extrabold text-primary text-sm mt-1">{{ entryPrice(entry) | price }}</p>
                </div>

                <!-- Quantity controls -->
                <div class="flex items-center gap-2">
                  <button class="w-7 h-7 rounded-full bg-surface-container flex items-center justify-center text-on-surface-variant active:scale-95 transition-transform"
                          (click)="cart.updateQuantity(entry.cartId, entry.quantity - 1)">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/></svg>
                  </button>
                  <span class="font-display font-bold text-on-surface text-sm w-5 text-center">{{ entry.quantity }}</span>
                  <button class="w-7 h-7 rounded-full bg-primary flex items-center justify-center text-on-primary active:scale-95 transition-transform"
                          (click)="cart.updateQuantity(entry.cartId, entry.quantity + 1)">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                  </button>
                </div>
              </div>
            }
          }
        </div>

        <!-- Footer -->
        @if (!cart.isEmpty()) {
          <div class="border-t border-outline-variant/70 bg-surface-container px-5 pb-1 pt-4 sm:px-6">
            <div class="flex items-center justify-between mb-4">
              <span class="font-body text-on-surface-variant">Total</span>
              <span class="font-display font-extrabold text-on-surface text-xl">{{ cart.total() | price }}</span>
            </div>
            <button class="w-full py-3.5 rounded-full bg-primary text-on-primary font-display font-bold text-base active:scale-[0.98] transition-transform"
                    (click)="order.emit()">
              Commander
            </button>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .animate-slide-up {
      animation: slideUp 0.3s ease-out;
    }
    @keyframes slideUp {
      from { transform: translateY(100%); }
      to { transform: translateY(0); }
    }
  `],
})
export class CartPanelComponent {
  close = output<void>();
  order = output<void>();

  readonly cart = inject(CartService);

  entryName(entry: CartEntry): string {
    return entry.type === 'standalone' ? entry.name : entry.mainItem.name;
  }

  entryPrice(entry: CartEntry): number {
    if (entry.type === 'standalone') return entry.price * entry.quantity;
    return (entry.mainItem.price + entry.sideItem.supplementPrice + entry.drinkItem.supplementPrice) * entry.quantity;
  }
}
