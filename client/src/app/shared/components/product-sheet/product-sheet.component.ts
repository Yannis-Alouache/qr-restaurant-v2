import { Component, effect, inject, input, output, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { PricePipe } from '../../pipes/price.pipe';

export interface ProductSheetData {
  name: string;
  description: string | null;
  price: number;
  /** Libellé de prix alternatif (ex. « Inclus », « + 1,50 € ») ; sinon prix formaté. */
  priceLabel: string | null;
  imagePath: string | null;
}

/** Fiche produit en bottom-sheet — copie pixel de la maquette, fermeture au swipe. */
@Component({
  selector: 'app-product-sheet',
  templateUrl: './product-sheet.component.html',
  styleUrl: './product-sheet.component.scss',
  imports: [PricePipe],
})
export class ProductSheetComponent {
  private readonly document = inject(DOCUMENT);

  product = input<ProductSheetData | null>(null);
  /** Libellé du bouton d'ajout (« Ajouter au panier » / « Ajouter au menu »). */
  addButtonLabel = input('Ajouter au panier');

  readonly closed = output<void>();
  readonly add = output<void>();

  readonly dragOffset = signal<number | null>(null);

  private startY = 0;
  private dragging = false;

  constructor() {
    effect(() => {
      this.document.body.style.overflow = this.product() ? 'hidden' : '';
    });
  }

  onTouchStart(event: TouchEvent): void {
    this.startY = event.touches[0].clientY;
    this.dragging = true;
  }

  onTouchMove(event: TouchEvent): void {
    if (!this.dragging) return;
    const dy = event.touches[0].clientY - this.startY;
    this.dragOffset.set(dy < 0 ? 0 : dy);
  }

  onTouchEnd(): void {
    if (!this.dragging) return;
    this.dragging = false;
    const dy = this.dragOffset() ?? 0;
    this.dragOffset.set(null);
    if (dy > 80) {
      this.closed.emit();
    }
  }
}
