import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CartDto } from '../models/api.models';

const EMPTY_CART: CartDto = { items: [], totalQuantity: 0, totalAmount: 0 };

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/cart`;

  private readonly _cart = signal<CartDto>(EMPTY_CART);
  readonly cart = this._cart.asReadonly();
  readonly count = computed(() => this._cart().totalQuantity);

  load(): Observable<CartDto> {
    return this.http.get<CartDto>(this.url).pipe(tap((c) => this._cart.set(c)));
  }

  /** Тихое обновление счётчика (ошибки игнорируются). */
  refresh(): void {
    this.load().subscribe({ error: () => void 0 });
  }

  add(productId: number, quantity = 1): Observable<CartDto> {
    return this.http
      .post<CartDto>(`${this.url}/items`, { productId, quantity })
      .pipe(tap((c) => this._cart.set(c)));
  }

  setQuantity(productId: number, quantity: number): Observable<CartDto> {
    return this.http
      .put<CartDto>(`${this.url}/items/${productId}`, { quantity })
      .pipe(tap((c) => this._cart.set(c)));
  }

  remove(productId: number): Observable<CartDto> {
    return this.http.delete<CartDto>(`${this.url}/items/${productId}`).pipe(tap((c) => this._cart.set(c)));
  }

  clear(): Observable<void> {
    return this.http.delete<void>(this.url).pipe(tap(() => this._cart.set(EMPTY_CART)));
  }

  /** Сброс локального состояния (выход, оформление заказа). */
  reset(): void {
    this._cart.set(EMPTY_CART);
  }
}
