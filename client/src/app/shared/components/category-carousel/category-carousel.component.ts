import {
  Component,
  ElementRef,
  afterNextRender,
  computed,
  effect,
  input,
  output,
  viewChild,
} from '@angular/core';

export interface CategoryCarouselEntry {
  id: string;
  name: string;
  imagePath: string | null;
}

/**
 * Carrousel horizontal infini de catégories — reproduction du comportement
 * de la maquette : triple clonage de la liste, replacement sans à-coup quand
 * on déborde dans un clone, flèches de défilement, item actif centré.
 */
@Component({
  selector: 'app-category-carousel',
  templateUrl: './category-carousel.component.html',
  styleUrl: './category-carousel.component.scss',
})
export class CategoryCarouselComponent {
  categories = input.required<CategoryCarouselEntry[]>();
  /** Id de la catégorie active (null = aucune mise en avant). */
  selectedId = input<string | null>(null);
  readonly select = output<string>();

  private readonly track = viewChild.required<ElementRef<HTMLElement>>('track');

  /** Trois copies de la liste : [clone][réels][clone]. */
  readonly loopItems = computed(() => {
    const cats = this.categories();
    const clones = cats.map((c, i) => ({ ...c, loopIndex: i }));
    const real = cats.map((c, i) => ({ ...c, loopIndex: cats.length + i }));
    const clonesAfter = cats.map((c, i) => ({ ...c, loopIndex: cats.length * 2 + i }));
    return [...clones, ...real, ...clonesAfter];
  });

  readonly palette = [
    'var(--cat-burgers)',
    'var(--cat-sandwichs)',
    'var(--cat-salades)',
    'var(--cat-pizzas)',
    'var(--cat-desserts)',
    'var(--cat-boissons)',
    'var(--cat-accompagnements)',
  ];

  private singleSetWidth = 0;
  private scrollTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    afterNextRender(() => {
      setTimeout(() => this.initializeScroll(), 50);
    });

    effect(() => {
      this.selectedId();
      // Re-centrer l'item actif (après rendu) quand la sélection change.
      setTimeout(() => this.scrollActiveIntoView(), 30);
    });
  }

  /** Couleur de repli stable pour une catégorie (indépendante du clonage). */
  colorFor(loopIndex: number): string {
    const categoryIndex = loopIndex % Math.max(1, this.categories().length);
    return this.palette[categoryIndex % this.palette.length];
  }

  isActive(item: { id: string }): boolean {
    return this.selectedId() === item.id;
  }

  onTrackScroll(): void {
    if (this.scrollTimer) clearTimeout(this.scrollTimer);
    this.scrollTimer = setTimeout(() => this.wrapScroll(), 60);
  }

  scrollBy(direction: -1 | 1): void {
    const el = this.track().nativeElement;
    const items = el.querySelectorAll<HTMLElement>('.cat-carousel-item');
    const itemWidth = items.length > 0 ? items[0].offsetWidth + 8 : 70;
    // Avancer d'une seule catégorie par clic de flèche.
    el.scrollLeft += itemWidth * direction;
  }

  private itemCount(): number {
    return this.categories().length;
  }

  private initializeScroll(): void {
    this.singleSetWidth = this.measureSetWidth();
    // Démarrer au début de la liste réelle avant de centrer l'item actif.
    this.jumpTo(this.singleSetWidth + 8);
    this.scrollActiveIntoView();
    setTimeout(() => {
      const el = this.track().nativeElement;
      const sl = el.scrollLeft;
      if (sl < this.singleSetWidth * 0.5 || sl > this.singleSetWidth * 1.8) {
        this.jumpTo(el.scrollLeft);
      }
    }, 100);
  }

  private measureSetWidth(): number {
    const el = this.track().nativeElement;
    const items = el.querySelectorAll<HTMLElement>('.cat-carousel-item');
    const count = this.itemCount();
    let width = 0;
    for (let i = count; i < count * 2 && i < items.length; i++) {
      width += items[i].offsetWidth + 8;
    }
    return Math.max(0, width - 8);
  }

  private scrollActiveIntoView(): void {
    const el = this.track().nativeElement;
    const activeItems = el.querySelectorAll<HTMLElement>('.cat-carousel-item.active');
    if (activeItems.length === 0) return;

    // Chaque catégorie existe en 3 exemplaires (clones) : centrer celui qui
    // est le plus proche du viewport actuel, sinon la sélection fait sauter
    // le carrousel d'un bout à l'autre et casse l'illusion de boucle infinie.
    const trackRect = el.getBoundingClientRect();
    const trackCenter = trackRect.left + trackRect.width / 2;
    let bestEl: HTMLElement | null = null;
    let bestDistance = Infinity;
    for (const item of Array.from(activeItems)) {
      const rect = item.getBoundingClientRect();
      const distance = Math.abs(rect.left + rect.width / 2 - trackCenter);
      if (distance < bestDistance) {
        bestDistance = distance;
        bestEl = item;
      }
    }
    if (!bestEl) return;

    const elRect = bestEl.getBoundingClientRect();
    const offset = elRect.left - trackRect.left - trackRect.width / 2 + elRect.width / 2;
    el.scrollLeft += offset;
  }

  /** Replacement sans transition au cœur de la liste réelle. */
  private jumpTo(scrollLeft: number): void {
    const el = this.track().nativeElement;
    el.style.scrollBehavior = 'auto';
    el.scrollLeft = scrollLeft;
    void el.scrollLeft;
    el.style.scrollBehavior = '';
  }

  private wrapScroll(): void {
    if (this.singleSetWidth === 0) this.singleSetWidth = this.measureSetWidth();
    const el = this.track().nativeElement;
    const sl = el.scrollLeft;

    if (sl >= this.singleSetWidth * 2 + 16) {
      this.jumpTo(sl - this.singleSetWidth - 8);
    }
    if (sl <= this.singleSetWidth * 0.3) {
      this.jumpTo(sl + this.singleSetWidth + 8);
    }
  }
}
