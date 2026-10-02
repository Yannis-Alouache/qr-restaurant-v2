import { Component, computed, inject, OnInit, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MenuService } from '../../services/menu.service';
import { CategoryView, CompositionEntry, ItemView, MenuView } from '../../models/menu.model';
import { CartService } from '../../../cart/services/cart.service';
import { ThemeService } from '../../../../shared/services/theme.service';
import { RestaurantHeaderComponent } from '../../../../shared/components/restaurant-header/restaurant-header.component';
import { CartBarComponent } from '../../../../shared/components/cart-bar/cart-bar.component';
import { CartDrawerComponent } from '../../../../shared/components/cart-drawer/cart-drawer.component';
import { ProductSheetComponent, ProductSheetData } from '../../../../shared/components/product-sheet/product-sheet.component';
import { CategoryCarouselComponent } from '../../../../shared/components/category-carousel/category-carousel.component';
import { ConfirmToastComponent } from '../../../../shared/components/confirm-toast/confirm-toast.component';
import { PricePipe } from '../../../../shared/pipes/price.pipe';
import {
  categoryFallbackColor,
  compositionsOf,
  menuEligibleItems,
  menuVariantOf,
  restaurantCover,
} from '../../utils/menu-utils';

interface StepEntry {
  item: ItemView | null;
  composition: CompositionEntry | null;
}

/**
 * Écran 3 de la maquette : composition d'une Formule Menu en 3 étapes
 * (plat → accompagnement → boisson) puis ajout au panier.
 */
@Component({
  selector: 'app-menu-stepper-screen',
  templateUrl: './menu-stepper-screen.component.html',
  styleUrl: './menu-stepper-screen.component.scss',
  imports: [
    RestaurantHeaderComponent,
    CategoryCarouselComponent,
    CartBarComponent,
    CartDrawerComponent,
    ProductSheetComponent,
    ConfirmToastComponent,
    PricePipe,
  ],
})
export class MenuStepperScreenComponent implements OnInit {
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

  step = signal(1);
  selectedBase = signal<ItemView | null>(null);
  selectedSide = signal<CompositionEntry | null>(null);
  selectedDrink = signal<CompositionEntry | null>(null);

  sheetProduct = signal<ProductSheetData | null>(null);
  /** Entrée en cours de consultation dans la fiche produit. */
  private pendingEntry: StepEntry | null = null;

  showCartDrawer = signal(false);

  protected readonly slug = this.route.snapshot.paramMap.get('slug') ?? '';
  protected readonly tableId = this.route.snapshot.paramMap.get('tableId') ?? '';
  private readonly initialCategoryId = this.route.snapshot.paramMap.get('categoryId') ?? '';

  readonly coverPath = computed(() => {
    const menu = this.menu();
    return menu ? restaurantCover(menu) : null;
  });

  /** Items proposés à l'étape courante. */
  readonly stepItems = computed<StepEntry[]>(() => {
    const category = this.category();
    const menu = this.menu();
    if (!category || !menu) return [];

    if (this.step() === 1) {
      return menuEligibleItems(category).map(item => ({ item, composition: null }));
    }
    const type = this.step() === 2 ? 'accompagnement' : 'boisson';
    return compositionsOf(menu.compositions, type).map(composition => ({
      item: null,
      composition,
    }));
  });

  /** Le carrousel ne met en avant la catégorie qu'à l'étape 1 (comportement maquette). */
  readonly carouselSelectedId = computed(() =>
    this.step() === 1 ? (this.category()?.id ?? null) : null,
  );

  readonly pageTitle = computed(() => {
    if (this.step() === 1) return this.category()?.name ?? '';
    if (this.step() === 2) return 'Accompagnements';
    return 'Boissons';
  });

  readonly pageSubtitle = computed(() => {
    if (this.step() === 1) return 'Choisissez votre plat principal';
    if (this.step() === 2) return 'Choisissez votre accompagnement';
    return 'Choisissez votre boisson';
  });

