import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { OrderService } from '../../services/order.service';
import { OrderStatusRealtimeService } from '../../services/order-status-realtime.service';
import { ConfirmationPageComponent } from './confirmation-page.component';

describe('ConfirmationPageComponent', () => {
  it('shows a waiting-payment state while the webhook has not confirmed the order yet', () => {
    const orderStatusRealtimeService = {
      connect: vi.fn(),
      disconnect: vi.fn(),
    };
    const orderService = {
      getOrder: vi.fn().mockReturnValue(
        of({
          id: 'order-1',
          status: 'en_attente_paiement',
          total: 12,
          createdAt: '2026-05-24T10:00:00Z',
          items: [
            {
              id: 'item-1',
              menuItemId: 'menu-item-1',
              name: 'Burger',
              quantity: 1,
              unitPrice: 12,
              menuGroupId: null,
              menuRole: null,
            },
          ],
        }),
      ),
    };

    TestBed.configureTestingModule({
      imports: [ConfirmationPageComponent],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: OrderStatusRealtimeService, useValue: orderStatusRealtimeService },
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

    const fixture = TestBed.createComponent(ConfirmationPageComponent);

    fixture.detectChanges();

    expect(orderService.getOrder).toHaveBeenCalledWith('order-1');
    expect(orderStatusRealtimeService.connect).toHaveBeenCalledWith('order-1', expect.any(Function));
    expect(fixture.nativeElement.textContent).toContain('Paiement en cours de confirmation');
    expect(fixture.nativeElement.textContent).not.toContain('Commande confirmée !');
  });

  it('updates the confirmation copy when realtime marks the order as paid', () => {
    let realtimeCallback: ((status: string) => void) | undefined;
    const orderStatusRealtimeService = {
      connect: vi.fn((_orderId: string, onStatusChange: (status: string) => void) => {
        realtimeCallback = onStatusChange;
      }),
      disconnect: vi.fn(),
    };
    const orderService = {
      getOrder: vi.fn().mockReturnValue(
        of({
          id: 'order-1',
          status: 'en_attente_paiement',
          total: 12,
          createdAt: '2026-05-24T10:00:00Z',
          items: [
            {
              id: 'item-1',
              menuItemId: 'menu-item-1',
              name: 'Burger',
              quantity: 1,
              unitPrice: 12,
              menuGroupId: null,
              menuRole: null,
            },
          ],
        }),
      ),
    };

    TestBed.configureTestingModule({
      imports: [ConfirmationPageComponent],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: OrderStatusRealtimeService, useValue: orderStatusRealtimeService },
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

    const fixture = TestBed.createComponent(ConfirmationPageComponent);

    fixture.detectChanges();

    realtimeCallback?.('nouvelle');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Commande confirmée !');
    expect(fixture.nativeElement.textContent).toContain('Votre commande a été enregistrée avec succès');
    expect(fixture.nativeElement.textContent).not.toContain('Paiement en cours de confirmation');
  });
});
