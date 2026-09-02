import { TestBed } from '@angular/core/testing';
import { CartBarComponent } from './cart-bar.component';
import { CartService } from '../../../features/cart/services/cart.service';

describe('CartBarComponent', () => {
  let cartService: CartService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CartBarComponent],
    }).compileComponents();

    cartService = TestBed.inject(CartService);
    cartService.clear();
  });

  it('stays visible even when the cart is empty (badge at zero)', () => {
    const fixture = TestBed.createComponent(CartBarComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Voir panier');
    expect(fixture.nativeElement.textContent).toContain('0,00 €');
    expect(fixture.nativeElement.querySelector('.cart-badge').textContent.trim()).toBe('0');
  });

  it('shows the item count and total when the cart has entries', () => {
    cartService.addStandalone('item-1', 'Burger classique', 12.5, null);

    const fixture = TestBed.createComponent(CartBarComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Voir panier');
    expect(fixture.nativeElement.textContent).toContain('12,50 €');
    expect(fixture.nativeElement.querySelector('.cart-badge').textContent.trim()).toBe('1');
  });
});
