import { Component, input, output } from '@angular/core';
import { ItemView } from '../../models/menu.model';
import { ItemCardComponent } from '../item-card/item-card.component';

@Component({
  selector: 'app-item-list',
  standalone: true,
  imports: [ItemCardComponent],
  template: `
    <div class="grid gap-3">
      @for (item of items(); track item.id) {
        @if (item.available && item.menuVariantOf === null) {
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

  canComposeAsMenu(item: ItemView): boolean {
    return this.items().some((candidate) => candidate.menuVariantOf === item.id && candidate.available);
  }
}
