import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  Output,
  computed,
  signal,
} from '@angular/core';
import { CategoryView, MenuItemView } from '../../core/services/menu.service';
import {
  LucideChevronDown,
  LucideCheck,
  LucideSearch,
  LucideImage,
} from '@lucide/angular';

interface PickerGroup {
  category: CategoryView;
  items: MenuItemView[];
}

/**
 * Custom product dropdown for the menu Formula step.
 *
 * Replaces the native <select>: products are grouped by category, each row
 * shows a thumbnail + name + unit price, and a search field filters across
 * all categories. Used for both the Accompagnements and Boissons panels.
 *
 * Note: the admin app is zoneless, so all mutable UI state lives in signals —
 * plain fields mutated in async/event callbacks would never re-render.
 */
@Component({
  selector: 'app-product-picker',
  imports: [LucideChevronDown, LucideCheck, LucideSearch, LucideImage],
  templateUrl: './product-picker.component.html',
  styleUrl: './product-picker.component.scss',
})
export class ProductPickerComponent {
  @Input() items: MenuItemView[] = [];
  @Input() categories: CategoryView[] = [];

  /**
   * Selected item id, kept in a signal so the `selectedItem` computed below is
   * reactive. The admin app is zoneless — a plain @Input field read inside a
   * computed would never trigger a re-evaluation, so the trigger label would
   * stay stuck on the placeholder after a pick.
   */
  private _value = signal('');
  @Input() set value(v: string) {
    this._value.set(v ?? '');
  }
  get value(): string {
    return this._value();
  }
  @Output() selection = new EventEmitter<string>();

  open = signal(false);
  query = signal('');

  /** Currently selected item, if any — drives the trigger label/thumbnail. */
  selectedItem = computed(() => this.items.find(i => i.id === this._value()) ?? null);

  /** Items whose name matches the current search query. */
  private filteredItems = computed(() => {
    const q = this.query().trim().toLowerCase();
    if (!q) return this.items;
    return this.items.filter(i => i.name.toLowerCase().includes(q));
  });

  /**
   * Items grouped by category (categories sorted by position), with empty
   * categories dropped after filtering.
   */
  groups = computed<PickerGroup[]>(() => {
    const filtered = this.filteredItems();
    return this.categories
      .slice()
      .sort((a, b) => a.position - b.position)
      .map(category => ({
        category,
        items: filtered.filter(i => i.categoryId === category.id),
      }))
      .filter(g => g.items.length > 0);
  });

  toggle(): void {
    this.open.update(v => !v);
    if (!this.open()) {
      this.query.set('');
    }
  }

  pick(itemId: string): void {
    this.selection.emit(itemId);
    this.open.set(false);
    this.query.set('');
  }

  /** Close when a click lands outside this component's host element. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.open()) return;
    if (!(event.target as Element | null)?.closest('app-product-picker')) {
      this.open.set(false);
      this.query.set('');
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open()) {
      this.open.set(false);
      this.query.set('');
    }
  }

  formatPrice(price: number): string {
    return price.toFixed(2).replace('.', ',') + ' €';
  }
}
