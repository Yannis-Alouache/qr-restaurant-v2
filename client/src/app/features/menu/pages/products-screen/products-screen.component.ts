import { Component, computed, inject, OnInit, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MenuService } from '../../services/menu.service';
import { CategoryView, ItemView, MenuView } from '../../models/menu.model';
import { CartService } from '../../../cart/services/cart.service';
import { ThemeService } from '../../../../shared/services/theme.service';
import { PricePipe } from '../../../../shared/pipes/price.pipe';
import { RestaurantHeaderComponent } from '../../../../shared/components/restaurant-header/restaurant-header.component';
import { CartBarComponent } from '../../../../shared/components/cart-bar/cart-bar.component';
import { CartDrawerComponent } from '../../../../shared/components/cart-drawer/cart-drawer.component';
import { ProductSheetComponent, ProductSheetData } from '../../../../shared/components/product-sheet/product-sheet.component';
import { CategoryCarouselComponent } from '../../../../shared/components/category-carousel/category-carousel.component';
import { ConfirmToastComponent } from '../../../../shared/components/confirm-toast/confirm-toast.component';
import { categoryCover, categoryFallbackColor, standaloneItems } from '../../utils/menu-utils';

/** Écran 4 de la maquette : grille d'articles solo d'une catégorie. */
@Component({
  selector: 'app-products-screen',
  templateUrl: './products-screen.component.html',
  styleUrl: './products-screen.component.scss',
  imports: [
    PricePipe,
    RestaurantHeaderComponent,
    CategoryCarouselComponent,
    CartBarComponent,
    CartDrawerComponent,
    ProductSheetComponent,
    ConfirmToastComponent,
  ],
})
export class ProductsScreenComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly menuService = inject(MenuService);
  private readonly themeService = inject(ThemeService);
  private readonly cart = inject(CartService);

  private readonly toastRef = viewChild.required(ConfirmToastComponent);

  menu = signal<MenuView | null>(null);
  category = signal<CategoryView | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  sheetProduct = signal<ProductSheetData | null>(null);
  private pendingItem: ItemView | null = null;

  showCartDrawer = signal(false);

  protected readonly slug = this.route.snapshot.paramMap.get('slug') ?? '';
  protected readonly tableId = this.route.snapshot.paramMap.get('tableId') ?? '';
  private readonly initialCategoryId = this.route.snapshot.paramMap.get('categoryId') ?? '';

  readonly coverPath = computed(() => {
    const categories = this.menu()?.categories ?? [];
    for (const category of categories) {
      const cover = categoryCover(category);
      if (cover) return cover;
    }
    return null;
  });

  readonly items = computed(() => {
    const category = this.category();
    return category ? standaloneItems(category) : [];
  });

  readonly carouselCategories = computed(() =>
    (this.menu()?.categories ?? []).map(c => ({ id: c.id, name: c.name, imagePath: c.imagePath })),
  );

  ngOnInit(): void {
    if (!this.slug || !this.initialCategoryId) {
      this.error.set('Catégorie introuvable');
      this.loading.set(false);
      return;
    }

    this.menuService.getMenu(this.slug).subscribe({
      next: data => {
        this.menu.set(data);
        this.themeService.apply(data.restaurant.themeId);
        const category = data.categories.find(c => c.id === this.initialCategoryId) ?? null;
        this.loading.set(false);
        if (!category) {
          this.error.set('Catégorie introuvable');
          return;
        }
        this.category.set(category);
      },
      error: () => {
        this.error.set('Impossible de charger le menu');
        this.loading.set(false);
      },
    });
  }

  selectCategory(categoryId: string): void {
    if (this.category()?.id === categoryId) return;
    const target = this.menu()?.categories.find(c => c.id === categoryId);
    if (target) this.category.set(target);
  }

  colorFor(index: number): string {
    return categoryFallbackColor(index);
  }

  openProductSheet(item: ItemView): void {
    this.pendingItem = item;
    this.sheetProduct.set({
      name: item.name,
      description: item.description,
      price: item.price,
      priceLabel: null,
      imagePath: item.imagePath,
    });
  }

  addFromSheet(): void {
    const item = this.pendingItem;
    if (!item) return;

    this.cart.addStandalone(item.id, item.name, item.price, item.imagePath);
    this.sheetProduct.set(null);
    this.toastRef().show('Article ajouté au panier', item.name);
    setTimeout(() => {
      void this.router.navigate(['/menu', this.slug, this.tableId]);
    }, 400);
  }

  goBack(): void {
    void this.router.navigate(['/menu', this.slug, this.tableId]);
  }

  goToCheckout(): void {
    this.showCartDrawer.set(false);
    void this.router.navigate(['/checkout', this.slug, this.tableId]);
  }
}
