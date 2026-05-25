import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MenuView } from '../models/menu.model';

@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly apiUrl = '/api/public/menu';

  constructor(private http: HttpClient) {}

  getMenu(slug: string): Observable<MenuView> {
    return this.http.get<MenuView>(`${this.apiUrl}/${slug}`);
  }
}
