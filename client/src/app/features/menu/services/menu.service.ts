import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, share, tap } from 'rxjs';
import { MenuView } from '../models/menu.model';

@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly apiUrl = '/api/public/menu';

  private readonly cache = new Map<string, MenuView>();
  private readonly inFlight = new Map<string, Observable<MenuView>>();

  /** Dernier menu chargé, exposé en signal pour les navigations internes. */
  readonly currentMenu = signal<MenuView | null>(null);

  constructor(private http: HttpClient) {}

  /**
   * Renvoie le menu depuis le cache quand il est déjà connu,
   * sinon déclenche un unique appel HTTP partagé pour le slug.
   */
  getMenu(slug: string): Observable<MenuView> {
    const cached = this.cache.get(slug);
    if (cached) {
      this.currentMenu.set(cached);
      return new Observable<MenuView>(observer => {
        observer.next(cached);
        observer.complete();
      });
    }

    const existing = this.inFlight.get(slug);
    if (existing) return existing;

    const request = this.http.get<MenuView>(`${this.apiUrl}/${slug}`).pipe(
      tap(menu => {
        this.cache.set(slug, menu);
        this.currentMenu.set(menu);
      }),
      catchError(err => {
        this.inFlight.delete(slug);
        throw err;
      }),
      share(),
    );

    this.inFlight.set(slug, request);
    return request;
  }
}
