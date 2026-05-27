import { Component, computed, input, output } from '@angular/core';
import { ItemView } from '../../models/menu.model';
import { ItemCardComponent } from '../item-card/item-card.component';

@Component({
  selector: 'app-item-list',
  standalone: true,
  imports: [ItemCardComponent],
  template: `
    <div class="grid gap-4">
      @if (visibleItems().length === 0) {
        <div class="rounded-[1.75rem] border border-outline-variant/70 bg-surface-container-low px-5 py-8 text-center shadow-[var(--shadow-soft)]">
          <p class="font-display text-base font-bold text-on-surface">Cette catégorie revient très vite.</p>
          <p class="mt-2 text-sm text-on-surface-variant">
            Aucun plat n'est disponible pour le moment.
          </p>
        </div>
      } @else {
        @for (item of visibleItems(); track item.id) {
          <app-item-card
            [item]="item"
            [canComposeAsMenu]="canComposeAsMenu(item)"
            (cardClick)="itemClick.emit($event)" />
        }
      }
    </div>
  `,
})
export class ItemListComponent {
  items = input.required<ItemView[]>();
  itemClick = output<ItemView>();
  readonly visibleItems = computed(() =>
    this.items().filter((item) => item.available && item.menuVariantOf === null),
  );

  canComposeAsMenu(item: ItemView): boolean {
    return this.items().some((candidate) => candidate.menuVariantOf === item.id && candidate.available);
  }
}
