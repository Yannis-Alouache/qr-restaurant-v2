import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MenuView } from '../models/menu.model';
import { normalizeMenuAssetUrls } from '../utils/menu-asset-url.util';

@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly apiUrl = '/api/public/menu';
  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);

  getMenu(slug: string): Observable<MenuView> {
    return this.http.get<MenuView>(`${this.apiUrl}/${slug}`).pipe(
      map((menu) => normalizeMenuAssetUrls(menu, this.document.baseURI)),
    );
  }
}
