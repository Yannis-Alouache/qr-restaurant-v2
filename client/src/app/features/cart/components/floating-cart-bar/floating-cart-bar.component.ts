import { Component, output, inject } from '@angular/core';
import { CartService } from '../../services/cart.service';
import { PricePipe } from '../../../../shared/pipes/price.pipe';

@Component({
  selector: 'app-floating-cart-bar',
  standalone: true,
  imports: [PricePipe],
  template: `
    @if (!cart.isEmpty()) {
      <div class="fixed inset-x-0 bottom-0 z-40 px-4 safe-bottom-pad">
        <button class="mx-auto flex w-full max-w-[42rem] items-center justify-between gap-4 rounded-[2rem] border border-white/10 bg-inverse-surface px-5 py-4 text-inverse-on-surface shadow-[var(--shadow-strong)] backdrop-blur-xl active:scale-[0.985] transition-transform"
                (click)="openCart.emit()">
          <div class="flex items-center gap-3 text-left">
            <div class="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-on-primary shadow-lg shadow-primary/25">
              <span class="font-display text-sm font-extrabold">{{ cart.itemCount() }}</span>
            </div>
            <div>
              <span class="block font-display text-sm font-bold">Voir le panier</span>
              <span class="block text-xs text-inverse-on-surface/70">Finaliser la commande</span>
            </div>
          </div>
          <span class="font-display text-lg font-extrabold">{{ cart.total() | price }}</span>
        </button>
      </div>
    }
  `,
})
export class FloatingCartBarComponent {
  openCart = output<void>();
  readonly cart = inject(CartService);
}
