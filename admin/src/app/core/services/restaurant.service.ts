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
  themeId: string;
  paymentProviderAccountId: string | null;
  clientBaseUrl: string;
}

export interface Table {
  id: string;
  number: number;
}

export interface OnboardingRequest {
  name: string;
  tableCount: number;
  themeId: string;
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
    data: Partial<Pick<Restaurant, 'name' | 'address' | 'logoPath' | 'themeId' | 'paymentProviderAccountId'>>,
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
