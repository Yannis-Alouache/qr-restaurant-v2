import { TestBed } from '@angular/core/testing';
import { ItemListComponent } from './item-list.component';

describe('ItemListComponent', () => {
  it('shows only standalone available items in the public list', () => {
    TestBed.configureTestingModule({
      imports: [ItemListComponent],
    });

    const fixture = TestBed.createComponent(ItemListComponent);
    fixture.componentRef.setInput('items', [
      {
        id: 'burger-1',
        name: 'Burger classique',
        description: null,
        price: 6.9,
        imagePath: null,
        available: true,
        menuVariantOf: null,
      },
      {
        id: 'menu-burger-1',
        name: 'Menu Burger classique',
        description: null,
        price: 10.4,
        imagePath: null,
        available: true,
        menuVariantOf: 'burger-1',
      },
      {
        id: 'drink-1',
        name: 'Coca-Cola',
        description: null,
        price: 2.5,
        imagePath: null,
        available: false,
        menuVariantOf: null,
      },
    ]);
    fixture.detectChanges();

    const textContent = fixture.nativeElement.textContent;

    expect(textContent).toContain('Burger classique');
    expect(textContent).toContain('Menu dispo');
    expect(textContent).not.toContain('Menu Burger classique');
    expect(textContent).not.toContain('Coca-Cola');
  });
});
