import { TestBed } from '@angular/core/testing';
import { CartService } from '../../../cart/services/cart.service';
import { ItemModalComponent } from './item-modal.component';

describe('ItemModalComponent', () => {
  it('shows menu composition actions only for items that can actually be composed as a menu', () => {
    TestBed.configureTestingModule({
      imports: [ItemModalComponent],
      providers: [
        {
          provide: CartService,
          useValue: {
            addStandalone: vi.fn(),
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(ItemModalComponent);
    fixture.componentRef.setInput('item', {
      id: 'burger-1',
      name: 'Burger classique',
      description: null,
      price: 6.9,
      imagePath: null,
      available: true,
      menuVariantOf: null,
    });
    fixture.componentRef.setInput('canComposeAsMenu', true);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Composer en menu');

    fixture.componentRef.setInput('canComposeAsMenu', false);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Composer en menu');
  });
});
