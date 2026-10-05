import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { StatsComponent } from './stats.component';
import { RestaurantStats } from '../../core/services/stats.service';

function statsOf(monthly: [number, number][], totalOrders: number, totalRevenue: number): RestaurantStats {
  return {
    period: { start: '2024-10-01', end: '2026-09-30' },
    summary: {
      totalOrders,
      totalRevenue,
      averageOrderValue: totalOrders > 0 ? totalRevenue / totalOrders : 0,
      refundedOrders: 1,
      refundedAmount: 19.4,
    },
    monthly: monthly.map(([orders, revenue], i) => ({
      month: `2025-${String(i + 1).padStart(2, '0')}`,
      orders,
      revenue,
      averageOrderValue: orders > 0 ? revenue / orders : 0,
    })),
    topItems: [
      { menuItemId: 'a', name: 'Menu Burger bacon', quantitySold: 4, revenue: 34 },
      { menuItemId: 'b', name: 'Frites', quantitySold: 5, revenue: 17.5 },
    ],
  };
}

describe('StatsComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [StatsComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  // forkJoin annule la fenêtre 2N dès que la fenêtre courante échoue : les
  // requêtes volontairement abortées ne doivent pas faire échouer verify().
  afterEach(() => http.verify({ ignoreCancelled: true }));

  function flushDashboard(months: number, fixture: ComponentFixture<StatsComponent>) {
    const current = http.expectOne(`/api/admin/stats?months=${months}`);
    current.flush(statsOf(Array.from({ length: months }, () => [2, 30] as [number, number]), 24, 720));
    const extended = http.expectOne(`/api/admin/stats?months=${months * 2}`);
    extended.flush(statsOf(Array.from({ length: months * 2 }, () => [2, 30] as [number, number]), 48, 1440));
    fixture.detectChanges();
  }

  it('affiche les KPIs, le graphique et le top 12 mois par défaut', () => {
    const fixture = TestBed.createComponent(StatsComponent);
    fixture.detectChanges();
    flushDashboard(12, fixture);

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="kpi-revenue-value"]')?.textContent).toMatch(/720,00\s€/);
    expect(el.querySelector('[data-testid="kpi-orders-value"]')?.textContent).toContain('24');
    expect(el.querySelector('[data-testid="kpi-refunds-value"]')?.textContent).toMatch(/19,40\s€/);
    // deltas vs 12 mois précédents : 24 → 24 commandes, croissance nulle
    expect(el.textContent).toContain('vs 12 mois précédents');
    expect(el.querySelectorAll('.chart-svg .hit').length).toBe(12);
    expect(el.querySelectorAll('.top-row').length).toBe(2);
    expect(el.querySelector('[data-testid="stats-empty"]')).toBeNull();
  });

  it('affiche l\'état vide quand aucune commande payée n\'existe sur la fenêtre', () => {
    const fixture = TestBed.createComponent(StatsComponent);
    fixture.detectChanges();

    const current = http.expectOne('/api/admin/stats?months=12');
    current.flush(statsOf(Array.from({ length: 12 }, () => [0, 0] as [number, number]), 0, 0));
    const extended = http.expectOne('/api/admin/stats?months=24');
    extended.flush(statsOf(Array.from({ length: 24 }, () => [0, 0] as [number, number]), 0, 0));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="stats-empty"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="kpi-revenue"]')).toBeNull();
  });

  it('rafraîchit la fenêtre via le sélecteur de période (appels N et 2N)', () => {
    const fixture = TestBed.createComponent(StatsComponent);
    fixture.detectChanges();
    flushDashboard(12, fixture);

    const select = fixture.nativeElement.querySelector('#stats-period') as HTMLSelectElement;
    select.value = '3';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    http.expectOne('/api/admin/stats?months=3').flush(
      statsOf(Array.from({ length: 3 }, () => [1, 15] as [number, number]), 3, 45));
    http.expectOne('/api/admin/stats?months=6').flush(
      statsOf(Array.from({ length: 6 }, () => [1, 15] as [number, number]), 6, 90));
    fixture.detectChanges();

    expect(localStorage.getItem('menzo-stats:months')).toBe('3');
    expect(fixture.nativeElement.querySelectorAll('.chart-svg .hit').length).toBe(3);
  });

  it('propose de réessayer quand l\'API échoue', () => {
    const fixture = TestBed.createComponent(StatsComponent);
    fixture.detectChanges();

    // L'échec de la fenêtre courante aborte la requête 2N (forkJoin) :
    // verify({ignoreCancelled}) l'absout, inutile de la satisfaire.
    http.expectOne('/api/admin/stats?months=12').flush('boom', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="stats-error"]')).not.toBeNull();
  });
});
