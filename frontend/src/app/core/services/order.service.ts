import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OrderDto, OrderStatus, Page, PlaceOrderRequest } from '../models/api.models';
import { toHttpParams } from './http-params.util';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/orders`;

  place(req: PlaceOrderRequest): Observable<OrderDto> {
    return this.http.post<OrderDto>(this.url, req);
  }
  my(page = 0, size = 10): Observable<Page<OrderDto>> {
    return this.http.get<Page<OrderDto>>(`${this.url}/my`, { params: toHttpParams({ page, size }) });
  }
  get(id: number): Observable<OrderDto> {
    return this.http.get<OrderDto>(`${this.url}/${id}`);
  }
  cancel(id: number): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${this.url}/${id}/cancel`, {});
  }
  all(params: { status?: OrderStatus | null; q?: string | null; page?: number; size?: number }): Observable<Page<OrderDto>> {
    return this.http.get<Page<OrderDto>>(this.url, { params: toHttpParams(params) });
  }
  changeStatus(id: number, status: OrderStatus): Observable<OrderDto> {
    return this.http.patch<OrderDto>(`${this.url}/${id}/status`, { status });
  }
}
