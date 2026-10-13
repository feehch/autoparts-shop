import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { silent } from '../interceptors/http-context';
import { CartService } from './cart.service';
import { AuthResponse, LoginRequest, RegisterRequest, RoleName, UserDto } from '../models/api.models';

const ACCESS_KEY = 'ap.accessToken';
const REFRESH_KEY = 'ap.refreshToken';
const USER_KEY = 'ap.user';

/** Иерархия ролей: роль даёт права всех ролей ниже. */
const ROLE_LEVEL: Record<RoleName, number> = { CUSTOMER: 1, MANAGER: 2, ADMIN: 3 };

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly cart = inject(CartService);
  private readonly url = `${environment.apiUrl}/auth`;

  private readonly _currentUser = signal<UserDto | null>(readJson<UserDto>(USER_KEY));
  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this._currentUser() !== null);
  /** Максимальный уровень роли текущего пользователя. */
  private readonly level = computed(() =>
    Math.max(0, ...(this._currentUser()?.roles ?? []).map((r) => ROLE_LEVEL[r] ?? 0)),
  );
  readonly isManager = computed(() => this.level() >= ROLE_LEVEL.MANAGER);
  readonly isAdmin = computed(() => this.level() >= ROLE_LEVEL.ADMIN);

  private refreshInFlight$: Observable<AuthResponse> | null = null;

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  hasRole(role: RoleName): boolean {
    return this.level() >= (ROLE_LEVEL[role] ?? Number.MAX_SAFE_INTEGER);
  }

  hasAnyRole(roles: RoleName[]): boolean {
    return roles.length === 0 || roles.some((r) => this.hasRole(r));
  }

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.url}/login`, req).pipe(tap((r) => this.storeAuth(r)));
  }

  register(req: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.url}/register`, req).pipe(tap((r) => this.storeAuth(r)));
  }

  /** Обновление пары токенов. Одновременные вызовы разделяют один запрос. */
  refresh(): Observable<AuthResponse> {
    const refreshToken = this.refreshToken;
    if (!refreshToken) {
      return throwError(() => new Error('Нет refresh-токена'));
    }
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http
        .post<AuthResponse>(`${this.url}/refresh`, { refreshToken }, { context: silent() })
        .pipe(
          tap((r) => this.storeAuth(r)),
          finalize(() => (this.refreshInFlight$ = null)),
          shareReplay(1),
        );
    }
    return this.refreshInFlight$;
  }

  loadMe(): Observable<UserDto> {
    return this.http.get<UserDto>(`${this.url}/me`).pipe(tap((u) => this.setUser(u)));
  }

  logout(redirect = true): void {
    const refreshToken = this.refreshToken;
    const done = () => {
      this.clear();
      if (redirect) this.router.navigate(['/login']);
    };
    if (refreshToken && this.accessToken) {
      this.http
        .post<void>(`${this.url}/logout`, { refreshToken }, { context: silent() })
        .pipe(catchError(() => of(null)))
        .subscribe(() => done());
    } else {
      done();
    }
  }

  /** Локальный выход без обращения к серверу (например, после неудачного refresh). */
  clear(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this._currentUser.set(null);
    this.cart.reset();
  }

  setUser(user: UserDto): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this._currentUser.set(user);
  }

  private storeAuth(r: AuthResponse): void {
    localStorage.setItem(ACCESS_KEY, r.accessToken);
    localStorage.setItem(REFRESH_KEY, r.refreshToken);
    this.setUser(r.user);
  }
}

function readJson<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : null;
  } catch {
    return null;
  }
}