  /** Chips de sélection affichées sous le stepper. */
  readonly selectionChips = computed(() => {
    const chips: string[] = [];
    if (this.selectedBase()) chips.push(this.selectedBase()!.name);
    if (this.selectedSide()) chips.push(this.selectedSide()!.menuItemName);
    if (this.selectedDrink()) chips.push(this.selectedDrink()!.menuItemName);
    return chips;
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

  /** Nom affiché pour une entrée d'étape (plat ou composition). */
  entryName(entry: StepEntry): string {
    return entry.item?.name ?? entry.composition?.menuItemName ?? '';
  }

  /** Couleur de repli pour une carte produit sans image. */
  colorFor(index: number): string {
    return categoryFallbackColor(index);
  }

  selectCategory(categoryId: string): void {
    if (this.step() === 1 && this.category()?.id === categoryId) return;
    const target = this.menu()?.categories.find(c => c.id === categoryId);
    if (!target) return;

    this.category.set(target);
    this.step.set(1);
    this.selectedBase.set(null);
    this.selectedSide.set(null);
    this.selectedDrink.set(null);
  }

  /** « Inclus » ou « + X,XX € » pour un accompagnement / une boisson de formule. */
  priceLabel(composition: CompositionEntry): string {
    return composition.supplementPrice === 0
      ? 'Inclus'
      : `+ ${composition.supplementPrice.toFixed(2).replace('.', ',')} €`;
  }

  isEntrySelected(entry: StepEntry): boolean {
    if (entry.item) {
      return this.step() === 1 && this.selectedBase()?.id === entry.item.id;
    }
    if (this.step() === 2) return this.selectedSide()?.menuItemId === entry.composition!.menuItemId;
    return this.selectedDrink()?.menuItemId === entry.composition!.menuItemId;
  }

  openProductSheet(entry: StepEntry): void {
    this.pendingEntry = entry;

    if (entry.item) {
      this.sheetProduct.set({
        name: entry.item.name,
        description: entry.item.description,
        price: entry.item.price,
        priceLabel: null,
        imagePath: entry.item.imagePath,
      });
      return;
    }

    const composition = entry.composition!;
    this.sheetProduct.set({
      name: composition.menuItemName,
      description: null,
      price: composition.supplementPrice,
      priceLabel: this.priceLabel(composition),
      imagePath: composition.menuItemImagePath,
    });
  }

  addFromSheet(): void {
    const entry = this.pendingEntry;
    if (!entry) return;

    if (this.step() === 1 && entry.item) {
      this.selectedBase.set(entry.item);
      this.advanceStep();
    } else if (this.step() === 2 && entry.composition) {
      this.selectedSide.set(entry.composition);
      this.advanceStep();
    } else if (this.step() === 3 && entry.composition) {
      this.selectedDrink.set(entry.composition);
      this.composeMenu();
    }
  }

  goBack(): void {
    if (this.step() > 1) {
      if (this.step() === 2) this.selectedSide.set(null);
      if (this.step() === 3) this.selectedDrink.set(null);
      this.step.set(this.step() - 1);
      return;
    }
    void this.router.navigate(['/menu', this.slug, this.tableId, 'c', this.category()?.id]);
  }

  goToCheckout(): void {
    this.showCartDrawer.set(false);
    void this.router.navigate(['/checkout', this.slug, this.tableId]);
  }

  private advanceStep(): void {
    this.sheetProduct.set(null);
    setTimeout(() => {
      this.step.set(this.step() + 1);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }, 300);
  }

  private composeMenu(): void {
    const category = this.category();
    const base = this.selectedBase();
    const side = this.selectedSide();
    const drink = this.selectedDrink();
    if (!category || !base || !side || !drink) return;

    const variant = menuVariantOf(category, base);
    if (!variant) {
      this.sheetProduct.set(null);
      return;
    }

    this.cart.addCombo(
      {
        id: variant.id,
        name: variant.name,
        price: variant.price,
        imagePath: variant.imagePath ?? base.imagePath,
      },
      {
        id: side.menuItemId,
        name: side.menuItemName,
        supplementPrice: side.supplementPrice,
        imagePath: side.menuItemImagePath,
      },
      {
        id: drink.menuItemId,
        name: drink.menuItemName,
        supplementPrice: drink.supplementPrice,
        imagePath: drink.menuItemImagePath,
      },
    );

    this.sheetProduct.set(null);
    this.toastRef().show('Menu ajouté au panier', variant.name);
    setTimeout(() => {
      void this.router.navigate(['/menu', this.slug, this.tableId]);
    }, 400);
  }
}
