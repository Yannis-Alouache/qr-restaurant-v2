import { TestBed } from '@angular/core/testing';
import { FloatingCartBarComponent } from './floating-cart-bar.component';
import { CartService } from '../../services/cart.service';

describe('FloatingCartBarComponent', () => {
  let cartService: CartService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FloatingCartBarComponent],
    }).compileComponents();

    cartService = TestBed.inject(CartService);
  });

  it('stays hidden while the cart is empty', () => {
    const fixture = TestBed.createComponent(FloatingCartBarComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Voir le panier');
  });

  it('shows the item count and total when the cart has entries', () => {
    cartService.addStandalone('item-1', 'Burger classique', 12.5, null);

    const fixture = TestBed.createComponent(FloatingCartBarComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Voir le panier');
    expect(fixture.nativeElement.textContent).toContain('12.50 €');
    expect(fixture.nativeElement.textContent).toContain('1');
  });
});
