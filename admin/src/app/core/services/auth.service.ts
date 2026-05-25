import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface AuthResponse {
  token: string;
  userId: string;
}

export interface SignupRequest {
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private tokenKey = 'qr_restaurant_token';
  private userIdKey = 'qr_restaurant_user_id';

  token = signal<string | null>(localStorage.getItem(this.tokenKey));
  userId = signal<string | null>(localStorage.getItem(this.userIdKey));
  isAuthenticated = computed(() => !!this.token());

  signup(data: SignupRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/signup', data).pipe(
      tap(res => this.setSession(res))
    );
  }

  login(data: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', data).pipe(
      tap(res => this.setSession(res))
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userIdKey);
    this.token.set(null);
    this.userId.set(null);
  }

  private setSession(res: AuthResponse): void {
    localStorage.setItem(this.tokenKey, res.token);
    localStorage.setItem(this.userIdKey, res.userId);
    this.token.set(res.token);
    this.userId.set(res.userId);
  }
}
