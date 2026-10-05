import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import {
  StatsService,
  readStoredMonths,
  MONTHS_STORAGE_KEY,
  MAX_MONTHS,
  MIN_MONTHS,
  RestaurantStats,
} from './stats.service';

const SAMPLE: RestaurantStats = {
  period: { start: '2025-10-01', end: '2026-09-30' },
  summary: {
    totalOrders: 152,
    totalRevenue: 2389.4,
    averageOrderValue: 15.72,
    refundedOrders: 2,
    refundedAmount: 35.9,
  },
  monthly: [
    { month: '2026-08', orders: 0, revenue: 0, averageOrderValue: 0 },
    { month: '2026-09', orders: 32, revenue: 512.35, averageOrderValue: 16.01 },
  ],
  topItems: [
    { menuItemId: 'e0ee-1', name: 'Menu Burger bacon', quantitySold: 4, revenue: 34 },
  ],
};

describe('StatsService', () => {
  let service: StatsService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StatsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('appelle /api/admin/stats avec la fenêtre demandée et expose la réponse', () => {
    let received: RestaurantStats | undefined;
    service.getStats(6).subscribe(r => (received = r));

    const req = http.expectOne('/api/admin/stats?months=6');
    expect(req.request.method).toBe('GET');
    req.flush(SAMPLE);

    expect(received).toEqual(SAMPLE);
  });

  it('persiste la fenêtre choisie et la borne entre ' + MIN_MONTHS + ' et ' + MAX_MONTHS, () => {
    service.setMonths(6);
    expect(service.selectedMonths()).toBe(6);
    expect(localStorage.getItem(MONTHS_STORAGE_KEY)).toBe('6');

    service.setMonths(MAX_MONTHS + 10);
    expect(service.selectedMonths()).toBe(MAX_MONTHS);
    expect(localStorage.getItem(MONTHS_STORAGE_KEY)).toBe(String(MAX_MONTHS));

    service.setMonths(MIN_MONTHS - 5);
    expect(service.selectedMonths()).toBe(MIN_MONTHS);
    expect(localStorage.getItem(MONTHS_STORAGE_KEY)).toBe(String(MIN_MONTHS));
  });

  it('survit à un stockage indisponible sans planter', () => {
    const setItem = Storage.prototype.setItem;
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });

    expect(() => service.setMonths(3)).not.toThrow();
    expect(service.selectedMonths()).toBe(3);

    Storage.prototype.setItem = setItem;
  });
});

describe('readStoredMonths', () => {
  afterEach(() => localStorage.clear());

  it('renvoie 12 mois par défaut en l\'absence de préférence', () => {
    expect(readStoredMonths()).toBe(12);
  });

  it('restaure la préférence persistée', () => {
    localStorage.setItem(MONTHS_STORAGE_KEY, '7');
    expect(readStoredMonths()).toBe(7);
  });

  it('borne ou ignore les valeurs invalides', () => {
    localStorage.setItem(MONTHS_STORAGE_KEY, '99');
    expect(readStoredMonths()).toBe(MAX_MONTHS);

    localStorage.setItem(MONTHS_STORAGE_KEY, '-3');
    expect(readStoredMonths()).toBe(MIN_MONTHS);

    localStorage.setItem(MONTHS_STORAGE_KEY, 'douze');
    expect(readStoredMonths()).toBe(12);
  });
});
