import { TestBed } from '@angular/core/testing';
import { CartService } from '../../../cart/services/cart.service';
import { ComboStepperComponent } from './combo-stepper.component';

describe('ComboStepperComponent', () => {
  it('shows only the menu variants that belong to the selected base item', () => {
    TestBed.configureTestingModule({
      imports: [ComboStepperComponent],
      providers: [
        {
          provide: CartService,
          useValue: {
            addCombo: vi.fn(),
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(ComboStepperComponent);
    fixture.componentRef.setInput('baseItemId', 'burger-1');
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
        description: 'Formule burger',
        price: 10.4,
        imagePath: null,
        available: true,
        menuVariantOf: 'burger-1',
      },
      {
        id: 'menu-chicken-1',
        name: 'Menu Chicken',
        description: 'Formule chicken',
        price: 11.2,
        imagePath: null,
        available: true,
        menuVariantOf: 'chicken-1',
      },
    ]);
    fixture.componentRef.setInput('compositions', []);
    fixture.detectChanges();

    const textContent = fixture.nativeElement.textContent;

    expect(textContent).toContain('Menu Burger classique');
    expect(textContent).not.toContain('Menu Chicken');
  });
});
