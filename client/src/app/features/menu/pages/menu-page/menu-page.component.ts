import { Component, inject, OnInit, signal } from '@angular/core';
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
      <div class="flex items-center justify-center min-h-screen bg-surface">
        <div class="text-center">
          <div class="w-10 h-10 border-3 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <div class="text-on-surface-variant font-body text-lg">Chargement du menu...</div>
        </div>
      </div>
    } @else if (error()) {
      <div class="flex items-center justify-center min-h-screen bg-surface">
        <div class="text-center">
          <div class="text-5xl mb-4">😕</div>
          <div class="text-error font-body text-lg">{{ error() }}</div>
        </div>
      </div>
    } @else if (menu()) {
      <div class="min-h-screen bg-surface safe-bottom-space">
        <!-- Restaurant Header -->
        <header class="relative overflow-hidden border-b border-outline-variant/60 bg-primary text-on-primary shadow-[var(--shadow-soft)]">
          <div class="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.18),transparent_45%)]"></div>
          <div class="relative mx-auto flex max-w-[42rem] flex-col gap-6 px-4 pb-6 pt-8 sm:px-5 sm:pb-7">
            <div class="inline-flex w-fit items-center gap-2 rounded-full bg-white/12 px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-on-primary/80">
              Menu digital
            </div>
            <div class="flex items-center gap-4">
            @if (menu()!.restaurant.logoPath) {
              <img [src]="menu()!.restaurant.logoPath" [alt]="menu()!.restaurant.name"
                   class="h-14 w-14 rounded-2xl object-cover bg-on-primary/10 ring-1 ring-white/15" />
            }
              <div class="min-w-0">
                <h1 class="font-display text-3xl font-bold tracking-tight sm:text-[2.125rem]">{{ menu()!.restaurant.name }}</h1>
              @if (menu()!.restaurant.address) {
                  <p class="mt-1 text-sm text-on-primary/80">{{ menu()!.restaurant.address }}</p>
              }
                <p class="mt-2 max-w-md text-sm text-on-primary/78">
                  Commandez à votre rythme et retrouvez votre panier à tout moment.
                </p>
              </div>
            </div>
          </div>
        </header>

        <!-- Category Navigation -->
        <nav class="sticky top-0 z-10 border-b border-outline-variant/60 bg-surface-container-low/92 backdrop-blur-xl">
          <div class="mx-auto max-w-[42rem] px-4 sm:px-5">
            <app-category-grid
              [categories]="menu()!.categories"
              [selectedId]="selectedCategoryId()"
              (selectCategory)="selectCategory($event)" />
          </div>
        </nav>

        <!-- Menu Items -->
        <main class="mx-auto flex max-w-[42rem] flex-col gap-5 px-4 pb-6 pt-5 sm:px-5">
          @if (selectedCategory()) {
            <div class="flex items-end justify-between gap-4">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.18em] text-on-surface-variant">Catégorie</p>
                <h2 class="mt-1 font-display text-2xl font-bold text-on-surface">
                  {{ selectedCategory()!.name }}
                </h2>
              </div>
            </div>
            <app-item-list
              [items]="selectedCategory()!.items"
              (itemClick)="openItemModal($event)" />
          } @else {
            <div class="text-center py-12 text-on-surface-variant font-body">
              Sélectionnez une catégorie pour voir les plats
            </div>
          }
        </main>

        <!-- Floating Cart Bar -->
        <app-floating-cart-bar (openCart)="showCartPanel.set(true)" />

        <!-- Item Modal -->
        @if (activeItem()) {
          <app-item-modal
            [item]="activeItem()!"
            [canComposeAsMenu]="canComposeAsMenu(activeItem()!)"
            (close)="activeItem.set(null)"
            (startCombo)="startComboStepper()" />
        }

        <!-- Combo Stepper -->
        @if (showComboStepper()) {
          <app-combo-stepper
            [items]="selectedCategory()?.items ?? []"
            [compositions]="menu()?.compositions ?? []"
            (cancel)="showComboStepper.set(false)"
            (complete)="showComboStepper.set(false); activeItem.set(null)" />
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
    .animate-spin {
      animation: spin 1s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
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

  openItemModal(item: ItemView): void {
    this.activeItem.set(item);
  }

  canComposeAsMenu(item: ItemView): boolean {
    return (this.selectedCategory()?.items ?? []).some(
      (candidate) => candidate.menuVariantOf === item.id && candidate.available,
    );
  }

  startComboStepper(): void {
    this.activeItem.set(null);
    this.showComboStepper.set(true);
  }

  goToCheckout(): void {
    this.showCartPanel.set(false);
    this.router.navigate(['/checkout', this.slug, this.tableId]);
  }
}
