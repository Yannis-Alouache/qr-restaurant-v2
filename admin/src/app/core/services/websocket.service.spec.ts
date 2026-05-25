import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { RestaurantService } from './restaurant.service';
import { WebSocketService } from './websocket.service';

let clientConfig: Record<string, unknown> | undefined;
let clientConstructorSpy = vi.fn();
let subscribeSpy = vi.fn();
let activateSpy = vi.fn();
let deactivateSpy = vi.fn();
let unsubscribeSpy = vi.fn();

type WebSocketServiceForTest = WebSocketService & {
  createSocket: () => unknown;
  createClient: (config: Record<string, unknown>) => unknown;
};

describe('WebSocketService', () => {
  beforeEach(() => {
    clientConfig = undefined;
    clientConstructorSpy = vi.fn();
    unsubscribeSpy = vi.fn();
    subscribeSpy = vi.fn().mockReturnValue({ unsubscribe: unsubscribeSpy });
    activateSpy = vi.fn();
    deactivateSpy = vi.fn();

    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        {
          provide: RestaurantService,
          useValue: {
            restaurant: signal({
              id: 'restaurant-1',
              name: 'Naia Burger',
              slug: 'naia-burger',
              address: null,
              logoPath: null,
              themeId: 'chaud',
              clientBaseUrl: 'https://client.example',
            }),
          },
        },
      ],
    });
  });

  it('subscribes to restaurant order updates over sockjs and stomp', () => {
    const service = TestBed.inject(WebSocketService) as WebSocketServiceForTest;
    const onMessage = vi.fn();

    const createSocketSpy = vi.spyOn(service, 'createSocket').mockReturnValue({ readyState: 1 });
    vi.spyOn(service, 'createClient').mockImplementation((config) => {
      clientConstructorSpy(config);
      clientConfig = config;

      return {
        active: false,
        activate: activateSpy,
        deactivate: deactivateSpy,
        subscribe: subscribeSpy,
      };
    });

    service.connect(onMessage);

    expect(clientConstructorSpy).toHaveBeenCalledOnce();
    expect(activateSpy).toHaveBeenCalledOnce();

    const webSocketFactory = clientConfig?.['webSocketFactory'] as (() => unknown) | undefined;
    expect(webSocketFactory).toBeTypeOf('function');
    webSocketFactory?.();
    expect(createSocketSpy).toHaveBeenCalledOnce();

    const onConnect = clientConfig?.['onConnect'] as (() => void) | undefined;
    onConnect?.();

    expect(service.connected()).toBe(true);
    expect(subscribeSpy).toHaveBeenCalledWith(
      '/topic/restaurants/restaurant-1/orders',
      expect.any(Function),
    );

    const subscriptionHandler = subscribeSpy.mock.calls[0][1] as (message: {
      body: string;
    }) => void;
    subscriptionHandler({
      body: JSON.stringify({ orderId: 'order-1', status: 'nouvelle' }),
    });

    expect(onMessage).toHaveBeenCalledWith({ orderId: 'order-1', status: 'nouvelle' });
  });

  it('disconnects the stomp client explicitly', () => {
    const service = TestBed.inject(WebSocketService) as WebSocketServiceForTest;

    vi.spyOn(service, 'createClient').mockImplementation((config) => {
      clientConstructorSpy(config);
      clientConfig = config;

      return {
        active: false,
        activate: activateSpy,
        deactivate: deactivateSpy,
        subscribe: subscribeSpy,
      };
    });

    service.connect(() => undefined);
    const onConnect = clientConfig?.['onConnect'] as (() => void) | undefined;
    onConnect?.();
    service.disconnect();

    expect(unsubscribeSpy).toHaveBeenCalledOnce();
    expect(deactivateSpy).toHaveBeenCalledOnce();
    expect(service.connected()).toBe(false);
  });

  it('resubscribes cleanly when the stomp client reconnects', () => {
    const service = TestBed.inject(WebSocketService) as WebSocketServiceForTest;

    vi.spyOn(service, 'createClient').mockImplementation((config) => {
      clientConstructorSpy(config);
      clientConfig = config;

      return {
        active: false,
        activate: activateSpy,
        deactivate: deactivateSpy,
        subscribe: subscribeSpy,
      };
    });

    service.connect(() => undefined);

    const onConnect = clientConfig?.['onConnect'] as (() => void) | undefined;
    onConnect?.();
    onConnect?.();

    expect(subscribeSpy).toHaveBeenCalledTimes(2);
    expect(unsubscribeSpy).toHaveBeenCalledOnce();
  });
});
