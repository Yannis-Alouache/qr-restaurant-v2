import { Component, output, inject } from '@angular/core';
import { CartService } from '../../services/cart.service';
import { PricePipe } from '../../../../shared/pipes/price.pipe';

@Component({
  selector: 'app-floating-cart-bar',
  standalone: true,
  imports: [PricePipe],
  template: `
    @if (!cart.isEmpty()) {
      <div class="floating-cart safe-bottom-pad">
        <button class="floating-cart__button" (click)="openCart.emit()">
          <div class="floating-cart__summary">
            <div class="floating-cart__badge">
              <span class="floating-cart__badge-value">{{ cart.itemCount() }}</span>
            </div>
            <div class="floating-cart__copy">
              <span class="floating-cart__label">Voir le panier</span>
              <span class="floating-cart__hint">Prêt à finaliser la commande</span>
            </div>
          </div>
          <div class="floating-cart__total">
            <span class="floating-cart__total-label">Total</span>
            <span class="floating-cart__amount">{{ cart.total() | price }}</span>
          </div>
        </button>
      </div>
    }
  `,
  styles: [`
    .floating-cart {
      position: fixed;
      inset-inline: 0;
      bottom: 0;
      z-index: 40;
      padding-inline: 1rem;
    }

    .floating-cart__button {
      display: flex;
      width: min(100%, var(--app-max-width));
      margin-inline: auto;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      border-radius: 2rem;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 22%, white 78%);
      background: var(--floating-cart-gradient);
      padding: 0.95rem 1rem;
      color: var(--color-inverse-on-surface);
      box-shadow: var(--shadow-strong);
      transition: transform 160ms ease, box-shadow 160ms ease;
    }

    .floating-cart__button:active {
      transform: scale(0.985);
    }

    .floating-cart__summary {
      display: flex;
      align-items: center;
      gap: 0.85rem;
      min-width: 0;
      text-align: left;
    }

    .floating-cart__badge {
      display: inline-grid;
      width: 2.9rem;
      height: 2.9rem;
      flex-shrink: 0;
      place-items: center;
      border-radius: 9999px;
      background:
        linear-gradient(
          135deg,
          color-mix(in srgb, var(--color-primary) 84%, white 16%),
          color-mix(in srgb, var(--color-secondary) 82%, var(--color-primary) 18%)
        );
      color: var(--color-on-primary);
      box-shadow: 0 18px 28px color-mix(in srgb, var(--color-primary) 24%, transparent);
    }

    .floating-cart__badge-value {
      font-family: var(--font-display);
      font-size: 0.95rem;
      font-weight: 800;
    }

    .floating-cart__copy {
      min-width: 0;
    }

    .floating-cart__label {
      display: block;
      font-family: var(--font-display);
      font-size: 1rem;
      font-weight: 800;
    }

    .floating-cart__hint {
      display: block;
      margin-top: 0.15rem;
      color: color-mix(in srgb, var(--color-inverse-on-surface) 70%, transparent);
      font-size: 0.78rem;
      font-weight: 600;
    }

    .floating-cart__total {
      display: flex;
      min-width: 4.75rem;
      flex-direction: column;
      align-items: flex-end;
      gap: 0.15rem;
      text-align: right;
    }

    .floating-cart__total-label {
      color: color-mix(in srgb, var(--color-inverse-on-surface) 64%, transparent);
      font-size: 0.72rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    .floating-cart__amount {
      font-family: var(--font-display);
      font-size: 1.1rem;
      font-weight: 800;
    }
  `],
})
export class FloatingCartBarComponent {
  openCart = output<void>();
  readonly cart = inject(CartService);
}
