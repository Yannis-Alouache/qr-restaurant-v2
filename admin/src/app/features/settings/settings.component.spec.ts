import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import QRCode from 'qrcode';
import { of } from 'rxjs';
import { ImageService } from '../../core/services/image.service';
import { Restaurant, RestaurantService } from '../../core/services/restaurant.service';
import { SettingsComponent } from './settings.component';

vi.mock('qrcode', () => ({
  default: {
    toDataURL: vi.fn(),
  },
}));

describe('SettingsComponent', () => {
  it('patches the settings form when the restaurant is loaded after component init', () => {
    const restaurantSignal = signal<Restaurant | null>(null);

    TestBed.configureTestingModule({
      imports: [SettingsComponent],
      providers: [
        {
          provide: RestaurantService,
          useValue: {
            restaurant: restaurantSignal,
            tables: signal([]),
            updateRestaurant: vi.fn(),
            loadTables: vi.fn(),
          },
        },
        {
          provide: ImageService,
          useValue: {
            upload: vi.fn(),
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(SettingsComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    restaurantSignal.set({
      id: 'restaurant-1',
      name: 'Naia Burger',
      slug: 'naia-burger',
      address: '12 Rue de la Paix',
      logoPath: null,
      themeId: 'chaud',
      paymentProviderAccountId: 'acct_seed_test',
      clientBaseUrl: 'https://client.example',
    });
    fixture.detectChanges();

    expect(component.form.getRawValue()).toEqual({
      name: 'Naia Burger',
      address: '12 Rue de la Paix',
      themeId: 'chaud',
      paymentProviderAccountId: 'acct_seed_test',
    });
  });

  it('generates qr codes with the configured client base url', async () => {
    vi.mocked(QRCode.toDataURL).mockImplementation(
      () => Promise.resolve('data:image/png;base64,qr-code') as Promise<string>,
    );

    TestBed.configureTestingModule({
      imports: [SettingsComponent],
      providers: [
        {
          provide: RestaurantService,
          useValue: {
            restaurant: signal<Restaurant | null>({
              id: 'restaurant-1',
              name: 'Naia Burger',
              slug: 'naia-burger',
              address: null,
              logoPath: null,
              themeId: 'chaud',
              paymentProviderAccountId: 'acct_seed_test',
              clientBaseUrl: 'https://client.example',
            }),
            tables: signal([
              { id: 'table-1', number: 1 },
            ]),
            updateRestaurant: vi.fn(),
            loadTables: vi.fn().mockReturnValue(of([])),
          },
        },
        {
          provide: ImageService,
          useValue: {
            upload: vi.fn(),
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(SettingsComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.generateQrCodes();
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(QRCode.toDataURL).toHaveBeenCalledWith(
      'https://client.example/menu/naia-burger/table-1',
      { width: 256, margin: 2 },
    );
    expect(component.showQrPanel).toBe(true);
    expect(component.qrCodes().get('table-1')).toBe('data:image/png;base64,qr-code');
  });

  it('saves the payment provider account id from settings', () => {
    const updateRestaurant = vi.fn().mockReturnValue(of({
      id: 'restaurant-1',
      name: 'Naia Burger',
      slug: 'naia-burger',
      address: '12 Rue de la Paix',
      logoPath: null,
      themeId: 'chaud',
      paymentProviderAccountId: 'acct_live_updated',
      clientBaseUrl: 'https://client.example',
    }));

    TestBed.configureTestingModule({
      imports: [SettingsComponent],
      providers: [
        {
          provide: RestaurantService,
          useValue: {
            restaurant: signal<Restaurant | null>({
              id: 'restaurant-1',
              name: 'Naia Burger',
              slug: 'naia-burger',
              address: '12 Rue de la Paix',
              logoPath: null,
              themeId: 'chaud',
              paymentProviderAccountId: 'acct_seed_test',
              clientBaseUrl: 'https://client.example',
            }),
            tables: signal([]),
            updateRestaurant,
            loadTables: vi.fn(),
          },
        },
        {
          provide: ImageService,
          useValue: {
            upload: vi.fn(),
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(SettingsComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({ paymentProviderAccountId: 'acct_live_updated' });
    component.save();

    expect(updateRestaurant).toHaveBeenCalledWith(expect.objectContaining({
      paymentProviderAccountId: 'acct_live_updated',
    }));
  });
});
