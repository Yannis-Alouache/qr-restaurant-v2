import { Component, input, output } from '@angular/core';
import { ItemView } from '../../models/menu.model';
import { PricePipe } from '../../../../shared/pipes/price.pipe';

@Component({
  selector: 'app-item-card',
  standalone: true,
  imports: [PricePipe],
  template: `
    <button class="w-full bg-surface-container-lowest rounded-2xl shadow-sm overflow-hidden flex gap-4 p-4 items-center active:scale-[0.98] transition-transform text-left"
            (click)="cardClick.emit(item())">
      <!-- Image -->
      <div class="w-20 h-20 rounded-xl bg-surface-container flex-shrink-0 overflow-hidden">
        @if (item().imagePath) {
          <img [src]="item().imagePath!" [alt]="item().name" class="w-full h-full object-cover rounded-xl" />
        } @else {
          <div class="w-full h-full flex items-center justify-center text-2xl">🍽️</div>
        }
      </div>

      <!-- Info -->
      <div class="flex-1 min-w-0">
        <h3 class="font-display font-bold text-on-surface text-base truncate">{{ item().name }}</h3>
        @if (item().description) {
          <p class="font-body text-on-surface-variant text-sm mt-0.5 line-clamp-2">{{ item().description }}</p>
        }
        <div class="flex items-center justify-between mt-2">
          <span class="font-display font-extrabold text-primary text-lg">{{ item().price | price }}</span>
          @if (canComposeAsMenu()) {
            <span class="text-xs bg-secondary/10 text-secondary px-2 py-0.5 rounded-full font-display font-bold">Menu dispo</span>
          }
        </div>
      </div>
    </button>
  `,
})
export class ItemCardComponent {
  item = input.required<ItemView>();
  canComposeAsMenu = input<boolean>(false);
  cardClick = output<ItemView>();
}
