import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PriceInputComponent } from '../../shared/price-input/price-input.component';
import { forkJoin } from 'rxjs';
import {
  MenuService,
  CategoryView,
  MenuItemView,
} from '../../core/services/menu.service';
import { ImageService } from '../../core/services/image.service';
import { ToastService } from '../../core/services/toast.service';
import { ConfirmService } from '../../core/services/confirm.service';
import { ADMIN_ICONS } from '../../core/icons';
import { ProductPickerComponent } from '../../shared/product-picker/product-picker.component';

@Component({
  selector: 'app-menu-management',
  imports: [ReactiveFormsModule, ProductPickerComponent, PriceInputComponent, ...ADMIN_ICONS],
  templateUrl: './menu-management.component.html',
  styleUrl: './menu-management.component.scss',
})
export class MenuManagementComponent implements OnInit {
  private menu = inject(MenuService);
  private image = inject(ImageService);
  /** Public so the recap CTA can call it from the template. */
  readonly toast = inject(ToastService);
  private confirm = inject(ConfirmService);

  currentStep = signal(1);
  loading = signal(false);

  // Pending states: a save/delete chains several requests (create, image
  // upload, refresh), so the triggering button stays disabled with a spinner
  // until the whole chain ends — this also blocks double submissions.
  savingCategory = signal(false);
  savingItem = signal(false);
  busyIds = signal<ReadonlySet<string>>(new Set());

  editingCategoryId = signal<string | null>(null);
  editingItemId = signal<string | null>(null);
  selectedCategoryId = signal<string | null>(null);

  categories = this.menu.categories;
  menuItems = this.menu.menuItems;
  compositions = this.menu.compositions;

  /** Base dishes (no menu variant) for the active category tab. */
  itemsForCategory = computed(() => {
    const catId = this.selectedCategoryId();
    if (!catId) {
      return this.menuItems().filter(i => i.menuVariantOf === null);
    }
    return this.menuItems().filter(i => i.menuVariantOf === null && i.categoryId === catId);
  });

  /** All base items available to attach as a formula option. */
  eligibleItems = computed(() =>
    this.menuItems().filter(i => i.available && i.menuVariantOf === null),
  );

  accCompositions = computed(() =>
    this.compositions().filter(c => c.compositionType === 'accompagnement'),
  );
  drinkCompositions = computed(() =>
    this.compositions().filter(c => c.compositionType === 'boisson'),
  );

  categorySaveLabel = computed(() => {
    if (this.savingCategory()) return this.editingCategoryId() ? 'Enregistrement…' : 'Ajout…';
    return this.editingCategoryId() ? 'Enregistrer' : 'Ajouter';
  });
  itemSaveLabel = computed(() => {
    if (this.savingItem()) return this.editingItemId() ? 'Enregistrement…' : 'Ajout…';
    return this.editingItemId() ? 'Enregistrer' : 'Ajouter';
  });

  // ── Category form ──────────────────────────────────────────────────
  categoryForm = new FormGroup({
    name: new FormControl('', Validators.required),
    hasMenu: new FormControl(false),
  });
  catImageFile = signal<File | null>(null);
  catImagePreview = signal<string>('');

  // ── Item form ──────────────────────────────────────────────────────
  itemForm = new FormGroup({
    name: new FormControl('', Validators.required),
    price: new FormControl(0, [Validators.required, Validators.min(0)]),
    isCombo: new FormControl(false),
    comboPrice: new FormControl(0, [Validators.min(0)]),
    description: new FormControl(''),
  });
  itemImageFile = signal<File | null>(null);
  itemImagePreview = signal<string>('');

  // ── Formula add rows ───────────────────────────────────────────────
  // Supplement is a FormControl<number|null> driven by <app-price-input>,
  // which keeps its own string buffer so decimals can be typed freely.
  accItemId = signal<string>('');
  accSupplement = new FormControl<number | null>(null);
  drinkItemId = signal<string>('');
  drinkSupplement = new FormControl<number | null>(null);

