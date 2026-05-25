import { OrderStatusRealtimeService } from './order-status-realtime.service';

let clientConfig: Record<string, unknown> | undefined;
let clientConstructorSpy = vi.fn();
let subscribeSpy = vi.fn();
let activateSpy = vi.fn();
let deactivateSpy = vi.fn();
let unsubscribeSpy = vi.fn();

type OrderStatusRealtimeServiceForTest = OrderStatusRealtimeService & {
  createSocket: () => unknown;
  createClient: (config: Record<string, unknown>) => unknown;
};

describe('OrderStatusRealtimeService', () => {
  beforeEach(() => {
    clientConfig = undefined;
    clientConstructorSpy = vi.fn();
    unsubscribeSpy = vi.fn();
    subscribeSpy = vi.fn().mockReturnValue({ unsubscribe: unsubscribeSpy });
    activateSpy = vi.fn();
    deactivateSpy = vi.fn();
  });

  it('subscribes to order status updates over sockjs and stomp', () => {
    const service = new OrderStatusRealtimeService() as OrderStatusRealtimeServiceForTest;
    const onStatusChange = vi.fn();

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

    service.connect('order-1', onStatusChange);

    expect(clientConstructorSpy).toHaveBeenCalledOnce();
    expect(activateSpy).toHaveBeenCalledOnce();

    const webSocketFactory = clientConfig?.['webSocketFactory'] as (() => unknown) | undefined;
    expect(webSocketFactory).toBeTypeOf('function');
    webSocketFactory?.();
    expect(createSocketSpy).toHaveBeenCalledOnce();

    const onConnect = clientConfig?.['onConnect'] as (() => void) | undefined;
    onConnect?.();

    expect(subscribeSpy).toHaveBeenCalledWith('/topic/orders/order-1', expect.any(Function));

    const subscriptionHandler = subscribeSpy.mock.calls[0][1] as (message: {
      body: string;
    }) => void;
    subscriptionHandler({
      body: JSON.stringify({ orderId: 'order-1', status: 'prete' }),
    });

    expect(onStatusChange).toHaveBeenCalledWith('prete');
  });

  it('disconnects the stomp client explicitly', () => {
    const service = new OrderStatusRealtimeService() as OrderStatusRealtimeServiceForTest;

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

    service.connect('order-1', () => undefined);
    const onConnect = clientConfig?.['onConnect'] as (() => void) | undefined;
    onConnect?.();
    service.disconnect();

    expect(unsubscribeSpy).toHaveBeenCalledOnce();
    expect(deactivateSpy).toHaveBeenCalledOnce();
  });

  it('resubscribes cleanly when the stomp client reconnects', () => {
    const service = new OrderStatusRealtimeService() as OrderStatusRealtimeServiceForTest;

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

    service.connect('order-1', () => undefined);

    const onConnect = clientConfig?.['onConnect'] as (() => void) | undefined;
    onConnect?.();
    onConnect?.();

    expect(subscribeSpy).toHaveBeenCalledTimes(2);
    expect(unsubscribeSpy).toHaveBeenCalledOnce();
  });
});
