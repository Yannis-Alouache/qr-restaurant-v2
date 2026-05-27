import { Component, input, output } from '@angular/core';
import { ItemView } from '../../models/menu.model';
import { PricePipe } from '../../../../shared/pipes/price.pipe';

@Component({
  selector: 'app-item-card',
  standalone: true,
  imports: [PricePipe],
  template: `
    <button class="item-card" (click)="cardClick.emit(item())">
      <div class="item-card__media">
        @if (item().imagePath) {
          <img [src]="item().imagePath!" [alt]="item().name" class="item-card__image" />
        } @else {
          <div class="item-card__placeholder">{{ itemInitial() }}</div>
        }
        <div class="item-card__media-scrim"></div>
        <div class="item-card__media-top">
          @if (canComposeAsMenu()) {
            <span class="item-card__badge">Menu dispo</span>
          }
          <span class="item-card__price">{{ item().price | price }}</span>
        </div>
      </div>

      <div class="item-card__content">
        <div class="item-card__heading">
          <h3 class="item-card__title">{{ item().name }}</h3>
          <span class="item-card__cta">Découvrir</span>
        </div>
        @if (item().description) {
          <p class="item-card__description">{{ item().description }}</p>
        }
        <div class="item-card__footer">
          <span class="item-card__hint">
            {{ canComposeAsMenu() ? 'Touchez pour choisir seul ou en menu' : 'Touchez pour voir la fiche complète' }}
          </span>
          <span class="item-card__icon" aria-hidden="true">+</span>
        </div>
      </div>
    </button>
  `,
  styles: [`
    :host {
      display: block;
    }

    .item-card {
      width: 100%;
      overflow: hidden;
      border-radius: 2rem;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 80%, white 20%);
      background:
        linear-gradient(
          180deg,
          color-mix(in srgb, var(--color-surface-container-lowest) 92%, white 8%) 0%,
          color-mix(in srgb, var(--color-surface-container-low) 95%, var(--color-primary) 5%) 100%
        );
      box-shadow: var(--shadow-soft);
      text-align: left;
      transition:
        transform 160ms ease,
        box-shadow 160ms ease,
        border-color 160ms ease;
    }

    .item-card:active {
      transform: scale(0.985);
      box-shadow: 0 14px 26px color-mix(in srgb, var(--color-primary) 18%, transparent);
    }

    .item-card__media {
      position: relative;
      aspect-ratio: 16 / 10;
      background:
        radial-gradient(circle at top, color-mix(in srgb, var(--color-secondary-container) 44%, transparent), transparent 58%),
        color-mix(in srgb, var(--color-primary) 8%, var(--color-surface-container) 92%);
    }

    .item-card__image,
    .item-card__placeholder {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .item-card__placeholder {
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

    .item-card__media-scrim {
      position: absolute;
      inset: 0;
      background:
        linear-gradient(180deg, rgba(0, 0, 0, 0.03) 0%, rgba(0, 0, 0, 0.22) 100%);
      pointer-events: none;
    }

    .item-card__media-top {
      position: absolute;
      inset: 0.9rem 0.9rem auto;
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 0.75rem;
    }

    .item-card__badge,
    .item-card__price {
      display: inline-flex;
      align-items: center;
      min-height: 1.9rem;
      border-radius: 9999px;
      padding: 0 0.8rem;
      font-family: var(--font-display);
      font-size: 0.78rem;
      font-weight: 800;
      letter-spacing: 0.01em;
      backdrop-filter: blur(12px);
    }

    .item-card__badge {
      background: color-mix(in srgb, var(--color-secondary-container) 78%, white 22%);
      color: var(--color-on-secondary-container);
      box-shadow: 0 12px 20px rgba(0, 0, 0, 0.08);
    }

    .item-card__price {
      margin-left: auto;
      background: color-mix(in srgb, var(--color-surface-container-lowest) 72%, white 28%);
      color: var(--color-primary);
      box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
    }

    .item-card__content {
      display: flex;
      flex-direction: column;
      gap: 0.85rem;
      padding: 1rem 1rem 1.05rem;
    }

    .item-card__heading {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 0.85rem;
    }

    .item-card__title {
      flex: 1;
      font-family: var(--font-display);
      font-size: 1.15rem;
      font-weight: 700;
      line-height: 1.2;
      color: var(--color-on-surface);
      text-wrap: balance;
    }

    .item-card__cta {
      display: inline-flex;
      align-items: center;
      min-height: 1.75rem;
      border-radius: 9999px;
      padding: 0 0.7rem;
      background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface-container-high) 88%);
      color: var(--color-primary);
      font-family: var(--font-display);
      font-size: 0.75rem;
      font-weight: 800;
      white-space: nowrap;
    }

    .item-card__description {
      display: -webkit-box;
      overflow: hidden;
      color: var(--color-on-surface-variant);
      font-size: 0.94rem;
      line-height: 1.45;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
    }

    .item-card__footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.9rem;
    }

    .item-card__hint {
      color: var(--color-on-surface-variant);
      font-size: 0.8rem;
      font-weight: 600;
      line-height: 1.35;
    }

    .item-card__icon {
      display: inline-grid;
      width: 2.3rem;
      height: 2.3rem;
      place-items: center;
      flex-shrink: 0;
      border-radius: 9999px;
      background:
        linear-gradient(
          135deg,
          color-mix(in srgb, var(--color-primary) 82%, white 18%),
          color-mix(in srgb, var(--color-secondary) 84%, var(--color-primary) 16%)
        );
      color: var(--color-on-primary);
      font-family: var(--font-display);
      font-size: 1.2rem;
      font-weight: 800;
      box-shadow: 0 14px 24px color-mix(in srgb, var(--color-primary) 22%, transparent);
    }
  `],
})
export class ItemCardComponent {
  item = input.required<ItemView>();
  canComposeAsMenu = input<boolean>(false);
  cardClick = output<ItemView>();

  itemInitial(): string {
    return this.item().name.trim().charAt(0).toUpperCase();
  }
}