  // ── Lifecycle ──────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    forkJoin({
      categories: this.menu.loadCategories(),
      items: this.menu.loadMenuItems(),
      compositions: this.menu.loadCompositions(),
    }).subscribe({ complete: () => this.loading.set(false) });
  }

  // ── Step navigation ────────────────────────────────────────────────
  goToStep(step: number): void {
    // Steps 2+ require categories to exist.
    if (step > 1 && this.categories().length === 0) {
      this.toast.show('Créez d\'abord une catégorie');
      return;
    }
    this.currentStep.set(step);
    if (step === 2 && !this.selectedCategoryId() && this.categories().length) {
      this.selectCategory(this.categories()[0].id);
    }
  }
  nextStep(): void {
    this.goToStep(this.currentStep() + 1);
  }
  prevStep(): void {
    this.goToStep(Math.max(1, this.currentStep() - 1));
  }

  // ── Category CRUD ──────────────────────────────────────────────────
  selectedCategoryAllowsMenu(): boolean {
    const id = this.selectedCategoryId();
    return this.categories().some(c => c.id === id && c.hasMenu);
  }

  saveCategory(): void {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }
    const val = this.categoryForm.value;
    const editId = this.editingCategoryId();
    const file = this.catImageFile();

    this.savingCategory.set(true);
    const done = () => {
      this.savingCategory.set(false);
      this.toast.show(editId ? 'Catégorie modifiée' : 'Catégorie ajoutée');
      this.menu.loadCategories().subscribe();
      this.cancelCategoryEdit();
    };
    const fail = (err: any) => {
      this.savingCategory.set(false);
      this.toast.show(err.error?.message ?? err.error?.error ?? 'Erreur');
    };

    if (editId) {
      this.menu.updateCategory(editId, { name: val.name!, hasMenu: val.hasMenu ?? false }).subscribe({
        next: (cat) => {
          if (file) {
            this.image.upload('category-images', file).subscribe({
              next: (res) => this.menu.updateCategory(cat.id, { imagePath: res.url }).subscribe({ next: done, error: fail }),
            });
          } else {
            done();
          }
        },
        error: fail,
      });
    } else {
      this.menu.createCategory({ name: val.name!, hasMenu: val.hasMenu ?? false }).subscribe({
        next: (cat) => {
          if (file) {
            this.image.upload('category-images', file).subscribe({
              next: (res) => this.menu.updateCategory(cat.id, { imagePath: res.url }).subscribe({ next: done, error: fail }),
            });
          } else {
            done();
          }
        },
        error: fail,
      });
    }
  }

  editCategory(cat: CategoryView): void {
    this.editingCategoryId.set(cat.id);
    this.categoryForm.patchValue({ name: cat.name, hasMenu: cat.hasMenu });
    this.catImageFile.set(null);
    this.catImagePreview.set(cat.imagePath ?? '');
    this.currentStep.set(1);
  }

  cancelCategoryEdit(): void {
    this.editingCategoryId.set(null);
    this.categoryForm.reset({ name: '', hasMenu: false });
    this.catImageFile.set(null);
    this.catImagePreview.set('');
  }

  async deleteCategory(id: string): Promise<void> {
    const cat = this.categories().find(c => c.id === id);
    const confirmed = await this.confirm.ask({
      title: 'Supprimer la catégorie',
      message: `Supprimer la catégorie « ${cat?.name} » et tous ses articles ? Cette action est irréversible.`,
      confirmLabel: 'Supprimer',
      tone: 'danger',
    });
    if (!confirmed) {
      return;
    }
    this.setBusy(id, true);
    this.menu.deleteCategory(id).subscribe({
      next: () => {
        this.setBusy(id, false);
        if (this.selectedCategoryId() === id) {
          this.selectedCategoryId.set(null);
        }
        this.toast.show('Catégorie supprimée');
        this.loadAll();
      },
      error: (err) => {
        this.setBusy(id, false);
        this.toast.show(err.error?.message ?? err.error?.error ?? 'Erreur');
      },
    });
  }

  onCategoryImagePicked(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      this.catImageFile.set(file);
      this.readPreview(file, this.catImagePreview);
    }
  }

  // ── Item CRUD ──────────────────────────────────────────────────────
  selectCategory(catId: string): void {
    this.selectedCategoryId.set(catId);
  }

  saveItem(): void {
    if (this.itemForm.invalid) {
      this.itemForm.markAllAsTouched();
      return;
    }
    const catId = this.selectedCategoryId();
    if (!catId) {
      this.toast.show('Sélectionnez une catégorie');
      return;
    }
    const val = this.itemForm.value;
    const editId = this.editingItemId();
    const file = this.itemImageFile();
    const wantsCombo = !editId && !!val.isCombo && (val.comboPrice ?? 0) > 0;

    if (wantsCombo && !this.selectedCategoryAllowsMenu()) {
      this.toast.show('Cette catégorie ne propose pas de menu');
      return;
    }

    const payload = {
      categoryId: catId,
      name: val.name!,
      description: val.description ?? '',
      price: val.price!,
    };

    this.savingItem.set(true);
    const fail = (err: any) => {
      this.savingItem.set(false);
      this.toast.show(err.error?.message ?? err.error?.error ?? 'Erreur');
    };
    const afterSaved = (item: MenuItemView) => {
      const finish = () => {
        this.savingItem.set(false);
        this.menu.loadMenuItems().subscribe();
        this.toast.show(editId ? 'Article modifié' : 'Article ajouté');
        this.cancelItemEdit();
      };
      if (file) {
        this.image.upload('menu-images', file).subscribe({
          next: (res) => this.menu.updateMenuItem(item.id, { imagePath: res.url }).subscribe({ next: finish, error: fail }),
        });
      } else {
        finish();
      }
    };

    if (editId) {
      this.menu.updateMenuItem(editId, payload).subscribe({
        next: (item) => {
          const variant = this.menuVariantOf(item);
          if (!variant) {
            afterSaved(item);
            return;
          }
          // Keep the "Menu X" variant in sync with its base item.
          this.menu
            .updateMenuItem(variant.id, {
              name: `Menu ${val.name!}`,
              price: val.comboPrice ?? variant.price,
            })
            .subscribe({
              next: () => afterSaved(item),
              error: () => {
                this.savingItem.set(false);
                this.menu.loadMenuItems().subscribe();
                this.toast.show('Article modifié (prix menu non mis à jour)');
                this.cancelItemEdit();
              },
            });
        },
        error: fail,
      });
    } else {
      this.menu.createMenuItem(payload).subscribe({
        next: (item) => {
          if (!wantsCombo) {
            afterSaved(item);
            return;
          }
          // Combo: create the menu variant first, then reuse the plain-item
          // save flow so the base item's image is uploaded and the list is
          // refreshed exactly once — after the image is attached. Reloading
          // before the upload (as this used to) left the card imageless until
          // the next item add.
          this.menu
            .createMenuItem({
              categoryId: catId,
              name: `Menu ${val.name}`,
              price: val.comboPrice!,
              menuVariantOf: item.id,
            })
            .subscribe({
              next: () => afterSaved(item),
              error: () => {
                this.savingItem.set(false);
                this.menu.loadMenuItems().subscribe();
                this.toast.show('Article ajouté (variante menu ignorée)');
                this.cancelItemEdit();
              },
            });
        },
        error: fail,
      });
    }
  }

  editItem(item: MenuItemView): void {
    this.editingItemId.set(item.id);
    this.selectCategory(item.categoryId);
    const variant = this.menuVariantOf(item);
    this.itemForm.patchValue({
      name: item.name,
      description: item.description ?? '',
      price: item.price,
      isCombo: !!variant,
      comboPrice: variant ? variant.price : 0,
    });
    this.itemImageFile.set(null);
    this.itemImagePreview.set(item.imagePath ?? '');
  }

  cancelItemEdit(): void {
    this.editingItemId.set(null);
    this.itemForm.reset({ name: '', price: 0, isCombo: false, comboPrice: 0, description: '' });
    this.itemImageFile.set(null);
    this.itemImagePreview.set('');
  }

  toggleAvailability(item: MenuItemView): void {
    this.setBusy(item.id, true);
    this.menu.toggleAvailability(item.id, !item.available).subscribe({
      next: () => {
        this.setBusy(item.id, false);
        this.menu.loadMenuItems().subscribe();
      },
      error: (err) => {
        this.setBusy(item.id, false);
        this.toast.show(err.error?.message ?? err.error?.error ?? 'Erreur');
      },
    });
  }

  async deleteItem(id: string): Promise<void> {
    const confirmed = await this.confirm.ask({
      title: 'Supprimer l\'article',
      message: 'Supprimer cet article ? Cette action est irréversible.',
      confirmLabel: 'Supprimer',
      tone: 'danger',
    });
    if (!confirmed) {
      return;
    }
    this.setBusy(id, true);
    this.menu.deleteMenuItem(id).subscribe({
      next: () => {
        this.setBusy(id, false);
        this.menu.loadMenuItems().subscribe();
        this.menu.loadCompositions().subscribe();
        this.toast.show('Article supprimé');
      },
      error: (err) => {
        this.setBusy(id, false);
        this.toast.show(err.error?.message ?? err.error?.error ?? 'Erreur');
      },
    });
  }

  /** Marks one card (category or item) as busy so its actions show a spinner. */
  private setBusy(id: string, busy: boolean): void {
    const next = new Set(this.busyIds());
    if (busy) {
      next.add(id);
    } else {
      next.delete(id);
    }
    this.busyIds.set(next);
  }

  onItemImagePicked(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      this.itemImageFile.set(file);
      this.readPreview(file, this.itemImagePreview);
    }
  }

  /** The menu-variant ("Menu X") of a base item, if any — its price is the menu price. */
  menuVariantOf(baseItem: MenuItemView): MenuItemView | undefined {
    return this.menuItems().find(m => m.menuVariantOf === baseItem.id);
  }

  itemCount(catId: string): number {
    return this.menuItems().filter(i => i.categoryId === catId && i.menuVariantOf === null).length;
  }

  // ── Composition (formula) CRUD ─────────────────────────────────────
  addComposition(type: 'accompagnement' | 'boisson'): void {
    const itemId = type === 'accompagnement' ? this.accItemId() : this.drinkItemId();
    const supplementControl = type === 'accompagnement' ? this.accSupplement : this.drinkSupplement;
    const supplement = supplementControl.value ?? 0;
    if (!itemId) {
      this.toast.show('Sélectionnez un produit');
      return;
    }
    this.menu.createComposition({ compositionType: type, menuItemId: itemId, supplementPrice: supplement }).subscribe({
      next: () => {
        this.menu.loadCompositions().subscribe();
        if (type === 'accompagnement') {
          this.accItemId.set('');
        } else {
          this.drinkItemId.set('');
        }
        supplementControl.reset(null);
        this.toast.show('Ajouté aux formules');
      },
      error: (err) => this.toast.show(err.error?.message ?? err.error?.error ?? 'Erreur'),
    });
  }

  deleteComposition(id: string): void {
    this.menu.deleteComposition(id).subscribe({
      next: () => this.menu.loadCompositions().subscribe(),
      error: (err) => this.toast.show(err.error?.message ?? err.error?.error ?? 'Erreur'),
    });
  }

  // ── Helpers ────────────────────────────────────────────────────────
  getItemName(itemId: string): string {
    return this.menuItems().find(i => i.id === itemId)?.name ?? '—';
  }
  getItemImage(itemId: string): string | null {
    return this.menuItems().find(i => i.id === itemId)?.imagePath ?? null;
  }
  getCategoryName(catId: string): string {
    return this.categories().find(c => c.id === catId)?.name ?? '—';
  }
  formatPrice(price: number): string {
    return price.toFixed(2).replace('.', ',') + ' €';
  }

  private readPreview(file: File, target: { set: (v: string) => void }): void {
    const reader = new FileReader();
    reader.onload = e => target.set(e.target?.result as string);
    reader.readAsDataURL(file);
  }
}
