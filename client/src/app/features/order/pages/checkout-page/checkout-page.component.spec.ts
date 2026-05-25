import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CartEntry } from '../../../cart/models/cart.model';
import { CartService } from '../../../cart/services/cart.service';
import { OrderService } from '../../services/order.service';
import { CheckoutPageComponent } from './checkout-page.component';

describe('CheckoutPageComponent', () => {
  it('keeps the cart intact and retries payment on the same order when checkout is refused', () => {
    const clear = vi.fn();
    const cartEntries: CartEntry[] = [
      {
        type: 'standalone',
        cartId: 'cart-1',
        itemId: 'item-1',
        name: 'Burger',
        price: 12,
        imagePath: null,
        quantity: 1,
      },
    ];
    const orderService = {
      createOrder: vi.fn().mockReturnValue(
        of({
          id: 'order-1',
          status: 'en_attente_paiement',
          total: 12,
          createdAt: '2026-05-24T10:00:00Z',
        }),
      ),
      createCheckoutSession: vi.fn().mockReturnValue(
        throwError(() => ({
          error: { message: "Ce restaurant n'a pas configuré les paiements en ligne" },
        })),
      ),
    };

    TestBed.configureTestingModule({
      imports: [CheckoutPageComponent],
      providers: [
        { provide: OrderService, useValue: orderService },
        {
          provide: CartService,
          useValue: {
            isEmpty: () => false,
            cartEntries: () => cartEntries,
            clear,
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({
                slug: 'naia-burger',
                tableId: 'table-1',
              }),
            },
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(CheckoutPageComponent);

    fixture.detectChanges();
    fixture.componentInstance.retryPayment();

    expect(orderService.createOrder).toHaveBeenCalledWith('naia-burger', 'table-1', cartEntries);
    expect(orderService.createOrder).toHaveBeenCalledTimes(1);
    expect(orderService.createCheckoutSession).toHaveBeenCalledTimes(2);
    expect(orderService.createCheckoutSession).toHaveBeenNthCalledWith(1, 'order-1');
    expect(orderService.createCheckoutSession).toHaveBeenNthCalledWith(2, 'order-1');
    expect(clear).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toBe(
      "Ce restaurant n'a pas configuré les paiements en ligne",
    );
  });
});
