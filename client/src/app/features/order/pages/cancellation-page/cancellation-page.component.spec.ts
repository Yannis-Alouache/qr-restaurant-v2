import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { throwError } from 'rxjs';
import { OrderService } from '../../services/order.service';
import { CancellationPageComponent } from './cancellation-page.component';

describe('CancellationPageComponent', () => {
  it('keeps the order retryable after a cancelled payment', () => {
    const orderService = {
      createCheckoutSession: vi.fn().mockReturnValue(
        throwError(() => ({
          error: { message: 'Le paiement en ligne est temporairement indisponible.' },
        })),
      ),
    };

    TestBed.configureTestingModule({
      imports: [CancellationPageComponent],
      providers: [
        { provide: OrderService, useValue: orderService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({
                orderId: 'order-1',
              }),
            },
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(CancellationPageComponent);

    fixture.detectChanges();
    fixture.componentInstance.retryPayment();

    expect(fixture.nativeElement.textContent).toContain('Votre commande est conservée');
    expect(orderService.createCheckoutSession).toHaveBeenCalledWith('order-1');
    expect(fixture.componentInstance.error()).toBe(
      'Le paiement en ligne est temporairement indisponible.',
    );
  });
});
