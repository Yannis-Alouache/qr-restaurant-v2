import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MenuService } from '../../services/menu.service';
import { CategoryView, MenuView } from '../../models/menu.model';
import { ThemeService } from '../../../../shared/services/theme.service';
import {
  categoryCover,
  categoryHasMenus,
  minMenuSavings,
} from '../../utils/menu-utils';

/** Écran 2 de la maquette : choix entre Formule Menu (mise en avant) et article solo. */
@Component({
  selector: 'app-order-type-screen',
  templateUrl: './order-type-screen.component.html',
  styleUrl: './order-type-screen.component.scss',
})
export class OrderTypeScreenComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly menuService = inject(MenuService);
  private readonly themeService = inject(ThemeService);

  menu = signal<MenuView | null>(null);
  category = signal<CategoryView | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  protected readonly slug = this.route.snapshot.paramMap.get('slug') ?? '';
  protected readonly tableId = this.route.snapshot.paramMap.get('tableId') ?? '';
  private readonly categoryId = this.route.snapshot.paramMap.get('categoryId') ?? '';

  readonly menuCover = computed(() => {
    const category = this.category();
    return category ? categoryCover(category) : null;
  });

  readonly soloCover = computed(() => {
    const category = this.category();
    if (!category) return null;
    const illustrated = category.items.find(
      item => item.imagePath && item.menuVariantOf === null,
    );
    return illustrated?.imagePath ?? categoryCover(category);
  });

  /** Économie réelle garantie par la formule (badge « −X € » de la maquette). */
  readonly savings = computed(() => {
    const menu = this.menu();
    const category = this.category();
    if (!menu || !category) return null;
    return minMenuSavings(category, menu.compositions, menu.categories);
  });

  readonly savingsLabel = computed(() => {
    const value = this.savings();
    return value !== null ? `−${value.toFixed(2).replace('.', ',')} €` : null;
  });

  ngOnInit(): void {
    if (!this.slug || !this.categoryId) {
      this.error.set('Catégorie introuvable');
      this.loading.set(false);
      return;
    }

    this.menuService.getMenu(this.slug).subscribe({
      next: data => {
        this.menu.set(data);
        this.themeService.apply(data.restaurant.themeId);
        const category = data.categories.find(c => c.id === this.categoryId) ?? null;
        this.loading.set(false);

        if (!category) {
          this.error.set('Catégorie introuvable');
          return;
        }
        this.category.set(category);

        // Catégorie sans formule : l'écran de choix est inutile.
        if (!categoryHasMenus(category)) {
          void this.router.navigate(
            ['/menu', this.slug, this.tableId, 'c', this.categoryId, 'solo'],
            { replaceUrl: true },
          );
        }
      },
      error: () => {
        this.error.set('Impossible de charger le menu');
        this.loading.set(false);
      },
    });
  }

  chooseMenu(): void {
    void this.router.navigate(['/menu', this.slug, this.tableId, 'c', this.categoryId, 'menu']);
  }

  chooseSolo(): void {
    void this.router.navigate(['/menu', this.slug, this.tableId, 'c', this.categoryId, 'solo']);
  }

  goBack(): void {
    void this.router.navigate(['/menu', this.slug, this.tableId]);
  }
}
