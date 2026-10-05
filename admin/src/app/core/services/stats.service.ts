import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface StatsPeriod {
  start: string;
  end: string;
}

export interface StatsSummary {
  totalOrders: number;
  totalRevenue: number;
  averageOrderValue: number;
  refundedOrders: number;
  refundedAmount: number;
}

export interface MonthlyStats {
  /** « YYYY-MM », de gauche à droite, mois sans activité compris (à zéro). */
  month: string;
  orders: number;
  revenue: number;
  averageOrderValue: number;
}

export interface TopItemStats {
  menuItemId: string;
  name: string;
  quantitySold: number;
  revenue: number;
}

export interface RestaurantStats {
  period: StatsPeriod;
  summary: StatsSummary;
  monthly: MonthlyStats[];
  topItems: TopItemStats[];
}

export const MIN_MONTHS = 1;
export const MAX_MONTHS = 24;

export const MONTHS_STORAGE_KEY = 'menzo-stats:months';

/** Fenêtre par défaut et bornes acceptées par l'API (400 sinon). */
export function readStoredMonths(): number {
  try {
    const stored = parseInt(localStorage.getItem(MONTHS_STORAGE_KEY) ?? '', 10);
    if (!Number.isNaN(stored)) {
      return Math.min(MAX_MONTHS, Math.max(MIN_MONTHS, stored));
    }
  } catch {
    // stockage indisponible (navigation privée) : fenêtre par défaut
  }
  return 12;
}

@Injectable({ providedIn: 'root' })
export class StatsService {
  private http = inject(HttpClient);

  /** Fenêtre sélectionnée (1-24 mois), persistée d'une session à l'autre. */
  selectedMonths = signal<number>(readStoredMonths());

  getStats(months: number): Observable<RestaurantStats> {
    return this.http.get<RestaurantStats>('/api/admin/stats', { params: { months } });
  }

  setMonths(months: number): void {
    const clamped = Math.min(MAX_MONTHS, Math.max(MIN_MONTHS, months));
    this.selectedMonths.set(clamped);
    try {
      localStorage.setItem(MONTHS_STORAGE_KEY, String(clamped));
    } catch {
      // stockage indisponible : la préférence reste en mémoire pour la session
    }
  }
}
