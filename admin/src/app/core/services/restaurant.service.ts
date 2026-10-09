import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface Restaurant {
  id: string;
  name: string;
  slug: string;
  address: string | null;
  logoPath: string | null;
  coverPath: string | null;
  themeId: string;
  paymentProviderAccountId: string | null;
  stripeConnectStatus: 'pending' | 'active' | 'restricted' | null;
  googleReviewUrl: string | null;
  clientBaseUrl: string;
}

export interface StripeConnectStatusView {
  paymentProviderAccountId: string | null;
  stripeConnectStatus: Restaurant['stripeConnectStatus'];
}

export interface Table {
  id: string;
  number: number;
}

export interface OnboardingRequest {
  name: string;
  tableCount: number;
  themeId: string;
  logoPath?: string;
}

export interface OnboardingResponse {
  id: string;
  slug: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class RestaurantService {
  private http = inject(HttpClient);
  restaurant = signal<Restaurant | null>(null);
  tables = signal<Table[]>([]);
  hasRestaurant = signal<boolean | null>(null);

  loadRestaurant(): Observable<Restaurant> {
    return this.http.get<Restaurant>('/api/admin/restaurant').pipe(
      tap(r => {
        this.restaurant.set(r);
        this.hasRestaurant.set(true);
      })
    );
  }

  updateRestaurant(
    data: Partial<Pick<Restaurant, 'name' | 'address' | 'logoPath' | 'coverPath' | 'themeId' | 'paymentProviderAccountId' | 'googleReviewUrl'>>,
  ): Observable<Restaurant> {
    return this.http.put<Restaurant>('/api/admin/restaurant', data).pipe(
      tap(r => this.restaurant.set(r))
    );
  }

  loadTables(): Observable<Table[]> {
    return this.http.get<Table[]>('/api/admin/restaurant/tables').pipe(
      tap(t => this.tables.set(t))
    );
  }

  /** Ouvre le flux Stripe Connect : crée le compte Express si besoin, renvoie l'URL d'onboarding. */
  startStripeOnboarding(): Observable<{ url: string; paymentProviderAccountId: string }> {
    return this.http.post<{ url: string; paymentProviderAccountId: string }>(
      '/api/admin/connect/stripe/onboarding', {});
  }

  /** Relit le statut du compte auprès de Stripe et met à jour l'état local. */
  refreshStripeStatus(): Observable<StripeConnectStatusView> {
    return this.http.get<StripeConnectStatusView>('/api/admin/connect/stripe/status').pipe(
      tap(status => this.restaurant.update(r => r
        ? { ...r, paymentProviderAccountId: status.paymentProviderAccountId, stripeConnectStatus: status.stripeConnectStatus }
        : r))
    );
  }

  createRestaurant(data: OnboardingRequest): Observable<OnboardingResponse> {
    return this.http.post<OnboardingResponse>('/api/admin/restaurants', data).pipe(
      tap(() => this.hasRestaurant.set(true))
    );
  }

  checkAndLoad(): void {
    this.loadRestaurant().subscribe({
      next: () => {},
      error: () => this.hasRestaurant.set(false)
    });
  }
}
