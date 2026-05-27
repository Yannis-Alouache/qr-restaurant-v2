import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MenuService } from '../../services/menu.service';
import { MenuView, CategoryView, ItemView } from '../../models/menu.model';
import { CategoryGridComponent } from '../../components/category-grid/category-grid.component';
import { ItemListComponent } from '../../components/item-list/item-list.component';
import { ItemModalComponent } from '../../components/item-modal/item-modal.component';
import { ComboStepperComponent } from '../../components/combo-stepper/combo-stepper.component';
import { CartPanelComponent } from '../../../cart/components/cart-panel/cart-panel.component';
import { FloatingCartBarComponent } from '../../../cart/components/floating-cart-bar/floating-cart-bar.component';
import { CartService } from '../../../cart/services/cart.service';
import { ThemeService } from '../../../../shared/services/theme.service';

@Component({
  selector: 'app-menu-page',
  standalone: true,
  imports: [
    CategoryGridComponent,
    ItemListComponent,
    ItemModalComponent,
    ComboStepperComponent,
    CartPanelComponent,
    FloatingCartBarComponent,
  ],
  template: `
    @if (loading()) {
      <div class="menu-state">
        <div class="menu-state__card">
          <div class="menu-loader"></div>
          <h1 class="menu-state__title">Chargement du menu</h1>
          <p class="menu-state__text">Préparation d'une expérience de commande plus visuelle et tactile.</p>
        </div>
      </div>
    } @else if (error()) {
      <div class="menu-state">
        <div class="menu-state__card">
          <div class="menu-state__emoji">😕</div>
          <h1 class="menu-state__title">Le menu est indisponible</h1>
          <p class="menu-state__text menu-state__text--error">{{ error() }}</p>
        </div>
      </div>
    } @else if (menu()) {
      <div class="menu-page safe-bottom-space">
        <header class="menu-hero">
          <div class="menu-hero__surface">
            <div class="menu-hero__glow menu-hero__glow--primary"></div>
            <div class="menu-hero__glow menu-hero__glow--secondary"></div>

            <div class="menu-hero__eyebrow">
              <span class="menu-pill">Commande à table</span>
              <span class="menu-pill menu-pill--soft">{{ categoryCount() }} catégories</span>
            </div>

            <div class="menu-brand">
              @if (menu()!.restaurant.logoPath) {
                <img
                  [src]="menu()!.restaurant.logoPath"
                  [alt]="menu()!.restaurant.name"
                  class="menu-brand__logo" />
              }
              <div class="menu-brand__copy">
                <h1 class="menu-brand__title">{{ menu()!.restaurant.name }}</h1>
                @if (menu()!.restaurant.address) {
                  <p class="menu-brand__address">{{ menu()!.restaurant.address }}</p>
                }
                <p class="menu-brand__description">
                  Une expérience mobile pensée pour être immédiate, tactile et gourmande dès l'ouverture du menu.
                </p>
              </div>
            </div>

            <div class="menu-hero__highlights">
              <div class="menu-hero__stat">
                <span class="menu-hero__stat-value">Tactile</span>
                <span class="menu-hero__stat-label">geste immédiat</span>
              </div>
              <div class="menu-hero__stat">
                <span class="menu-hero__stat-value">Visuel</span>
                <span class="menu-hero__stat-label">photos mises en avant</span>
              </div>
              <div class="menu-hero__stat">
                <span class="menu-hero__stat-value">Premium</span>
                <span class="menu-hero__stat-label">ambiance à table</span>
              </div>
            </div>
          </div>
        </header>

        <main class="menu-shell">
          <section class="menu-section">
            <div class="menu-section__head">
              <div>
                <p class="menu-section__eyebrow">Choisissez une famille</p>
                <h2 class="menu-section__title">Catégories</h2>
              </div>
              <span class="menu-section__counter">{{ categoryCount() }}</span>
            </div>
            <app-category-grid
              [categories]="menu()!.categories"
              [selectedId]="selectedCategoryId()"
              (selectCategory)="selectCategory($event)" />
          </section>

          @if (selectedCategory()) {
            <section class="category-spotlight">
              <div class="category-spotlight__copy">
                <span class="menu-pill">À l'affiche</span>
                <h2 class="category-spotlight__title">{{ selectedCategory()!.name }}</h2>
                <p class="category-spotlight__description">
                  {{ selectedCategoryItemCount() }} plat{{ selectedCategoryItemCount() > 1 ? 's' : '' }}
                  à découvrir{{ selectedCategory()!.hasMenu ? ', disponible aussi en formule.' : '.' }}
                </p>
              </div>
              <div class="category-spotlight__media">
                @if (selectedCategory()!.imagePath) {
                  <img
                    [src]="selectedCategory()!.imagePath!"
                    [alt]="selectedCategory()!.name"
                    class="category-spotlight__image" />
                } @else {
                  <div class="category-spotlight__fallback">{{ selectedCategoryInitial() }}</div>
                }
              </div>
            </section>

            <section class="menu-section menu-section--items">
              <div class="menu-section__head">
                <div>
                  <p class="menu-section__eyebrow">À commander maintenant</p>
                  <h2 class="menu-section__title">Les {{ selectedCategoryLabel() }}</h2>
                </div>
                @if (selectedCategory()!.hasMenu) {
                  <span class="menu-pill menu-pill--accent">Formule dispo</span>
                } @else {
                  <span class="menu-section__counter">{{ selectedCategoryItemCount() }}</span>
                }
              </div>

              <app-item-list
                [items]="selectedCategory()!.items"
                (itemClick)="openItemModal($event)" />
            </section>
          } @else {
            <section class="menu-empty">
              <h2 class="menu-empty__title">Sélectionnez une catégorie</h2>
              <p class="menu-empty__text">
                Les plats apparaissent ici dès qu'une catégorie est choisie.
              </p>
            </section>
          }
        </main>

        <app-floating-cart-bar (openCart)="showCartPanel.set(true)" />

        @if (activeItem()) {
          <app-item-modal
            [item]="activeItem()!"
            [canComposeAsMenu]="canComposeAsMenu(activeItem()!)"
            (close)="activeItem.set(null)"
            (startCombo)="startComboStepper($event)" />
        }

        <!-- Combo Stepper -->
        @if (showComboStepper() && comboBaseItemId()) {
          <app-combo-stepper
            [items]="selectedCategory()?.items ?? []"
            [compositions]="menu()?.compositions ?? []"
            [baseItemId]="comboBaseItemId()!"
            (cancel)="showComboStepper.set(false); comboBaseItemId.set(null)"
            (complete)="showComboStepper.set(false); comboBaseItemId.set(null); activeItem.set(null)" />
        }

        <!-- Cart Panel -->
        @if (showCartPanel()) {
          <app-cart-panel
            (close)="showCartPanel.set(false)"
            (order)="goToCheckout()" />
        }
      </div>
    }
  `,
  styles: [`
    :host {
      display: block;
    }

    .menu-page {
      min-height: 100dvh;
    }

    .menu-state {
      display: flex;
      min-height: 100dvh;
      align-items: center;
      justify-content: center;
      padding: 1.5rem;
    }

    .menu-state__card {
      width: min(100%, 24rem);
      border-radius: 2rem;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 76%, white 24%);
      background:
        linear-gradient(
          180deg,
          color-mix(in srgb, var(--color-surface-container-lowest) 90%, white 10%) 0%,
          color-mix(in srgb, var(--color-surface-container-low) 94%, var(--color-primary) 6%) 100%
        );
      padding: 1.6rem;
      text-align: center;
      box-shadow: var(--shadow-strong);
    }

    .menu-loader {
      width: 3rem;
      height: 3rem;
      margin: 0 auto 1rem;
      border-radius: 9999px;
      border: 0.22rem solid color-mix(in srgb, var(--color-primary) 16%, transparent);
      border-top-color: var(--color-primary);
      animation: spin 1s linear infinite;
    }

    .menu-state__emoji {
      margin-bottom: 0.8rem;
      font-size: 3rem;
    }

    .menu-state__title {
      font-family: var(--font-display);
      font-size: 1.45rem;
      font-weight: 800;
      color: var(--color-on-surface);
    }

    .menu-state__text {
      margin-top: 0.6rem;
      color: var(--color-on-surface-variant);
      font-size: 0.95rem;
      line-height: 1.5;
    }

    .menu-state__text--error {
      color: var(--color-error);
      font-weight: 600;
    }

    .menu-hero,
    .menu-shell {
      width: min(100%, var(--app-max-width));
      margin-inline: auto;
      padding-inline: 1rem;
    }

    .menu-hero {
      padding-top: 1rem;
    }

    .menu-hero__surface {
      position: relative;
      overflow: hidden;
      border-radius: 2.3rem;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 76%, white 24%);
      background: var(--menu-hero-gradient);
      padding: 1rem;
      box-shadow: var(--shadow-strong);
      isolation: isolate;
    }

    .menu-hero__glow {
      position: absolute;
      border-radius: 9999px;
      filter: blur(24px);
      opacity: 0.65;
      pointer-events: none;
    }

    .menu-hero__glow--primary {
      top: -1.4rem;
      right: -1.1rem;
      width: 7rem;
      height: 7rem;
      background: color-mix(in srgb, var(--color-primary) 36%, white 64%);
    }

    .menu-hero__glow--secondary {
      bottom: -1.8rem;
      left: -1.6rem;
      width: 8rem;
      height: 8rem;
      background: color-mix(in srgb, var(--color-secondary-container) 58%, white 42%);
    }

    .menu-hero__eyebrow,
    .menu-section__head {
      position: relative;
      z-index: 1;
    }

    .menu-hero__eyebrow {
      display: flex;
      flex-wrap: wrap;
      gap: 0.55rem;
      margin-bottom: 1rem;
    }

    .menu-pill {
      display: inline-flex;
      align-items: center;
      min-height: 1.95rem;
      border-radius: 9999px;
      padding: 0 0.85rem;
      background: color-mix(in srgb, var(--color-primary) 14%, white 86%);
      color: var(--color-primary);
      font-family: var(--font-display);
      font-size: 0.76rem;
      font-weight: 800;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      box-shadow: 0 14px 24px rgba(0, 0, 0, 0.08);
    }

    .menu-pill--soft {
      background: color-mix(in srgb, var(--color-surface-container-lowest) 76%, white 24%);
      color: var(--color-on-surface-variant);
    }

    .menu-pill--accent {
      background: color-mix(in srgb, var(--color-secondary-container) 74%, white 26%);
      color: var(--color-on-secondary-container);
    }

    .menu-brand {
      position: relative;
      z-index: 1;
      display: flex;
      gap: 0.9rem;
      align-items: flex-start;
    }

    .menu-brand__logo {
      width: 4.2rem;
      height: 4.2rem;
      flex-shrink: 0;
      border-radius: 1.45rem;
      object-fit: cover;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 72%, white 28%);
      background: color-mix(in srgb, var(--color-surface-container-lowest) 82%, white 18%);
      box-shadow: 0 18px 28px rgba(0, 0, 0, 0.1);
    }

    .menu-brand__copy {
      min-width: 0;
    }

    .menu-brand__title {
      font-family: var(--font-display);
      font-size: clamp(1.95rem, 7vw, 2.45rem);
      font-weight: 800;
      line-height: 1.02;
      color: var(--color-on-surface);
      text-wrap: balance;
    }

    .menu-brand__address {
      margin-top: 0.45rem;
      color: var(--color-on-surface-variant);
      font-size: 0.9rem;
      font-weight: 600;
    }

    .menu-brand__description {
      margin-top: 0.8rem;
      max-width: 26rem;
      color: color-mix(in srgb, var(--color-on-surface) 74%, var(--color-on-surface-variant) 26%);
      font-size: 0.98rem;
      line-height: 1.5;
      text-wrap: pretty;
    }

    .menu-hero__highlights {
      position: relative;
      z-index: 1;
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 0.7rem;
      margin-top: 1.1rem;
    }

    .menu-hero__stat {
      display: flex;
      min-height: 4.35rem;
      flex-direction: column;
      justify-content: flex-end;
      gap: 0.2rem;
      border-radius: 1.4rem;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 70%, white 30%);
      background: color-mix(in srgb, var(--color-surface-container-lowest) 68%, white 32%);
      padding: 0.85rem;
      box-shadow: 0 14px 26px rgba(0, 0, 0, 0.06);
    }

    .menu-hero__stat-value {
      font-family: var(--font-display);
      font-size: 0.94rem;
      font-weight: 800;
      color: var(--color-on-surface);
    }

    .menu-hero__stat-label {
      color: var(--color-on-surface-variant);
      font-size: 0.74rem;
      font-weight: 600;
      line-height: 1.3;
    }

    .menu-shell {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      padding-top: 1rem;
      padding-bottom: 1rem;
    }

    .menu-section {
      display: flex;
      flex-direction: column;
      gap: 0.9rem;
    }

    .menu-section--items {
      gap: 1rem;
    }

    .menu-section__head {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 0.8rem;
    }

    .menu-section__eyebrow {
      color: var(--color-on-surface-variant);
      font-size: 0.78rem;
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .menu-section__title {
      margin-top: 0.2rem;
      font-family: var(--font-display);
      font-size: 1.55rem;
      font-weight: 800;
      line-height: 1.08;
      color: var(--color-on-surface);
      text-wrap: balance;
    }

    .menu-section__counter {
      display: inline-grid;
      min-width: 2.4rem;
      min-height: 2.4rem;
      place-items: center;
      border-radius: 9999px;
      background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface-container-high) 88%);
      color: var(--color-primary);
      font-family: var(--font-display);
      font-size: 0.95rem;
      font-weight: 800;
      box-shadow: 0 14px 24px rgba(0, 0, 0, 0.06);
    }

    .category-spotlight {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 7.5rem;
      gap: 0.95rem;
      align-items: center;
      overflow: hidden;
      border-radius: 2rem;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 72%, white 28%);
      background: var(--menu-spotlight-gradient);
      padding: 1rem;
      box-shadow: var(--shadow-soft);
    }

    .category-spotlight__copy {
      min-width: 0;
    }

    .category-spotlight__title {
      margin-top: 0.75rem;
      font-family: var(--font-display);
      font-size: 1.7rem;
      font-weight: 800;
      line-height: 1.08;
      color: var(--color-on-surface);
      text-wrap: balance;
    }

    .category-spotlight__description {
      margin-top: 0.55rem;
      color: var(--color-on-surface-variant);
      font-size: 0.95rem;
      line-height: 1.5;
    }

    .category-spotlight__media {
      overflow: hidden;
      border-radius: 1.6rem;
      aspect-ratio: 1 / 1;
      background:
        radial-gradient(circle at top, color-mix(in srgb, var(--color-secondary-container) 42%, transparent), transparent 58%),
        color-mix(in srgb, var(--color-primary) 10%, var(--color-surface-container-high) 90%);
      box-shadow: 0 18px 30px rgba(0, 0, 0, 0.1);
    }

    .category-spotlight__image,
    .category-spotlight__fallback {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .category-spotlight__fallback {
      display: grid;
      place-items: center;
      font-family: var(--font-display);
      font-size: 2.5rem;
      font-weight: 800;
      color: color-mix(in srgb, var(--color-on-primary) 84%, white 16%);
      background:
        radial-gradient(circle at top, color-mix(in srgb, var(--color-secondary-container) 70%, white 30%), transparent 58%),
        linear-gradient(
          135deg,
          color-mix(in srgb, var(--color-primary) 78%, black 22%),
          color-mix(in srgb, var(--color-secondary) 82%, var(--color-primary) 18%)
        );
    }

    .menu-empty {
      border-radius: 1.9rem;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 70%, white 30%);
      background: color-mix(in srgb, var(--color-surface-container-lowest) 86%, white 14%);
      padding: 1.5rem 1.2rem;
      text-align: center;
      box-shadow: var(--shadow-soft);
    }

    .menu-empty__title {
      font-family: var(--font-display);
      font-size: 1.2rem;
      font-weight: 800;
      color: var(--color-on-surface);
    }

    .menu-empty__text {
      margin-top: 0.55rem;
      color: var(--color-on-surface-variant);
      font-size: 0.94rem;
      line-height: 1.5;
    }

    .animate-spin {
      animation: spin 1s linear infinite;
    }

    @media (max-width: 23rem) {
      .menu-hero__highlights {
        grid-template-columns: 1fr;
      }

      .category-spotlight {
        grid-template-columns: 1fr;
      }

      .category-spotlight__media {
        width: 8rem;
      }
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }
  `],
})
export class MenuPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly menuService = inject(MenuService);
  private readonly themeService = inject(ThemeService);
  readonly cart = inject(CartService);

  menu = signal<MenuView | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  selectedCategoryId = signal<string | null>(null);
  selectedCategory = signal<CategoryView | null>(null);
  readonly categoryCount = computed(() => this.menu()?.categories.length ?? 0);
  readonly selectedCategoryItemCount = computed(
    () =>
      (this.selectedCategory()?.items ?? []).filter(
        (item) => item.available && item.menuVariantOf === null,
      ).length,
  );
  comboBaseItemId = signal<string | null>(null);

  activeItem = signal<ItemView | null>(null);
  showComboStepper = signal(false);
  showCartPanel = signal(false);

  private slug = '';
  private tableId = '';

  ngOnInit(): void {
    this.slug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.tableId = this.route.snapshot.paramMap.get('tableId') ?? '';

    if (!this.slug) {
      this.error.set('Restaurant introuvable');
      this.loading.set(false);
      return;
    }

    this.menuService.getMenu(this.slug).subscribe({
      next: (data) => {
        this.menu.set(data);
        this.themeService.apply(data.restaurant.themeId);
        this.loading.set(false);
        if (data.categories.length > 0) {
          this.selectCategory(data.categories[0].id);
        }
      },
      error: () => {
        this.error.set('Impossible de charger le menu');
        this.loading.set(false);
      },
    });
  }

  selectCategory(categoryId: string): void {
    this.selectedCategoryId.set(categoryId);
    const category = this.menu()?.categories.find(c => c.id === categoryId) ?? null;
    this.selectedCategory.set(category);
  }

  selectedCategoryInitial(): string {
    return this.selectedCategory()?.name.trim().charAt(0).toUpperCase() ?? '?';
  }

  selectedCategoryLabel(): string {
    return this.selectedCategory()?.name.toLowerCase() ?? '';
  }

  openItemModal(item: ItemView): void {
    this.activeItem.set(item);
  }

  canComposeAsMenu(item: ItemView): boolean {
    return (this.selectedCategory()?.items ?? []).some(
      (candidate) => candidate.menuVariantOf === item.id && candidate.available,
    );
  }

  startComboStepper(baseItemId: string): void {
    this.comboBaseItemId.set(baseItemId);
    this.activeItem.set(null);
    this.showComboStepper.set(true);
  }

  goToCheckout(): void {
    this.showCartPanel.set(false);
    this.router.navigate(['/checkout', this.slug, this.tableId]);
  }
}
