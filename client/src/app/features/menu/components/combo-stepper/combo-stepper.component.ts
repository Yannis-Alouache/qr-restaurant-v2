import { Component, input, output, signal, computed, inject } from '@angular/core';
import { ItemView, CompositionEntry } from '../../models/menu.model';
import { CartService } from '../../../cart/services/cart.service';
import { PricePipe } from '../../../../shared/pipes/price.pipe';

@Component({
  selector: 'app-combo-stepper',
  standalone: true,
  imports: [PricePipe],
  template: `
    <div class="fixed inset-0 z-50 flex items-end justify-center" (click)="cancel.emit()">
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
      <div class="relative max-h-[85vh] w-full max-w-[42rem] overflow-y-auto rounded-t-[2rem] border border-outline-variant/60 bg-surface-container-low p-5 shadow-[var(--shadow-strong)] animate-slide-up safe-bottom-pad sm:p-6"
           (click)="$event.stopPropagation()">
        <div class="mx-auto mb-4 h-1.5 w-14 rounded-full bg-outline-variant"></div>

        <!-- Header -->
        <div class="flex items-center justify-between mb-4">
          <h2 class="font-display text-lg font-bold text-on-surface">
            {{ step() === 1 ? 'Choisissez votre plat' : step() === 2 ? 'Choisissez votre accompagnement' : 'Choisissez votre boisson' }}
          </h2>
          <button class="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center text-on-surface-variant"
                  (click)="cancel.emit()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <!-- Progress -->
        <div class="flex gap-2 mb-6">
          @for (s of [1, 2, 3]; track s) {
            <div class="flex-1 h-1.5 rounded-full transition-colors"
                 [class]="s <= step() ? 'bg-primary' : 'bg-surface-container-high'"></div>
          }
        </div>

        <!-- Step 1: Main selection -->
        @if (step() === 1) {
          <div class="space-y-3">
            @for (item of comboVariants(); track item.id) {
              <button class="w-full flex items-center gap-3 p-3 rounded-2xl transition-all active:scale-[0.98]"
                      [class]="selectedMain()?.id === item.id ? 'bg-primary/10 ring-2 ring-primary' : 'bg-surface-container hover:bg-surface-container-high'"
                      (click)="selectMain(item)">
                <div class="w-14 h-14 rounded-xl bg-surface-container flex-shrink-0 overflow-hidden">
                  @if (item.imagePath) {
                    <img [src]="item.imagePath" [alt]="item.name" class="w-full h-full object-cover" />
                  } @else {
                    <div class="w-full h-full flex items-center justify-center text-xl">🍽️</div>
                  }
                </div>
                <div class="flex-1 text-left">
                  <p class="font-display font-semibold text-on-surface text-sm">{{ item.name }}</p>
                  <p class="font-display font-extrabold text-primary">{{ item.price | price }}</p>
                </div>
                @if (selectedMain()?.id === item.id) {
                  <div class="w-6 h-6 rounded-full bg-primary flex items-center justify-center">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
                  </div>
                }
              </button>
            }
          </div>
        }

        <!-- Step 2: Side selection -->
        @if (step() === 2) {
          <div class="space-y-3">
            @for (comp of sides(); track comp.menuItemId) {
              <button class="w-full flex items-center gap-3 p-3 rounded-2xl transition-all active:scale-[0.98]"
                      [class]="selectedSide()?.menuItemId === comp.menuItemId ? 'bg-primary/10 ring-2 ring-primary' : 'bg-surface-container hover:bg-surface-container-high'"
                      (click)="selectSide(comp)">
                <div class="w-14 h-14 rounded-xl bg-surface-container flex-shrink-0 overflow-hidden">
                  @if (comp.menuItemImagePath) {
                    <img [src]="comp.menuItemImagePath" [alt]="comp.menuItemName" class="w-full h-full object-cover" />
                  } @else {
                    <div class="w-full h-full flex items-center justify-center text-xl">🍟</div>
                  }
                </div>
                <div class="flex-1 text-left">
                  <p class="font-display font-semibold text-on-surface text-sm">{{ comp.menuItemName }}</p>
                  <p class="font-display font-bold text-sm"
                     [class]="comp.supplementPrice === 0 ? 'text-on-surface-variant' : 'text-secondary'">
                    {{ comp.supplementPrice === 0 ? 'Inclus' : '+ ' + (comp.supplementPrice | price) }}
                  </p>
                </div>
                @if (selectedSide()?.menuItemId === comp.menuItemId) {
                  <div class="w-6 h-6 rounded-full bg-primary flex items-center justify-center">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
                  </div>
                }
              </button>
            }
          </div>
        }

        <!-- Step 3: Drink selection -->
        @if (step() === 3) {
          <div class="space-y-3">
            @for (comp of drinks(); track comp.menuItemId) {
              <button class="w-full flex items-center gap-3 p-3 rounded-2xl transition-all active:scale-[0.98]"
                      [class]="selectedDrink()?.menuItemId === comp.menuItemId ? 'bg-primary/10 ring-2 ring-primary' : 'bg-surface-container hover:bg-surface-container-high'"
                      (click)="selectDrink(comp)">
                <div class="w-14 h-14 rounded-xl bg-surface-container flex-shrink-0 overflow-hidden">
                  @if (comp.menuItemImagePath) {
                    <img [src]="comp.menuItemImagePath" [alt]="comp.menuItemName" class="w-full h-full object-cover" />
                  } @else {
                    <div class="w-full h-full flex items-center justify-center text-xl">🥤</div>
                  }
                </div>
                <div class="flex-1 text-left">
                  <p class="font-display font-semibold text-on-surface text-sm">{{ comp.menuItemName }}</p>
                  <p class="font-display font-bold text-sm"
                     [class]="comp.supplementPrice === 0 ? 'text-on-surface-variant' : 'text-secondary'">
                    {{ comp.supplementPrice === 0 ? 'Inclus' : '+ ' + (comp.supplementPrice | price) }}
                  </p>
                </div>
                @if (selectedDrink()?.menuItemId === comp.menuItemId) {
                  <div class="w-6 h-6 rounded-full bg-primary flex items-center justify-center">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
                  </div>
                }
              </button>
            }
          </div>
        }

        <!-- Navigation -->
        <div class="flex gap-3 mt-6">
          @if (step() > 1) {
            <button class="flex-1 py-3 rounded-full border-2 border-outline text-on-surface font-display font-bold text-sm active:scale-[0.98] transition-transform"
                    (click)="step.set(step() - 1)">
              Retour
            </button>
          }
          <button class="flex-1 py-3 rounded-full font-display font-bold text-sm active:scale-[0.98] transition-transform disabled:opacity-40 disabled:pointer-events-none"
                  [class]="step() === 3 ? 'bg-primary text-on-primary' : 'bg-primary text-on-primary'"
                  [disabled]="!canProceed()"
                  (click)="nextStep()">
            {{ step() === 3 ? 'Ajouter au panier' : 'Suivant' }}
          </button>
        </div>
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
export class ComboStepperComponent {
  items = input.required<ItemView[]>();
  compositions = input.required<CompositionEntry[]>();
  cancel = output<void>();
  complete = output<void>();

  private readonly cart = inject(CartService);

  step = signal(1);
  selectedMain = signal<ItemView | null>(null);
  selectedSide = signal<CompositionEntry | null>(null);
  selectedDrink = signal<CompositionEntry | null>(null);

  comboVariants = computed(() =>
    this.items().filter(i => i.menuVariantOf !== null && i.available)
  );

  sides = computed(() =>
    this.compositions().filter(c => c.compositionType === 'accompagnement')
  );

  drinks = computed(() =>
    this.compositions().filter(c => c.compositionType === 'boisson')
  );

  canProceed = computed(() => {
    if (this.step() === 1) return this.selectedMain() !== null;
    if (this.step() === 2) return this.selectedSide() !== null;
    return this.selectedDrink() !== null;
  });

  selectMain(item: ItemView): void {
    this.selectedMain.set(item);
  }

  selectSide(comp: CompositionEntry): void {
    this.selectedSide.set(comp);
  }

  selectDrink(comp: CompositionEntry): void {
    this.selectedDrink.set(comp);
  }

  nextStep(): void {
    if (this.step() < 3) {
      this.step.set(this.step() + 1);
      return;
    }

    const main = this.selectedMain()!;
    const side = this.selectedSide()!;
    const drink = this.selectedDrink()!;

    this.cart.addCombo(
      { id: main.id, name: main.name, price: main.price, imagePath: main.imagePath },
      { id: side.menuItemId, name: side.menuItemName, supplementPrice: side.supplementPrice, imagePath: side.menuItemImagePath },
      { id: drink.menuItemId, name: drink.menuItemName, supplementPrice: drink.supplementPrice, imagePath: drink.menuItemImagePath },
    );

    this.complete.emit();
  }
}
