import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { MenuService } from '../../../menu/services/menu.service';
import { OrderService } from '../../services/order.service';
import { OrderStatusRealtimeService } from '../../services/order-status-realtime.service';
import { ConfirmationPageComponent } from './confirmation-page.component';

const REVIEW_CONTEXT_KEY = 'menzo:review-prompt';

function orderFixture(status: string) {
  return {
    id: 'order-1',
    status,
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
  };
}

describe('ConfirmationPageComponent', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('shows a waiting-payment state while the webhook has not confirmed the order yet', () => {
    const orderStatusRealtimeService = {
      connect: vi.fn(),
      disconnect: vi.fn(),
    };
    const orderService = {
      getOrder: vi.fn().mockReturnValue(of(orderFixture('en_attente_paiement'))),
    };
    const menuService = { getMenu: vi.fn() };

    TestBed.configureTestingModule({
      imports: [ConfirmationPageComponent],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: OrderStatusRealtimeService, useValue: orderStatusRealtimeService },
        { provide: MenuService, useValue: menuService },
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
    expect(orderStatusRealtimeService.connect).toHaveBeenCalledWith('order-1', expect.any(Function), expect.any(Function));
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
      getOrder: vi.fn().mockReturnValue(of(orderFixture('en_attente_paiement'))),
    };
    const menuService = { getMenu: vi.fn() };

    TestBed.configureTestingModule({
      imports: [ConfirmationPageComponent],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: OrderStatusRealtimeService, useValue: orderStatusRealtimeService },
        { provide: MenuService, useValue: menuService },
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

  it('shows the Google review card once the order is served and the restaurant configured a review link', () => {
    sessionStorage.setItem(
      REVIEW_CONTEXT_KEY,
      JSON.stringify({ slug: 'naia-burger', tableId: 'table-1', orderId: 'order-1', status: 'nouvelle' }),
    );
    const orderStatusRealtimeService = { connect: vi.fn(), disconnect: vi.fn() };
    const orderService = {
      getOrder: vi.fn().mockReturnValue(of(orderFixture('servie'))),
    };
    const menuService = {
      getMenu: vi.fn().mockReturnValue(
        of({ restaurant: { googleReviewUrl: 'https://g.page/r/naia-burger/review' } }),
      ),
    };

    TestBed.configureTestingModule({
      imports: [ConfirmationPageComponent],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: OrderStatusRealtimeService, useValue: orderStatusRealtimeService },
        { provide: MenuService, useValue: menuService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ orderId: 'order-1' }) } },
        },
      ],
    });

    const fixture = TestBed.createComponent(ConfirmationPageComponent);
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('[data-testid="google-review-card"]');
    expect(card).not.toBeNull();
    const link = card.querySelector('a.review-button') as HTMLAnchorElement;
    expect(link.href).toBe('https://g.page/r/naia-burger/review');
    // Le contexte mémorisé au paiement passe à « servie » : le menu saura
    // que la table a été servie même si le client ferme cette page.
    expect(JSON.parse(sessionStorage.getItem(REVIEW_CONTEXT_KEY)!).status).toBe('servie');
  });

  it('does not show the review card before the order is served', () => {
    sessionStorage.setItem(
      REVIEW_CONTEXT_KEY,
      JSON.stringify({ slug: 'naia-burger', tableId: 'table-1', orderId: 'order-1', status: 'nouvelle' }),
    );
    const orderStatusRealtimeService = { connect: vi.fn(), disconnect: vi.fn() };
    const orderService = {
      getOrder: vi.fn().mockReturnValue(of(orderFixture('prete'))),
    };
    const menuService = {
      getMenu: vi.fn().mockReturnValue(
        of({ restaurant: { googleReviewUrl: 'https://g.page/r/naia-burger/review' } }),
      ),
    };

    TestBed.configureTestingModule({
      imports: [ConfirmationPageComponent],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: OrderStatusRealtimeService, useValue: orderStatusRealtimeService },
        { provide: MenuService, useValue: menuService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ orderId: 'order-1' }) } },
        },
      ],
    });

    const fixture = TestBed.createComponent(ConfirmationPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="google-review-card"]')).toBeNull();
  });

  it('does not show the review card when the restaurant has no review link', () => {
    sessionStorage.setItem(
      REVIEW_CONTEXT_KEY,
      JSON.stringify({ slug: 'naia-burger', tableId: 'table-1', orderId: 'order-1', status: 'nouvelle' }),
    );
    const orderStatusRealtimeService = { connect: vi.fn(), disconnect: vi.fn() };
    const orderService = {
      getOrder: vi.fn().mockReturnValue(of(orderFixture('servie'))),
    };
    const menuService = {
      getMenu: vi.fn().mockReturnValue(of({ restaurant: { googleReviewUrl: null } })),
    };

    TestBed.configureTestingModule({
      imports: [ConfirmationPageComponent],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: OrderStatusRealtimeService, useValue: orderStatusRealtimeService },
        { provide: MenuService, useValue: menuService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ orderId: 'order-1' }) } },
        },
      ],
    });

    const fixture = TestBed.createComponent(ConfirmationPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="google-review-card"]')).toBeNull();
  });
});
