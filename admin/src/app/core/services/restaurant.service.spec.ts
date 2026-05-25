import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { RestaurantService, Restaurant } from './restaurant.service';

describe('RestaurantService', () => {
  let service: RestaurantService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(RestaurantService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('stores the restaurant after loadRestaurant succeeds', () => {
    const restaurant: Restaurant = {
      id: 'resto-1',
      name: 'Naia Burger',
      slug: 'naia-burger',
      address: '12 Rue de la Paix',
      logoPath: null,
      themeId: 'chaud',
      paymentProviderAccountId: 'acct_seed_test',
      clientBaseUrl: 'https://client.example',
    };

    service.loadRestaurant().subscribe((result) => {
      expect(result).toEqual(restaurant);
    });

    const request = httpTesting.expectOne('/api/admin/restaurant');
    request.flush(restaurant);

    expect(service.restaurant()).toEqual(restaurant);
    expect(service.hasRestaurant()).toBe(true);
  });

  it('updates the cached restaurant after updateRestaurant succeeds', () => {
    const updatedRestaurant: Restaurant = {
      id: 'resto-1',
      name: 'Naia Burger',
      slug: 'naia-burger',
      address: '12 Rue de la Paix',
      logoPath: 'https://cdn.test/logo.png',
      themeId: 'nature',
      paymentProviderAccountId: 'acct_live_updated',
      clientBaseUrl: 'https://client.example',
    };

    service.updateRestaurant({ themeId: 'nature', paymentProviderAccountId: 'acct_live_updated' }).subscribe((result) => {
      expect(result).toEqual(updatedRestaurant);
    });

    const request = httpTesting.expectOne('/api/admin/restaurant');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ themeId: 'nature', paymentProviderAccountId: 'acct_live_updated' });
    request.flush(updatedRestaurant);

    expect(service.restaurant()).toEqual(updatedRestaurant);
  });
});
