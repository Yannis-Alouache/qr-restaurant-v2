import { Component, input, output, signal, computed, inject } from '@angular/core';
import { ItemView, CompositionEntry } from '../../models/menu.model';
import { CartService } from '../../../cart/services/cart.service';
import { PricePipe } from '../../../../shared/pipes/price.pipe';

@Component({
  selector: 'app-combo-stepper',
  standalone: true,
  imports: [PricePipe],
  template: `
    <div class="sheet-backdrop" (click)="cancel.emit()">
      <div class="sheet-backdrop__scrim"></div>
      <div
        class="combo-sheet animate-slide-up safe-bottom-pad"
        role="dialog"
        aria-modal="true"
        aria-labelledby="combo-stepper-title"
        (click)="$event.stopPropagation()">
        <div class="combo-sheet__handle"></div>
        <button class="combo-sheet__close" type="button" aria-label="Fermer la composition du menu" (click)="cancel.emit()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>

        <div class="combo-sheet__content">
          <p class="combo-sheet__eyebrow">Composer un menu</p>
          <div class="combo-sheet__header">
            <div>
              <h2 id="combo-stepper-title" class="combo-sheet__title">{{ stepTitle() }}</h2>
              <p class="combo-sheet__description">{{ stepDescription() }}</p>
            </div>
          </div>

          <div class="combo-sheet__progress">
            @for (s of [1, 2, 3]; track s) {
              <div class="combo-step" [class.combo-step--active]="s <= step()">
                <span class="combo-step__number">{{ s }}</span>
                <span class="combo-step__label">{{ stepLabel(s) }}</span>
              </div>
            }
          </div>

          @if (step() === 1) {
            <div class="combo-sheet__options">
              @for (item of comboVariants(); track item.id) {
                <button
                  class="combo-option"
                  [class.combo-option--selected]="selectedMain()?.id === item.id"
                  (click)="selectMain(item)">
                  <div class="combo-option__media">
                    @if (item.imagePath) {
                      <img [src]="item.imagePath" [alt]="item.name" class="combo-option__image" />
                    } @else {
                      <div class="combo-option__placeholder">{{ item.name.trim().charAt(0).toUpperCase() }}</div>
                    }
                  </div>
                  <div class="combo-option__body">
                    <p class="combo-option__name">{{ item.name }}</p>
                    <p class="combo-option__meta">{{ item.price | price }}</p>
                  </div>
                  <div class="combo-option__check" [class.combo-option__check--visible]="selectedMain()?.id === item.id">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
                  </div>
                </button>
              }
            </div>
          }

          @if (step() === 2) {
            <div class="combo-sheet__options">
              @for (comp of sides(); track comp.menuItemId) {
                <button
                  class="combo-option"
                  [class.combo-option--selected]="selectedSide()?.menuItemId === comp.menuItemId"
                  (click)="selectSide(comp)">
                  <div class="combo-option__media">
                    @if (comp.menuItemImagePath) {
                      <img [src]="comp.menuItemImagePath" [alt]="comp.menuItemName" class="combo-option__image" />
                    } @else {
                      <div class="combo-option__placeholder">🍟</div>
                    }
                  </div>
                  <div class="combo-option__body">
                    <p class="combo-option__name">{{ comp.menuItemName }}</p>
                    <p class="combo-option__meta"
                       [class.combo-option__meta--included]="comp.supplementPrice === 0">
                      {{ comp.supplementPrice === 0 ? 'Inclus' : '+ ' + (comp.supplementPrice | price) }}
                    </p>
                  </div>
                  <div class="combo-option__check" [class.combo-option__check--visible]="selectedSide()?.menuItemId === comp.menuItemId">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
                  </div>
                </button>
              }
            </div>
          }

          @if (step() === 3) {
            <div class="combo-sheet__options">
              @for (comp of drinks(); track comp.menuItemId) {
                <button
                  class="combo-option"
                  [class.combo-option--selected]="selectedDrink()?.menuItemId === comp.menuItemId"
                  (click)="selectDrink(comp)">
                  <div class="combo-option__media">
                    @if (comp.menuItemImagePath) {
                      <img [src]="comp.menuItemImagePath" [alt]="comp.menuItemName" class="combo-option__image" />
                    } @else {
                      <div class="combo-option__placeholder">🥤</div>
                    }
                  </div>
                  <div class="combo-option__body">
                    <p class="combo-option__name">{{ comp.menuItemName }}</p>
                    <p class="combo-option__meta"
                       [class.combo-option__meta--included]="comp.supplementPrice === 0">
                      {{ comp.supplementPrice === 0 ? 'Inclus' : '+ ' + (comp.supplementPrice | price) }}
                    </p>
                  </div>
                  <div class="combo-option__check" [class.combo-option__check--visible]="selectedDrink()?.menuItemId === comp.menuItemId">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
                  </div>
                </button>
              }
            </div>
          }

          <div class="combo-sheet__actions">
            @if (step() > 1) {
              <button class="combo-sheet__button combo-sheet__button--secondary" (click)="step.set(step() - 1)">
                Retour
              </button>
            }
            <button
              class="combo-sheet__button combo-sheet__button--primary"
              [disabled]="!canProceed()"
              (click)="nextStep()">
              {{ step() === 3 ? 'Ajouter au panier' : 'Suivant' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .sheet-backdrop {
      position: fixed;
      inset: 0;
      z-index: 50;
      display: flex;
      align-items: flex-end;
      justify-content: center;
    }

    .sheet-backdrop__scrim {
      position: absolute;
      inset: 0;
      background: rgba(14, 10, 8, 0.52);
      backdrop-filter: blur(16px);
    }

    .combo-sheet {
      position: relative;
      max-height: 92vh;
      width: min(100%, var(--app-max-width));
      overflow: hidden;
      border-radius: 2.3rem 2.3rem 0 0;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 70%, white 30%);
      background:
        linear-gradient(
          180deg,
          color-mix(in srgb, var(--color-surface-container-lowest) 92%, white 8%) 0%,
          color-mix(in srgb, var(--color-surface-container-low) 96%, var(--color-primary) 4%) 100%
        );
      box-shadow: var(--shadow-strong);
    }

    .combo-sheet__handle {
      width: 3.4rem;
      height: 0.34rem;
      margin: 0.8rem auto 0;
      border-radius: 9999px;
      background: color-mix(in srgb, var(--color-outline) 52%, white 48%);
    }

    .combo-sheet__close {
      position: absolute;
      top: 1rem;
      right: 1rem;
      z-index: 2;
      display: inline-grid;
      width: 2.35rem;
      height: 2.35rem;
      place-items: center;
      border-radius: 9999px;
      background: color-mix(in srgb, var(--color-surface-container-lowest) 70%, white 30%);
      color: var(--color-on-surface-variant);
      box-shadow: 0 14px 20px rgba(0, 0, 0, 0.1);
    }

    .combo-sheet__content {
      overflow-y: auto;
      padding: 1rem 1rem 1rem;
    }

    .combo-sheet__eyebrow {
      color: var(--color-on-surface-variant);
      font-size: 0.78rem;
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .combo-sheet__header {
      margin-top: 0.3rem;
    }

    .combo-sheet__title {
      font-family: var(--font-display);
      font-size: 1.55rem;
      font-weight: 800;
      line-height: 1.08;
      color: var(--color-on-surface);
      text-wrap: balance;
    }

    .combo-sheet__description {
      margin-top: 0.45rem;
      color: var(--color-on-surface-variant);
      font-size: 0.92rem;
      line-height: 1.5;
    }

    .combo-sheet__progress {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 0.55rem;
      margin-top: 1rem;
    }

    .combo-step {
      display: flex;
      min-height: 4rem;
      flex-direction: column;
      justify-content: center;
      gap: 0.2rem;
      border-radius: 1.35rem;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 72%, white 28%);
      background: color-mix(in srgb, var(--color-surface-container-lowest) 84%, white 16%);
      padding: 0.75rem;
      transition: background 160ms ease, border-color 160ms ease, transform 160ms ease;
    }

    .combo-step--active {
      background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface-container-high) 88%);
      border-color: color-mix(in srgb, var(--color-primary) 34%, white 66%);
    }

    .combo-step__number {
      display: inline-grid;
      width: 1.55rem;
      height: 1.55rem;
      place-items: center;
      border-radius: 9999px;
      background: color-mix(in srgb, var(--color-primary) 18%, white 82%);
      color: var(--color-primary);
      font-family: var(--font-display);
      font-size: 0.76rem;
      font-weight: 800;
    }

    .combo-step__label {
      color: var(--color-on-surface);
      font-size: 0.78rem;
      font-weight: 700;
      line-height: 1.3;
    }

    .combo-sheet__options {
      display: grid;
      gap: 0.75rem;
      margin-top: 1rem;
    }

    .combo-option {
      display: flex;
      align-items: center;
      gap: 0.85rem;
      width: 100%;
      border-radius: 1.6rem;
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 72%, white 28%);
      background:
        linear-gradient(
          180deg,
          color-mix(in srgb, var(--color-surface-container-lowest) 88%, white 12%) 0%,
          color-mix(in srgb, var(--color-surface-container-low) 96%, var(--color-primary) 4%) 100%
        );
      padding: 0.75rem;
      text-align: left;
      box-shadow: 0 14px 26px rgba(0, 0, 0, 0.06);
      transition:
        transform 160ms ease,
        box-shadow 160ms ease,
        border-color 160ms ease,
        background 160ms ease;
    }

    .combo-option:active {
      transform: scale(0.985);
    }

    .combo-option--selected {
      border-color: color-mix(in srgb, var(--color-primary) 44%, white 56%);
      background:
        linear-gradient(
          180deg,
          color-mix(in srgb, var(--color-primary-container) 48%, white 52%) 0%,
          color-mix(in srgb, var(--color-surface-container-lowest) 72%, var(--color-primary) 28%) 100%
        );
      box-shadow: 0 20px 34px color-mix(in srgb, var(--color-primary) 18%, transparent);
    }

    .combo-option__media {
      overflow: hidden;
      width: 4.4rem;
      height: 4.4rem;
      flex-shrink: 0;
      border-radius: 1.2rem;
      background:
        radial-gradient(circle at top, color-mix(in srgb, var(--color-secondary-container) 42%, transparent), transparent 58%),
        color-mix(in srgb, var(--color-primary) 10%, var(--color-surface-container-high) 90%);
    }

    .combo-option__image,
    .combo-option__placeholder {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .combo-option__placeholder {
      display: grid;
      place-items: center;
      font-family: var(--font-display);
      font-size: 1.6rem;
      font-weight: 800;
      color: color-mix(in srgb, var(--color-on-primary) 84%, white 16%);
      background:
        radial-gradient(circle at top, color-mix(in srgb, var(--color-secondary-container) 70%, white 30%), transparent 58%),
        linear-gradient(
          135deg,
          color-mix(in srgb, var(--color-primary) 78%, black 22%),
          color-mix(in srgb, var(--color-secondary) 84%, var(--color-primary) 16%)
        );
    }

    .combo-option__body {
      flex: 1;
      min-width: 0;
    }

    .combo-option__name {
      font-family: var(--font-display);
      font-size: 0.98rem;
      font-weight: 700;
      line-height: 1.25;
      color: var(--color-on-surface);
      text-wrap: balance;
    }

    .combo-option__meta {
      margin-top: 0.35rem;
      color: var(--color-primary);
      font-size: 0.86rem;
      font-weight: 800;
    }

    .combo-option__meta--included {
      color: var(--color-on-surface-variant);
    }

    .combo-option__check {
      display: inline-grid;
      width: 2rem;
      height: 2rem;
      place-items: center;
      flex-shrink: 0;
      border-radius: 9999px;
      background: color-mix(in srgb, var(--color-primary) 14%, var(--color-surface-container-high) 86%);
      color: transparent;
      transition: background 160ms ease, color 160ms ease, transform 160ms ease;
    }

    .combo-option__check--visible {
      background: color-mix(in srgb, var(--color-primary) 84%, white 16%);
      color: var(--color-on-primary);
      transform: scale(1.02);
      box-shadow: 0 14px 24px color-mix(in srgb, var(--color-primary) 20%, transparent);
    }

    .combo-sheet__actions {
      display: flex;
      gap: 0.75rem;
      margin-top: 1rem;
    }

    .combo-sheet__button {
      flex: 1;
      min-height: 3.4rem;
      border-radius: 9999px;
      padding: 0 1rem;
      font-family: var(--font-display);
      font-size: 0.98rem;
      font-weight: 800;
      transition: transform 160ms ease, box-shadow 160ms ease, opacity 160ms ease;
    }

    .combo-sheet__button:active {
      transform: scale(0.985);
    }

    .combo-sheet__button:disabled {
      opacity: 0.42;
      pointer-events: none;
    }

    .combo-sheet__button--primary {
      background:
        linear-gradient(
          135deg,
          color-mix(in srgb, var(--color-primary) 84%, white 16%),
          color-mix(in srgb, var(--color-secondary) 84%, var(--color-primary) 16%)
        );
      color: var(--color-on-primary);
      box-shadow: 0 22px 34px color-mix(in srgb, var(--color-primary) 24%, transparent);
    }

    .combo-sheet__button--secondary {
      background: color-mix(in srgb, var(--color-surface-container-lowest) 84%, white 16%);
      color: var(--color-on-surface);
      border: 1px solid color-mix(in srgb, var(--color-outline-variant) 78%, white 22%);
    }

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
  baseItemId = input.required<string>();
  cancel = output<void>();
  complete = output<void>();

  private readonly cart = inject(CartService);

  step = signal(1);
  selectedMain = signal<ItemView | null>(null);
  selectedSide = signal<CompositionEntry | null>(null);
  selectedDrink = signal<CompositionEntry | null>(null);

  comboVariants = computed(() =>
    this.items().filter(i => i.menuVariantOf === this.baseItemId() && i.available)
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

  stepTitle(): string {
    if (this.step() === 1) return 'Choisissez votre plat';
    if (this.step() === 2) return 'Choisissez votre accompagnement';
    return 'Choisissez votre boisson';
  }

  stepDescription(): string {
    if (this.step() === 1) return 'Sélectionnez la base de votre formule.';
    if (this.step() === 2) return 'Ajoutez l’accompagnement qui complète le menu.';
    return 'Terminez avec la boisson de votre choix.';
  }

  stepLabel(step: number): string {
    if (step === 1) return 'Plat';
    if (step === 2) return 'Accompagnement';
    return 'Boisson';
  }

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
