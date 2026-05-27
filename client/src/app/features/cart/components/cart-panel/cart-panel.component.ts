import { Component, output, inject } from '@angular/core';
import { CartService } from '../../services/cart.service';
import { CartEntry } from '../../models/cart.model';
import { PricePipe } from '../../../../shared/pipes/price.pipe';

@Component({
  selector: 'app-cart-panel',
  standalone: true,
  imports: [PricePipe],
  template: `
    <div class="sheet-backdrop" (click)="close.emit()">
      <div class="sheet-backdrop__scrim"></div>
      <div
        class="cart-sheet animate-slide-up safe-bottom-pad"
        role="dialog"
        aria-modal="true"
        aria-labelledby="cart-panel-title"
        (click)="$event.stopPropagation()">
        <div class="cart-sheet__handle"></div>

        <div class="cart-sheet__header">
          <div>
            <h2 id="cart-panel-title" class="cart-sheet__title">
              Votre panier
              <span class="cart-sheet__count">({{ cart.itemCount() }})</span>
            </h2>
            <p class="cart-sheet__subtitle">Retrouvez vos choix avant validation.</p>
          </div>
          <button class="cart-sheet__close" type="button" aria-label="Fermer le panier" (click)="close.emit()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <div class="cart-sheet__content">
          @if (cart.isEmpty()) {
            <div class="cart-sheet__empty">
              <p class="cart-sheet__empty-emoji">🛒</p>
              <p class="cart-sheet__empty-title">Votre panier est vide</p>
              <p class="cart-sheet__empty-text">Ajoutez un plat pour commencer votre commande.</p>
            </div>
          } @else {
            @for (entry of cart.cartEntries(); track entry.cartId) {
              <div class="cart-entry">
                <div class="cart-entry__media">
                  @if (entry.type === 'standalone' && entry.imagePath) {
                    <img [src]="entry.imagePath" [alt]="entry.name" class="cart-entry__image" />
                  } @else if (entry.type === 'combo' && entry.mainItem.imagePath) {
                    <img [src]="entry.mainItem.imagePath" [alt]="entry.mainItem.name" class="cart-entry__image" />
                  } @else {
                    <div class="cart-entry__placeholder">🍽️</div>
                  }
                </div>

                <div class="cart-entry__details">
                  <p class="cart-entry__name">{{ entryName(entry) }}</p>
                  @if (entry.type === 'combo') {
                    <p class="cart-entry__composition">
                      {{ entry.sideItem.name }} + {{ entry.drinkItem.name }}
                    </p>
                  }
                  <p class="cart-entry__price">{{ entryPrice(entry) | price }}</p>
                </div>

                <div class="cart-entry__quantity">
                  <button class="cart-entry__quantity-button cart-entry__quantity-button--muted"
                          type="button"
                          [attr.aria-label]="'Retirer un article : ' + entryName(entry)"
                          (click)="cart.updateQuantity(entry.cartId, entry.quantity - 1)">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/></svg>
                  </button>
                  <span class="cart-entry__quantity-value">{{ entry.quantity }}</span>
                  <button class="cart-entry__quantity-button cart-entry__quantity-button--primary"
                          type="button"
                          [attr.aria-label]="'Ajouter un article : ' + entryName(entry)"
                          (click)="cart.updateQuantity(entry.cartId, entry.quantity + 1)">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                  </button>
                </div>
              </div>
            }
          }
        </div>

        @if (!cart.isEmpty()) {
          <div class="cart-sheet__footer">
            <div class="cart-sheet__total">
              <span class="cart-sheet__total-label">Total</span>
              <span class="cart-sheet__total-value">{{ cart.total() | price }}</span>
            </div>
            <button class="cart-sheet__order-button" (click)="order.emit()">
              Commander
            </button>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .sheet-backdrop {
      position: fixed;
      inset: 0;
      z-index: 50;
      display: flex;
      align-items: flex-end;
      justify-content: center;
    }

    .sheet-backdrop__scrim {
      position: absolute;
      inset: 0;
      background: rgba(14, 10, 8, 0.52);
      backdrop-filter: blur(16px);
    }

    .cart-sheet {
      position: relative;
      display: flex;
      max-height: 92vh;
      width: min(100%, var(--app-max-width));
      flex-direction: column;
      overflow: hidden;
      border-radius: 2.3rem 2.3rem 0 0;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 70%, white 30%);
      background:
        linear-gradient(
          180deg,
          color-mix(in srgb, var(--color-surface-container-lowest) 92%, white 8%) 0%,
          color-mix(in srgb, var(--color-surface-container-low) 96%, var(--color-primary) 4%) 100%
        );
      box-shadow: var(--shadow-strong);
    }

    .cart-sheet__handle {
      width: 3.4rem;
      height: 0.34rem;
      margin: 0.8rem auto 0;
      border-radius: 9999px;
      background: color-mix(in srgb, var(--color-outline) 52%, white 48%);
    }

    .cart-sheet__header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 1rem;
      padding: 1rem 1rem 0.9rem;
    }

    .cart-sheet__title {
      font-family: var(--font-display);
      font-size: 1.45rem;
      font-weight: 800;
      line-height: 1.05;
      color: var(--color-on-surface);
    }

    .cart-sheet__count {
      color: var(--color-on-surface-variant);
      font-size: 1rem;
      font-weight: 700;
    }

    .cart-sheet__subtitle {
      margin-top: 0.35rem;
      color: var(--color-on-surface-variant);
      font-size: 0.9rem;
      line-height: 1.45;
    }

    .cart-sheet__close {
      display: inline-grid;
      width: 2.35rem;
      height: 2.35rem;
      place-items: center;
      flex-shrink: 0;
      border-radius: 9999px;
      background: color-mix(in srgb, var(--color-surface-container-lowest) 70%, white 30%);
      color: var(--color-on-surface-variant);
      box-shadow: 0 14px 20px rgba(0, 0, 0, 0.1);
    }

    .cart-sheet__content {
      flex: 1;
      overflow-y: auto;
      padding: 0.25rem 1rem 1rem;
    }

    .cart-sheet__empty {
      border-radius: 1.8rem;
      background: color-mix(in srgb, var(--color-surface-container-lowest) 84%, white 16%);
      padding: 2rem 1rem;
      text-align: center;
      box-shadow: 0 14px 26px rgba(0, 0, 0, 0.06);
    }

    .cart-sheet__empty-emoji {
      font-size: 2.7rem;
    }

    .cart-sheet__empty-title {
      margin-top: 0.7rem;
      font-family: var(--font-display);
      font-size: 1.1rem;
      font-weight: 800;
      color: var(--color-on-surface);
    }

    .cart-sheet__empty-text {
      margin-top: 0.35rem;
      color: var(--color-on-surface-variant);
      font-size: 0.9rem;
      line-height: 1.5;
    }

    .cart-entry {
      display: flex;
      align-items: center;
      gap: 0.85rem;
      border-radius: 1.6rem;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 72%, white 28%);
      background:
        linear-gradient(
          180deg,
          color-mix(in srgb, var(--color-surface-container-lowest) 88%, white 12%) 0%,
          color-mix(in srgb, var(--color-surface-container-low) 96%, var(--color-primary) 4%) 100%
        );
      padding: 0.8rem;
      box-shadow: 0 14px 26px rgba(0, 0, 0, 0.06);
    }

    .cart-entry + .cart-entry {
      margin-top: 0.8rem;
    }

    .cart-entry__media {
      width: 3.7rem;
      height: 3.7rem;
      overflow: hidden;
      flex-shrink: 0;
      border-radius: 1.1rem;
      background:
        radial-gradient(circle at top, color-mix(in srgb, var(--color-secondary-container) 42%, transparent), transparent 58%),
        color-mix(in srgb, var(--color-primary) 10%, var(--color-surface-container-high) 90%);
    }

    .cart-entry__image,
    .cart-entry__placeholder {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .cart-entry__placeholder {
      display: grid;
      place-items: center;
      font-size: 1.25rem;
    }

    .cart-entry__details {
      flex: 1;
      min-width: 0;
    }

    .cart-entry__name {
      font-family: var(--font-display);
      font-size: 0.95rem;
      font-weight: 700;
      line-height: 1.25;
      color: var(--color-on-surface);
    }

    .cart-entry__composition {
      margin-top: 0.2rem;
      color: var(--color-on-surface-variant);
      font-size: 0.76rem;
      line-height: 1.35;
    }

    .cart-entry__price {
      margin-top: 0.4rem;
      color: var(--color-primary);
      font-family: var(--font-display);
      font-size: 0.92rem;
      font-weight: 800;
    }

    .cart-entry__quantity {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      flex-shrink: 0;
    }

    .cart-entry__quantity-button {
      display: inline-grid;
      width: 2rem;
      height: 2rem;
      place-items: center;
      border-radius: 9999px;
      transition: transform 160ms ease;
    }

    .cart-entry__quantity-button:active {
      transform: scale(0.95);
    }

    .cart-entry__quantity-button--muted {
      background: color-mix(in srgb, var(--color-surface-container-high) 78%, white 22%);
      color: var(--color-on-surface-variant);
    }

    .cart-entry__quantity-button--primary {
      background:
        linear-gradient(
          135deg,
          color-mix(in srgb, var(--color-primary) 84%, white 16%),
          color-mix(in srgb, var(--color-secondary) 84%, var(--color-primary) 16%)
        );
      color: var(--color-on-primary);
      box-shadow: 0 14px 24px color-mix(in srgb, var(--color-primary) 20%, transparent);
    }

    .cart-entry__quantity-value {
      width: 1.4rem;
      text-align: center;
      font-family: var(--font-display);
      font-size: 0.92rem;
      font-weight: 800;
      color: var(--color-on-surface);
    }

    .cart-sheet__footer {
      border-top: 1px solid color-mix(in srgb, var(--color-outline-variant) 70%, white 30%);
      background: color-mix(in srgb, var(--color-surface-container-lowest) 86%, white 14%);
      padding: 1rem 1rem 0.5rem;
    }

    .cart-sheet__total {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 0.85rem;
    }

    .cart-sheet__total-label {
      color: var(--color-on-surface-variant);
      font-size: 0.88rem;
      font-weight: 700;
    }

    .cart-sheet__total-value {
      font-family: var(--font-display);
      font-size: 1.3rem;
      font-weight: 800;
      color: var(--color-on-surface);
    }

    .cart-sheet__order-button {
      width: 100%;
      min-height: 3.45rem;
      border-radius: 9999px;
      padding: 0 1rem;
      background:
        linear-gradient(
          135deg,
          color-mix(in srgb, var(--color-primary) 84%, white 16%),
          color-mix(in srgb, var(--color-secondary) 84%, var(--color-primary) 16%)
        );
      color: var(--color-on-primary);
      font-family: var(--font-display);
      font-size: 1rem;
      font-weight: 800;
      box-shadow: 0 22px 34px color-mix(in srgb, var(--color-primary) 24%, transparent);
      transition: transform 160ms ease;
    }

    .cart-sheet__order-button:active {
      transform: scale(0.985);
    }

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
