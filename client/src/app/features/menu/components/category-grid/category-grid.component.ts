import { Component, input, output } from '@angular/core';
import { CategoryView } from '../../models/menu.model';

@Component({
  selector: 'app-category-grid',
  standalone: true,
  template: `
    <div class="flex gap-3 py-3 overflow-x-auto scrollbar-hide">
      @for (category of categories(); track category.id) {
        <button
          (click)="selectCategory.emit(category.id)"
          class="flex-shrink-0 px-4 py-2 rounded-full font-display text-sm font-semibold transition-all whitespace-nowrap"
          [class]="isSelected(category.id)
            ? 'bg-primary text-on-primary shadow-md'
            : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'">
          {{ category.name }}
        </button>
      }
    </div>
  `,
  styles: [`
    .scrollbar-hide::-webkit-scrollbar { display: none; }
    .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
  `],
})
export class CategoryGridComponent {
  categories = input.required<CategoryView[]>();
  selectedId = input<string | null>(null);
  selectCategory = output<string>();

  isSelected(categoryId: string): boolean {
    return this.selectedId() === categoryId;
  }
}
