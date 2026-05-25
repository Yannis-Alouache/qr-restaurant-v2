import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { OrderService } from '../../core/services/order.service';
import { RestaurantService } from '../../core/services/restaurant.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { OrdersComponent } from './orders.component';

describe('OrdersComponent', () => {
  it('connects to realtime updates when the restaurant arrives after the page is created', () => {
    const restaurant = signal<ReturnType<RestaurantService['restaurant']>>(null);
    const orderService = {
      orders: signal([]),
      loadOrders: vi.fn().mockReturnValue(of([])),
      updateStatus: vi.fn(),
    };
    const webSocketService = {
      connected: signal(false),
      connect: vi.fn(),
      disconnect: vi.fn(),
    };

    TestBed.configureTestingModule({
      imports: [OrdersComponent],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: WebSocketService, useValue: webSocketService },
        {
          provide: RestaurantService,
          useValue: {
            restaurant,
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(OrdersComponent);

    fixture.detectChanges();

    expect(orderService.loadOrders).toHaveBeenCalledOnce();
    expect(webSocketService.connect).not.toHaveBeenCalled();

    restaurant.set({
      id: 'restaurant-1',
      name: 'Naia Burger',
      slug: 'naia-burger',
      address: null,
      logoPath: null,
      themeId: 'chaud',
      paymentProviderAccountId: null,
      clientBaseUrl: 'https://client.example',
    });
    fixture.detectChanges();

    expect(webSocketService.connect).toHaveBeenCalledOnce();
    expect(webSocketService.connect).toHaveBeenCalledWith(expect.any(Function));
  });
});
