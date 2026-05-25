import { Component, computed, inject, input, output } from '@angular/core';
import { ItemView } from '../../models/menu.model';
import { CartService } from '../../../cart/services/cart.service';
import { PricePipe } from '../../../../shared/pipes/price.pipe';

@Component({
  selector: 'app-item-modal',
  standalone: true,
  imports: [PricePipe],
  template: `
    <div class="fixed inset-0 z-50 flex items-end justify-center" (click)="close.emit()">
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
      <div class="relative w-full max-w-[42rem] rounded-t-[2rem] border border-outline-variant/60 bg-surface-container-low p-5 shadow-[var(--shadow-strong)] animate-slide-up safe-bottom-pad sm:p-6"
           (click)="$event.stopPropagation()">
        <div class="mx-auto mb-4 h-1.5 w-14 rounded-full bg-outline-variant"></div>
        <!-- Close -->
        <button class="absolute top-4 right-4 w-8 h-8 rounded-full bg-surface-container flex items-center justify-center text-on-surface-variant"
                (click)="close.emit()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>

        <!-- Image -->
        @if (item().imagePath) {
          <div class="mb-4 w-full overflow-hidden rounded-[1.75rem]">
            <img [src]="item().imagePath!" [alt]="item().name" class="w-full h-full object-cover" />
          </div>
        }

        <!-- Info -->
        <h2 class="font-display text-xl font-bold text-on-surface">{{ item().name }}</h2>
        @if (item().description) {
          <p class="font-body text-on-surface-variant text-sm mt-1">{{ item().description }}</p>
        }
        <p class="font-display font-extrabold text-primary text-2xl mt-3">{{ item().price | price }}</p>

        <!-- Actions -->
        @if (showMenuChoice()) {
          <div class="mt-6 space-y-3">
            <button class="w-full py-3 rounded-full bg-primary text-on-primary font-display font-bold text-base active:scale-[0.98] transition-transform"
                    (click)="addToCartStandalone()">
              Ajouter seul
            </button>
            <button class="w-full py-3 rounded-full bg-secondary-container text-on-secondary-container font-display font-bold text-base active:scale-[0.98] transition-transform"
                    (click)="startCombo.emit()">
              Composer en menu
            </button>
          </div>
        } @else {
          <button class="w-full mt-6 py-3 rounded-full bg-primary text-on-primary font-display font-bold text-base active:scale-[0.98] transition-transform"
                  (click)="addToCartStandalone()">
            Ajouter au panier
          </button>
        }
      </div>
    </div>
  `,
  styles: [`
    .animate-slide-up {
      animation: slideUp 0.3s ease-out;
    }
    @keyframes slideUp {
      from { transform: translateY(100%); }
      to { transform: translateY(0); }
    }
  `],
})
export class ItemModalComponent {
  item = input.required<ItemView>();
  canComposeAsMenu = input<boolean>(false);
  close = output<void>();
  startCombo = output<void>();

  private readonly cart = inject(CartService);

  showMenuChoice = computed(() => this.canComposeAsMenu());

  addToCartStandalone(): void {
    const i = this.item();
    this.cart.addStandalone(i.id, i.name, i.price, i.imagePath);
    this.close.emit();
  }
}
