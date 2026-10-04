import { KitchenAlertService } from './kitchen-alert.service';

/**
 * Notification DOM remplacée par un doublon : jsdom n'en fournit pas et le
 * service lit `Notification.permission` pour décider d'afficher ou non.
 */
class FakeNotification {
  static permission: NotificationPermission = 'granted';
  static requestPermission = vi.fn(async (): Promise<NotificationPermission> => 'granted');
  onclick: (() => void) | null = null;
  close(): void {}
}

/** Service sous test avec des sutures concrètes : fréquences jouées, notifications émises, verrous d'écran. */
class TestableAlertService extends KitchenAlertService {
  playedFrequencies: number[] = [];
  notifications: { title: string; body: string }[] = [];
  lockRequests = 0;
  releasedLocks = 0;
  hidden = false;

  protected override createAudioContext(): AudioContext {
    const played = this.playedFrequencies;
    return {
      currentTime: 0,
      state: 'running',
      resume: () => Promise.resolve(),
      destination: {},
      createGain: () => ({
        gain: { setValueAtTime: () => {}, exponentialRampToValueAtTime: () => {} },
        connect: (target: unknown) => target,
      }),
      createOscillator: () => {
        let frequency = 0;
        return {
          get frequency() {
            return {
              get value() { return frequency; },
              set value(f: number) { frequency = f; },
            };
          },
          connect: (target: unknown) => target,
          start: () => { played.push(frequency); },
          stop: () => {},
        };
      },
    } as unknown as AudioContext;
  }

  protected override showSystemNotification(title: string, body: string): void {
    this.notifications.push({ title, body });
  }

  protected override isPageHidden(): boolean {
    return this.hidden;
  }

  protected override acquireScreenLock() {
    this.lockRequests++;
    return Promise.resolve({
      release: async () => { this.releasedLocks++; },
      addEventListener: () => {},
    });
  }
}

describe('KitchenAlertService', () => {
  let service: TestableAlertService;

  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal('Notification', FakeNotification);
    vi.useFakeTimers();
    service = new TestableAlertService();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('est désactivé par défaut', () => {
    expect(service.enabled()).toBe(false);
  });

  it('enable() active, persiste la préférence, déverrouille l\'audio et verrouille l\'écran', async () => {
    const notificationsGranted = await service.enable();

    expect(notificationsGranted).toBe(true);
    expect(service.enabled()).toBe(true);
    expect(localStorage.getItem('kitchen.alerts.enabled')).toBe('true');
    expect(FakeNotification.requestPermission).toHaveBeenCalled();
    expect(service.lockRequests).toBe(1);
  });

  it('enable() renvoie false quand les notifications sont refusées, mais laisse le son actif', async () => {
    FakeNotification.requestPermission.mockResolvedValue('denied');

    const notificationsGranted = await service.enable();

    expect(notificationsGranted).toBe(false);
    expect(service.enabled()).toBe(true);
  });

  it('onNewOrder() est sans effet tant que les alertes sont désactivées', () => {
    service.onNewOrder({ tableNumber: 12, total: 24.5 });

    expect(service.playedFrequencies).toEqual([]);
    expect(service.notifications).toEqual([]);
  });

  it('onNewOrder() sonne, notifie quand l\'onglet est caché et programme un rappel à 30 s', async () => {
    await service.enable();
    service.hidden = true;

    service.onNewOrder({ tableNumber: 12, total: 24.5 });

    expect(service.playedFrequencies).toEqual([880, 1174.66]);
    expect(service.notifications).toEqual([
      { title: 'Nouvelle commande', body: 'Table 12 — 24,50 €' },
    ]);

    await vi.advanceTimersByTimeAsync(30_000);

    expect(service.playedFrequencies).toEqual([880, 1174.66, 880, 1174.66]);
  });

  it('ne notifie pas quand la page est visible : le board se met à jour sous les yeux', async () => {
    await service.enable();

    service.onNewOrder({ tableNumber: 3, total: 18 });

    expect(service.playedFrequencies).toEqual([880, 1174.66]);
    expect(service.notifications).toEqual([]);
  });

  it('setPendingNewOrders(0) arrête le rappel sonore', async () => {
    await service.enable();

    service.onNewOrder({ tableNumber: 3, total: 18 });
    service.setPendingNewOrders(0);
    await vi.advanceTimersByTimeAsync(60_000);

    expect(service.playedFrequencies).toHaveLength(2);
  });

  it('disable() coupe la boucle, relâche l\'écran et efface la persistance', async () => {
    await service.enable();
    service.onNewOrder({ tableNumber: 3, total: 18 });

    service.disable();

    expect(service.enabled()).toBe(false);
    expect(localStorage.getItem('kitchen.alerts.enabled')).toBe('false');
    expect(service.releasedLocks).toBe(1);

    await vi.advanceTimersByTimeAsync(30_000);
    expect(service.playedFrequencies).toHaveLength(2);
  });

  it('restaure la préférence au rechargement de la page', async () => {
    localStorage.setItem('kitchen.alerts.enabled', 'true');

    const reloaded = new TestableAlertService();
    await Promise.resolve(); // le verrouillage d'écran est différé après le constructeur

    expect(reloaded.enabled()).toBe(true);
    expect(reloaded.lockRequests).toBe(1);
  });
});
