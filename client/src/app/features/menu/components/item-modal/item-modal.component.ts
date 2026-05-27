import { Component, computed, inject, input, output } from '@angular/core';
import { ItemView } from '../../models/menu.model';
import { CartService } from '../../../cart/services/cart.service';
import { PricePipe } from '../../../../shared/pipes/price.pipe';

@Component({
  selector: 'app-item-modal',
  standalone: true,
  imports: [PricePipe],
  template: `
    <div class="sheet-backdrop" (click)="close.emit()">
      <div class="sheet-backdrop__scrim"></div>
      <div
        class="item-sheet animate-slide-up safe-bottom-pad"
        role="dialog"
        aria-modal="true"
        aria-labelledby="item-modal-title"
        (click)="$event.stopPropagation()">
        <div class="item-sheet__handle"></div>
        <button class="item-sheet__close" type="button" aria-label="Fermer la fiche produit" (click)="close.emit()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>

        <div class="item-sheet__media">
          @if (item().imagePath) {
            <img [src]="item().imagePath!" [alt]="item().name" class="item-sheet__image" />
          } @else {
            <div class="item-sheet__placeholder">{{ itemInitial() }}</div>
          }
          <div class="item-sheet__media-scrim"></div>
          <div class="item-sheet__badges">
            <span class="item-sheet__badge">Fiche produit</span>
            @if (showMenuChoice()) {
              <span class="item-sheet__badge item-sheet__badge--accent">Composer en menu</span>
            }
          </div>
        </div>

        <div class="item-sheet__content">
          <div class="item-sheet__header">
            <div>
              <p class="item-sheet__eyebrow">À commander</p>
              <h2 id="item-modal-title" class="item-sheet__title">{{ item().name }}</h2>
            </div>
            <span class="item-sheet__price">{{ item().price | price }}</span>
          </div>

        @if (item().description) {
            <p class="item-sheet__description">{{ item().description }}</p>
        }
          <p class="item-sheet__note">
            {{ showMenuChoice() ? 'Disponible seul ou en formule complète.' : 'Ajout rapide à votre table.' }}
          </p>

          @if (showMenuChoice()) {
            <div class="item-sheet__actions">
              <button class="item-sheet__button item-sheet__button--primary" (click)="addToCartStandalone()">
                Ajouter seul
              </button>
              <button class="item-sheet__button item-sheet__button--secondary" (click)="startCombo.emit(item().id)">
                Composer en menu
              </button>
            </div>
          } @else {
            <button class="item-sheet__button item-sheet__button--primary item-sheet__button--single" (click)="addToCartStandalone()">
              Ajouter au panier
            </button>
          }
        </div>
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

    .item-sheet {
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

    .item-sheet__handle {
      width: 3.4rem;
      height: 0.34rem;
      margin: 0.8rem auto 0;
      border-radius: 9999px;
      background: color-mix(in srgb, var(--color-outline) 52%, white 48%);
    }

    .item-sheet__close {
      position: absolute;
      top: 1rem;
      right: 1rem;
      z-index: 2;
      display: inline-grid;
      width: 2.35rem;
      height: 2.35rem;
      place-items: center;
      border-radius: 9999px;
      background: color-mix(in srgb, var(--color-surface-container-lowest) 70%, white 30%);
      color: var(--color-on-surface-variant);
      box-shadow: 0 14px 20px rgba(0, 0, 0, 0.1);
    }

    .item-sheet__media {
      position: relative;
      margin: 0.85rem 1rem 0;
      aspect-ratio: 16 / 11;
      overflow: hidden;
      border-radius: 1.9rem;
      background:
        radial-gradient(circle at top, color-mix(in srgb, var(--color-secondary-container) 42%, transparent), transparent 58%),
        color-mix(in srgb, var(--color-primary) 10%, var(--color-surface-container-high) 90%);
    }

    .item-sheet__image,
    .item-sheet__placeholder {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .item-sheet__placeholder {
      display: grid;
      place-items: center;
      font-family: var(--font-display);
      font-size: 3rem;
      font-weight: 800;
      color: color-mix(in srgb, var(--color-on-primary) 82%, white 18%);
      background:
        radial-gradient(circle at top, color-mix(in srgb, var(--color-secondary-container) 70%, white 30%), transparent 58%),
        linear-gradient(
          135deg,
          color-mix(in srgb, var(--color-primary) 78%, black 22%),
          color-mix(in srgb, var(--color-secondary) 84%, var(--color-primary) 16%)
        );
    }

    .item-sheet__media-scrim {
      position: absolute;
      inset: 0;
      background:
        linear-gradient(180deg, rgba(0, 0, 0, 0.02) 0%, rgba(0, 0, 0, 0.24) 100%);
      pointer-events: none;
    }

    .item-sheet__badges {
      position: absolute;
      inset: 0.9rem 0.9rem auto;
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .item-sheet__badge {
      display: inline-flex;
      align-items: center;
      min-height: 1.8rem;
      border-radius: 9999px;
      padding: 0 0.8rem;
      background: color-mix(in srgb, var(--color-surface-container-lowest) 72%, white 28%);
      color: var(--color-primary);
      font-family: var(--font-display);
      font-size: 0.74rem;
      font-weight: 800;
      letter-spacing: 0.02em;
      text-transform: uppercase;
      box-shadow: 0 14px 24px rgba(0, 0, 0, 0.08);
      backdrop-filter: blur(12px);
    }

    .item-sheet__badge--accent {
      background: color-mix(in srgb, var(--color-secondary-container) 78%, white 22%);
      color: var(--color-on-secondary-container);
    }

    .item-sheet__content {
      overflow-y: auto;
      padding: 1.15rem 1rem 1rem;
    }

    .item-sheet__header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 0.8rem;
    }

    .item-sheet__eyebrow {
      color: var(--color-on-surface-variant);
      font-size: 0.78rem;
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .item-sheet__title {
      margin-top: 0.3rem;
      font-family: var(--font-display);
      font-size: 1.7rem;
      font-weight: 800;
      line-height: 1.08;
      color: var(--color-on-surface);
      text-wrap: balance;
    }

    .item-sheet__price {
      display: inline-flex;
      align-items: center;
      min-height: 2.35rem;
      border-radius: 9999px;
      padding: 0 0.95rem;
      background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface-container-high) 88%);
      color: var(--color-primary);
      font-family: var(--font-display);
      font-size: 1rem;
      font-weight: 800;
      white-space: nowrap;
      box-shadow: 0 14px 24px rgba(0, 0, 0, 0.06);
    }

    .item-sheet__description {
      margin-top: 0.9rem;
      color: var(--color-on-surface-variant);
      font-size: 0.96rem;
      line-height: 1.55;
    }

    .item-sheet__note {
      margin-top: 0.85rem;
      border-radius: 1.35rem;
      background: color-mix(in srgb, var(--color-primary) 8%, var(--color-surface-container-high) 92%);
      padding: 0.9rem 0.95rem;
      color: var(--color-on-surface);
      font-size: 0.9rem;
      font-weight: 600;
      line-height: 1.45;
    }

    .item-sheet__actions {
      display: grid;
      gap: 0.75rem;
      margin-top: 1rem;
    }

    .item-sheet__button {
      width: 100%;
      min-height: 3.45rem;
      border-radius: 9999px;
      padding: 0 1.2rem;
      font-family: var(--font-display);
      font-size: 1rem;
      font-weight: 800;
      transition: transform 160ms ease, box-shadow 160ms ease;
    }

    .item-sheet__button:active {
      transform: scale(0.985);
    }

    .item-sheet__button--primary {
      background:
        linear-gradient(
          135deg,
          color-mix(in srgb, var(--color-primary) 84%, white 16%),
          color-mix(in srgb, var(--color-secondary) 84%, var(--color-primary) 16%)
        );
      color: var(--color-on-primary);
      box-shadow: 0 22px 34px color-mix(in srgb, var(--color-primary) 24%, transparent);
    }

    .item-sheet__button--secondary {
      background: color-mix(in srgb, var(--color-secondary-container) 78%, white 22%);
      color: var(--color-on-secondary-container);
      box-shadow: 0 18px 30px rgba(0, 0, 0, 0.08);
    }

    .item-sheet__button--single {
      margin-top: 1rem;
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
export class ItemModalComponent {
  item = input.required<ItemView>();
  canComposeAsMenu = input<boolean>(false);
  close = output<void>();
  startCombo = output<string>();

  private readonly cart = inject(CartService);

  showMenuChoice = computed(() => this.canComposeAsMenu());

  addToCartStandalone(): void {
    const i = this.item();
    this.cart.addStandalone(i.id, i.name, i.price, i.imagePath);
    this.close.emit();
  }

  itemInitial(): string {
    return this.item().name.trim().charAt(0).toUpperCase();
  }
}
