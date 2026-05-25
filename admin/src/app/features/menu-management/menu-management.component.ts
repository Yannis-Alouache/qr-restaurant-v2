import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { switchMap } from 'rxjs';
import {
  MenuService,
  CategoryView,
  MenuItemView,
  CompositionView,
} from '../../core/services/menu.service';
import { ImageService } from '../../core/services/image.service';

type Tab = 'categories' | 'items' | 'compositions';

@Component({
  selector: 'app-menu-management',
  imports: [ReactiveFormsModule],
  templateUrl: './menu-management.component.html',
  styleUrl: './menu-management.component.scss',
})
export class MenuManagementComponent implements OnInit {
  private menu = inject(MenuService);
  private image = inject(ImageService);

  activeTab = signal<Tab>('categories');
  loading = signal(false);
  editingCategoryId = signal<string | null>(null);
  editingItemId = signal<string | null>(null);

  categories = this.menu.categories;
  menuItems = this.menu.menuItems;
  compositions = this.menu.compositions;

  itemsForSelectedCategory = computed(() => {
    const catId = this.selectedCategoryId();
    if (!catId) return this.menuItems();
    return this.menuItems().filter((i) => i.categoryId === catId);
  });

  baseItems = computed(() => this.menuItems().filter((i) => i.menuVariantOf === null));

  allItemsForCompositions = computed(() =>
    this.menuItems().filter((item) => item.available && item.menuVariantOf === null),
  );

  selectedCategoryId = signal<string | null>(null);
  compositionFilter = signal<'accompagnement' | 'boisson' | null>(null);

  filteredCompositions = computed(() => {
    const filter = this.compositionFilter();
    if (!filter) return this.compositions();
    return this.compositions().filter((c) => c.compositionType === filter);
  });

  // Category form
  categoryForm = new FormGroup({
    name: new FormControl('', Validators.required),
    hasMenu: new FormControl(false),
  });

  // Item form
  itemForm = new FormGroup({
    categoryId: new FormControl('', Validators.required),
    name: new FormControl('', Validators.required),
    description: new FormControl(''),
    price: new FormControl(0, [Validators.required, Validators.min(0)]),
    isCombo: new FormControl(false),
    comboPrice: new FormControl(0, [Validators.min(0)]),
  });

  // Composition form
  compositionForm = new FormGroup({
    compositionType: new FormControl<'accompagnement' | 'boisson'>(
      'accompagnement',
      Validators.required,
    ),
    menuItemId: new FormControl('', Validators.required),
    supplementPrice: new FormControl(0, [Validators.min(0)]),
  });

  ngOnInit(): void {
    this.loadAll();
    this.itemForm.controls.categoryId.valueChanges.subscribe(() => {
      if (!this.selectedItemCategoryAllowsMenu()) {
        this.itemForm.patchValue({ isCombo: false, comboPrice: 0 }, { emitEvent: false });
      }
    });
  }

  loadAll(): void {
    this.loading.set(true);
    Promise.all([
      this.menu.loadCategories().toPromise(),
      this.menu.loadMenuItems().toPromise(),
      this.menu.loadCompositions().toPromise(),
    ]).finally(() => this.loading.set(false));
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
  }

  // Category CRUD
  saveCategory(): void {
    if (this.categoryForm.invalid) return;
    const val = this.categoryForm.value;
    const editId = this.editingCategoryId();

    const obs = editId
      ? this.menu.updateCategory(editId, { name: val.name!, hasMenu: val.hasMenu ?? false })
      : this.menu.createCategory({ name: val.name!, hasMenu: val.hasMenu ?? false });

    obs.subscribe({
      next: () => {
        this.menu.loadCategories().subscribe();
        this.cancelCategoryEdit();
      },
      error: (err) => alert(err.error?.error ?? 'Erreur'),
    });
  }

  editCategory(cat: CategoryView): void {
    this.editingCategoryId.set(cat.id);
    this.categoryForm.patchValue({ name: cat.name, hasMenu: cat.hasMenu });
  }

  cancelCategoryEdit(): void {
    this.editingCategoryId.set(null);
    this.categoryForm.reset({ name: '', hasMenu: false });
  }

  deleteCategory(id: string): void {
    if (!confirm('Supprimer cette catégorie et tous ses articles ?')) return;
    this.menu.deleteCategory(id).subscribe({
      next: () => {
        if (this.selectedCategoryId() === id) {
          this.selectedCategoryId.set(null);
        }
        this.loadAll();
      },
      error: (err) => alert(err.error?.error ?? 'Erreur'),
    });
  }

  // Item CRUD
  selectCategoryForItems(catId: string): void {
    this.selectedCategoryId.set(catId);
  }

