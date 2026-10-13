import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page, ProductDto, ProductRequest, ProductSearchParams, ProductSummaryDto } from '../models/api.models';
import { toHttpParams } from './http-params.util';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/products`;

  search(params: ProductSearchParams): Observable<Page<ProductSummaryDto>> {
    return this.http.get<Page<ProductSummaryDto>>(this.url, { params: toHttpParams(params) });
  }
  get(id: number): Observable<ProductDto> {
    return this.http.get<ProductDto>(`${this.url}/${id}`);
  }
  create(req: ProductRequest): Observable<ProductDto> {
    return this.http.post<ProductDto>(this.url, req);
  }
  update(id: number, req: ProductRequest): Observable<ProductDto> {
    return this.http.put<ProductDto>(`${this.url}/${id}`, req);
  }
  updateStock(id: number, stockQuantity: number): Observable<ProductDto> {
    return this.http.patch<ProductDto>(`${this.url}/${id}/stock`, { stockQuantity });
  }
  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
