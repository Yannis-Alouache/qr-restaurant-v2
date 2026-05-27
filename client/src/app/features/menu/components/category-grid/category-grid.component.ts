import { Component, input, output } from '@angular/core';
import { CategoryView } from '../../models/menu.model';

@Component({
  selector: 'app-category-grid',
  standalone: true,
  template: `
    <div class="category-grid">
      @for (category of categories(); track category.id) {
        <button
          class="category-card"
          (click)="selectCategory.emit(category.id)"
          [class.category-card--selected]="isSelected(category.id)">
          <div class="category-card__media">
            @if (category.imagePath) {
              <img [src]="category.imagePath" [alt]="category.name" class="category-card__image" />
            } @else {
              <div class="category-card__placeholder">{{ categoryInitial(category.name) }}</div>
            }
            <div class="category-card__scrim"></div>
            @if (category.hasMenu) {
              <span class="category-card__badge">Formule</span>
            }
            @if (isSelected(category.id)) {
              <span class="category-card__state">Active</span>
            }
          </div>
          <div class="category-card__body">
            <span class="category-card__name">{{ category.name }}</span>
            <span class="category-card__meta">
              {{ availableStandaloneCount(category) }} plat{{ availableStandaloneCount(category) > 1 ? 's' : '' }}
            </span>
          </div>
        </button>
      }
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .category-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 0.85rem;
    }

    @media (max-width: 21.25rem) {
      .category-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    .category-card {
      display: flex;
      flex-direction: column;
      gap: 0.7rem;
      min-height: 10.5rem;
      padding: 0.65rem;
      border-radius: 1.6rem;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 82%, white 18%);
      background:
        linear-gradient(
          180deg,
          color-mix(in srgb, var(--color-surface-container-lowest) 86%, white 14%) 0%,
          color-mix(in srgb, var(--color-surface-container-low) 94%, var(--color-primary) 6%) 100%
        );
      box-shadow: var(--shadow-soft);
      overflow: hidden;
      text-align: left;
      transition:
        transform 160ms ease,
        box-shadow 160ms ease,
        border-color 160ms ease,
        background 160ms ease;
    }

    .category-card:active {
      transform: scale(0.985);
    }

    .category-card--selected {
      transform: translateY(-0.1rem);
      border-color: color-mix(in srgb, var(--color-primary) 52%, white 48%);
      box-shadow: 0 20px 36px color-mix(in srgb, var(--color-primary) 24%, transparent);
      background:
        linear-gradient(
          180deg,
          color-mix(in srgb, var(--color-primary-container) 46%, white 54%) 0%,
          color-mix(in srgb, var(--color-surface-container-lowest) 72%, var(--color-primary) 28%) 100%
        );
    }

    .category-card__media {
      position: relative;
      aspect-ratio: 1 / 1;
      border-radius: 1.2rem;
      overflow: hidden;
      background: color-mix(in srgb, var(--color-primary) 10%, var(--color-surface-container-high) 90%);
    }

    .category-card__image,
    .category-card__placeholder {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .category-card__placeholder {
      display: grid;
      place-items: center;
      font-family: var(--font-display);
      font-size: 2rem;
      font-weight: 800;
      color: color-mix(in srgb, var(--color-on-primary) 78%, white 22%);
      background:
        radial-gradient(circle at top, color-mix(in srgb, var(--color-secondary-container) 78%, white 22%), transparent 58%),
        linear-gradient(
          135deg,
          color-mix(in srgb, var(--color-primary) 78%, black 22%),
          color-mix(in srgb, var(--color-secondary) 80%, var(--color-primary) 20%)
        );
    }

    .category-card__scrim {
      position: absolute;
      inset: 0;
      background:
        linear-gradient(180deg, rgba(0, 0, 0, 0.02) 0%, rgba(0, 0, 0, 0.12) 100%);
      pointer-events: none;
    }

    .category-card__badge,
    .category-card__state {
      position: absolute;
      top: 0.55rem;
      display: inline-flex;
      align-items: center;
      min-height: 1.55rem;
      border-radius: 9999px;
      padding: 0 0.55rem;
      font-family: var(--font-display);
      font-size: 0.68rem;
      font-weight: 800;
      letter-spacing: 0.02em;
      backdrop-filter: blur(12px);
    }

    .category-card__badge {
      left: 0.55rem;
      background: color-mix(in srgb, var(--color-secondary-container) 72%, white 28%);
      color: var(--color-on-secondary-container);
      box-shadow: 0 10px 16px rgba(0, 0, 0, 0.08);
    }

    .category-card__state {
      right: 0.55rem;
      background: color-mix(in srgb, var(--color-primary) 84%, white 16%);
      color: var(--color-on-primary);
      box-shadow: 0 10px 16px color-mix(in srgb, var(--color-primary) 22%, transparent);
    }

    .category-card__body {
      display: flex;
      min-height: 3rem;
      flex-direction: column;
      gap: 0.2rem;
    }

    .category-card__name {
      display: -webkit-box;
      overflow: hidden;
      font-family: var(--font-display);
      font-size: 0.92rem;
      font-weight: 700;
      line-height: 1.15;
      color: var(--color-on-surface);
      text-wrap: balance;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .category-card__meta {
      font-size: 0.72rem;
      font-weight: 600;
      color: var(--color-on-surface-variant);
    }
  `],
})
export class CategoryGridComponent {
  categories = input.required<CategoryView[]>();
  selectedId = input<string | null>(null);
  selectCategory = output<string>();

  isSelected(categoryId: string): boolean {
    return this.selectedId() === categoryId;
  }

  categoryInitial(name: string): string {
    return name.trim().charAt(0).toUpperCase();
  }

  availableStandaloneCount(category: CategoryView): number {
    return category.items.filter((item) => item.available && item.menuVariantOf === null).length;
  }
}