  saveItem(): void {
    if (this.itemForm.invalid) return;
    const val = this.itemForm.value;
    const editId = this.editingItemId();
    const createComboVariant = !editId && !!val.isCombo && !!val.comboPrice && val.comboPrice > 0;

    if (createComboVariant && !this.selectedItemCategoryAllowsMenu()) {
      alert('La catégorie sélectionnée ne permet pas de créer une variante menu');
      return;
    }

    const payload: any = {
      categoryId: val.categoryId!,
      name: val.name!,
      description: val.description ?? '',
      price: val.price!,
    };

    const obs = editId
      ? this.menu.updateMenuItem(editId, payload)
      : this.menu.createMenuItem(payload);

    obs.subscribe({
      next: (item) => {
        const comboVariant$ = createComboVariant
          ? this.menu
              .createMenuItem({
                categoryId: val.categoryId!,
                name: `Menu ${val.name}`,
                price: val.comboPrice!,
                menuVariantOf: item.id,
              })
              .pipe(switchMap(() => this.menu.loadMenuItems()))
          : this.menu.loadMenuItems();

        comboVariant$.subscribe({
          next: () => {
            this.cancelItemEdit();
          },
          error: (err) => {
            this.menu.loadMenuItems().subscribe();
            alert(err.error?.error ?? 'Erreur');
          },
        });
      },
      error: (err) => alert(err.error?.error ?? 'Erreur'),
    });
  }

  editItem(item: MenuItemView): void {
    this.editingItemId.set(item.id);
    this.itemForm.patchValue({
      categoryId: item.categoryId,
      name: item.name,
      description: item.description ?? '',
      price: item.price,
      isCombo: false,
      comboPrice: 0,
    });
  }

  cancelItemEdit(): void {
    this.editingItemId.set(null);
    this.itemForm.reset({
      categoryId: '',
      name: '',
      description: '',
      price: 0,
      isCombo: false,
      comboPrice: 0,
    });
  }

  toggleAvailability(item: MenuItemView): void {
    this.menu.toggleAvailability(item.id, !item.available).subscribe({
      next: () => this.menu.loadMenuItems().subscribe(),
      error: (err) => alert(err.error?.error ?? 'Erreur'),
    });
  }

  deleteItem(id: string): void {
    if (!confirm('Supprimer cet article ?')) return;
    this.menu.deleteMenuItem(id).subscribe({
      next: () => {
        this.menu.loadMenuItems().subscribe();
        this.menu.loadCompositions().subscribe();
      },
      error: (err) => alert(err.error?.error ?? 'Erreur'),
    });
  }

  // Composition CRUD
  saveComposition(): void {
    if (this.compositionForm.invalid) return;
    const val = this.compositionForm.value;

    this.menu
      .createComposition({
        compositionType: val.compositionType!,
        menuItemId: val.menuItemId!,
        supplementPrice: val.supplementPrice ?? 0,
      })
      .subscribe({
        next: () => {
          this.menu.loadCompositions().subscribe();
          this.compositionForm.reset({
            compositionType: 'accompagnement',
            menuItemId: '',
            supplementPrice: 0,
          });
        },
        error: (err) => alert(err.error?.error ?? 'Erreur'),
      });
  }

  deleteComposition(id: string): void {
    this.menu.deleteComposition(id).subscribe({
      next: () => this.menu.loadCompositions().subscribe(),
      error: (err) => alert(err.error?.error ?? 'Erreur'),
    });
  }

  // Image upload
  onCategoryImageUpload(event: Event, categoryId: string): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    this.image.upload('category-images', file).subscribe({
      next: (res) => {
        this.menu.updateCategory(categoryId, { imagePath: res.url }).subscribe({
          next: () => this.menu.loadCategories().subscribe(),
        });
      },
      error: () => alert("Erreur lors de l'upload"),
    });
  }

  onItemImageUpload(event: Event, itemId: string): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    this.image.upload('menu-images', file).subscribe({
      next: (res) => {
        this.menu.updateMenuItem(itemId, { imagePath: res.url }).subscribe({
          next: () => this.menu.loadMenuItems().subscribe(),
        });
      },
      error: () => alert("Erreur lors de l'upload"),
    });
  }

  getItemName(itemId: string): string {
    return this.menuItems().find((i) => i.id === itemId)?.name ?? '—';
  }

  getCategoryName(catId: string): string {
    return this.categories().find((c) => c.id === catId)?.name ?? '—';
  }

  formatPrice(price: number): string {
    return price.toFixed(2).replace('.', ',') + ' €';
  }

  selectedItemCategoryAllowsMenu(): boolean {
    const categoryId = this.itemForm.controls.categoryId.value;
    return this.categories().some((category) => category.id === categoryId && category.hasMenu);
  }
}
