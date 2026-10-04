import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';

const STORAGE_KEY = 'qr-restaurant-cart';

describe('CartService', () => {
  let service: CartService;

  beforeEach(() => {
    sessionStorage.removeItem(STORAGE_KEY);
    TestBed.configureTestingModule({});
    service = TestBed.inject(CartService);
  });

  afterEach(() => {
    sessionStorage.removeItem(STORAGE_KEY);
  });

  /** Simule un rafraîchissement : nouvelle instance, donc relecture du stockage. */
  function injectFreshService(): CartService {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    return TestBed.inject(CartService);
  }

  it('persiste le panier dans sessionStorage à chaque ajout', () => {
    service.addStandalone('item-1', 'Burger', 12.5, null);

    const stored = JSON.parse(sessionStorage.getItem(STORAGE_KEY) ?? '[]');
    expect(stored).toHaveLength(1);
    expect(stored[0]).toMatchObject({ type: 'standalone', itemId: 'item-1', quantity: 1 });
  });

  it('restaure le panier au démarrage du service (après rafraîchissement)', () => {
    service.addCombo(
      { id: 'menu-1', name: 'Big Burger', price: 14, imagePath: null },
      { id: 'side-1', name: 'Frites', supplementPrice: 0, imagePath: null },
      { id: 'drink-1', name: 'Soda', supplementPrice: 1.5, imagePath: null },
    );
    const restored = injectFreshService();

    expect(restored.itemCount()).toBe(1);
    expect(restored.total()).toBe(15.5);
    expect(restored.cartEntries()[0].type).toBe('combo');
  });

  it('persiste les mises à jour de quantité et les suppressions', () => {
    service.addStandalone('item-1', 'Burger', 12.5, null);
    const cartId = service.cartEntries()[0].cartId;

    service.updateQuantity(cartId, 3);
    let stored = JSON.parse(sessionStorage.getItem(STORAGE_KEY) ?? '[]');
    expect(stored[0].quantity).toBe(3);

    service.remove(cartId);
    stored = JSON.parse(sessionStorage.getItem(STORAGE_KEY) ?? '[]');
    expect(stored).toHaveLength(0);
  });

  it('vide le stockage lors du clear (panier payé)', () => {
    service.addStandalone('item-1', 'Burger', 12.5, null);

    service.clear();

    expect(service.isEmpty()).toBe(true);
    expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('repart d\'un panier vide si le payload stocké est corrompu', () => {
    sessionStorage.setItem(STORAGE_KEY, '{not-json');

    const restored = injectFreshService();

    expect(restored.isEmpty()).toBe(true);
  });

  it('ignore les entrées stockées qui ne respectent pas le schéma', () => {
    const payload = JSON.stringify([
      { type: 'standalone' },
      {
        type: 'standalone', cartId: 'a', itemId: 'item-1', name: 'Burger',
        price: 12.5, imagePath: null, quantity: 2,
      },
      { type: 'combo', cartId: 'b', quantity: 1 },
      'garbage',
    ]);
    sessionStorage.setItem(STORAGE_KEY, payload);

    const restored = injectFreshService();

    expect(restored.itemCount()).toBe(2);
    expect(restored.cartEntries()).toHaveLength(1);
    expect(restored.cartEntries()[0]).toMatchObject({
      type: 'standalone', itemId: 'item-1', quantity: 2,
    });
  });
});
