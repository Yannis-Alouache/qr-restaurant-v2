import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MenuService } from '../../services/menu.service';
import { MenuView } from '../../models/menu.model';
import { ThemeService } from '../../../../shared/services/theme.service';
import { RestaurantHeaderComponent } from '../../../../shared/components/restaurant-header/restaurant-header.component';
import { CartBarComponent } from '../../../../shared/components/cart-bar/cart-bar.component';
import { CartDrawerComponent } from '../../../../shared/components/cart-drawer/cart-drawer.component';
import {
  categoryFallbackColor,
  categoryHasMenus,
  restaurantCover,
} from '../../utils/menu-utils';

type Filter = 'menu' | 'populaire';

/** Écran 1 de la maquette : grille de catégories + panier flottant. */
@Component({
  selector: 'app-categories-screen',
  templateUrl: './categories-screen.component.html',
  styleUrl: './categories-screen.component.scss',
  imports: [RestaurantHeaderComponent, CartBarComponent, CartDrawerComponent],
})
export class CategoriesScreenComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly menuService = inject(MenuService);
  private readonly themeService = inject(ThemeService);

  menu = signal<MenuView | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  activeFilter = signal<Filter>('menu');
  showCartDrawer = signal(false);

  /** Bannière de couverture : image dédiée du restaurant, sinon première image de catégorie. */
  readonly coverPath = computed(() => {
    const menu = this.menu();
    return menu ? restaurantCover(menu) : null;
  });

  protected readonly slug = this.route.snapshot.paramMap.get('slug') ?? '';
  protected readonly tableId = this.route.snapshot.paramMap.get('tableId') ?? '';

  ngOnInit(): void {
    if (!this.slug) {
      this.error.set('Restaurant introuvable');
      this.loading.set(false);
      return;
    }

    this.menuService.getMenu(this.slug).subscribe({
      next: data => {
        this.menu.set(data);
        this.themeService.apply(data.restaurant.themeId);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger le menu');
        this.loading.set(false);
      },
    });
  }

  setFilter(filter: Filter): void {
    this.activeFilter.set(filter);
  }

  colorFor(index: number): string {
    return categoryFallbackColor(index);
  }

  openCategory(categoryId: string): void {
    const category = this.menu()?.categories.find(c => c.id === categoryId);
    if (!category) return;

    // Sans formule menu disponible, l'étape de choix Menu/Solo n'a pas de sens.
    const suffix = categoryHasMenus(category) ? [] : ['solo'];
    void this.router.navigate(['/menu', this.slug, this.tableId, 'c', category.id, ...suffix]);
  }

  goToCheckout(): void {
    this.showCartDrawer.set(false);
    void this.router.navigate(['/checkout', this.slug, this.tableId]);
  }
}
