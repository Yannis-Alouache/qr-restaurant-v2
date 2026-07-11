import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap, map, catchError, finalize } from 'rxjs';

/**
 * Auth response from the API. The JWT itself lives in an httpOnly cookie set
 * by the server — the body only ever carries the authenticated user id.
 */
export interface AuthResponse {
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
  private userIdKey = 'qr_restaurant_user_id';
  private emailKey = 'qr_restaurant_email';

  /** Persisted only to personalise the UI (e.g. avatar initials). */
  userId = signal<string | null>(localStorage.getItem(this.userIdKey));
  email = signal<string | null>(localStorage.getItem(this.emailKey));
  /** Optimistically authenticated when a userId is remembered; verified via /me on first guard. */
  isAuthenticated = signal<boolean>(!!this.userId());

  /** True only after /me (or login/signup) confirmed the cookie this session. */
  private sessionVerified = false;
  private resolving: Observable<boolean> | null = null;

  signup(data: SignupRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/signup', data).pipe(
      tap(res => this.setAuthed(res.userId, data.email)),
    );
  }

  login(data: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', data).pipe(
      tap(res => this.setAuthed(res.userId, data.email)),
    );
  }

  /** Expires the server cookie, then clears local state regardless of the response. */
  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', null).pipe(
      tap(() => this.clear()),
      catchError(() => {
        this.clear();
        return of(void 0);
      }),
    );
  }

  /**
   * Verifies the session against the httpOnly cookie. Resolves exactly once
   * for concurrent callers. Returns true when the cookie is still valid.
   */
  resolveSession(): Observable<boolean> {
    if (this.sessionVerified && this.isAuthenticated()) {
      return of(true);
    }
    if (this.resolving) {
      return this.resolving;
    }
    this.resolving = this.http.get<AuthResponse>('/api/auth/me').pipe(
      tap(res => this.setAuthed(res.userId)),
      map(() => true),
      catchError(() => {
        this.clear();
        return of(false);
      }),
      finalize(() => {
        this.resolving = null;
      }),
    );
    return this.resolving;
  }

  private setAuthed(userId: string, email?: string): void {
    localStorage.setItem(this.userIdKey, userId);
    this.userId.set(userId);
    if (email) {
      localStorage.setItem(this.emailKey, email);
      this.email.set(email);
    }
    this.isAuthenticated.set(true);
    this.sessionVerified = true;
  }

  private clear(): void {
    localStorage.removeItem(this.userIdKey);
    localStorage.removeItem(this.emailKey);
    this.userId.set(null);
    this.email.set(null);
    this.isAuthenticated.set(false);
    this.sessionVerified = false;
  }
}
