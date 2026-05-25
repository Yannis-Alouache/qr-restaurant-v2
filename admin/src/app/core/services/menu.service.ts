import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface CategoryView {
  id: string;
  name: string;
  imagePath: string | null;
  position: number;
  hasMenu: boolean;
}

export interface MenuItemView {
  id: string;
  categoryId: string;
  name: string;
  description: string | null;
  price: number;
  imagePath: string | null;
  available: boolean;
  menuVariantOf: string | null;
}

export interface CompositionView {
  id: string;
  compositionType: string;
  menuItemId: string;
  supplementPrice: number;
}

export interface CreateCategoryRequest {
  name: string;
  imagePath?: string;
  position?: number;
  hasMenu?: boolean;
}

export interface CreateMenuItemRequest {
  categoryId: string;
  name: string;
  description?: string;
  price: number;
  imagePath?: string;
  menuVariantOf?: string | null;
}

export interface CreateCompositionRequest {
  compositionType: string;
  menuItemId: string;
  supplementPrice?: number;
}

@Injectable({ providedIn: 'root' })
export class MenuService {
  private http = inject(HttpClient);
  categories = signal<CategoryView[]>([]);
  menuItems = signal<MenuItemView[]>([]);
  compositions = signal<CompositionView[]>([]);

  loadCategories(): Observable<CategoryView[]> {
    return this.http.get<CategoryView[]>('/api/admin/categories').pipe(
      tap(c => this.categories.set(c))
    );
  }

  createCategory(data: CreateCategoryRequest): Observable<CategoryView> {
    return this.http.post<CategoryView>('/api/admin/categories', data);
  }

  updateCategory(id: string, data: Partial<CreateCategoryRequest>): Observable<CategoryView> {
    return this.http.put<CategoryView>(`/api/admin/categories/${id}`, data);
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/categories/${id}`);
  }

  loadMenuItems(): Observable<MenuItemView[]> {
    return this.http.get<MenuItemView[]>('/api/admin/menu-items').pipe(
      tap(i => this.menuItems.set(i))
    );
  }

  createMenuItem(data: CreateMenuItemRequest): Observable<MenuItemView> {
    return this.http.post<MenuItemView>('/api/admin/menu-items', data);
  }

  updateMenuItem(id: string, data: Partial<CreateMenuItemRequest & { available?: boolean }>): Observable<MenuItemView> {
    return this.http.put<MenuItemView>(`/api/admin/menu-items/${id}`, data);
  }

  toggleAvailability(id: string, available: boolean): Observable<void> {
    return this.http.patch<void>(`/api/admin/menu-items/${id}/availability`, { available });
  }

  deleteMenuItem(id: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/menu-items/${id}`);
  }

  loadCompositions(): Observable<CompositionView[]> {
    return this.http.get<CompositionView[]>('/api/admin/compositions').pipe(
      tap(c => this.compositions.set(c))
    );
  }

  createComposition(data: CreateCompositionRequest): Observable<CompositionView> {
    return this.http.post<CompositionView>('/api/admin/compositions', data);
  }

  deleteComposition(id: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/compositions/${id}`);
  }
}
