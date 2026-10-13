import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BrandDto,
  BrandRequest,
  CarMakeDto,
  CarMakeRequest,
  CarModelDto,
  CarModelRequest,
  CategoryDto,
  CategoryNode,
  CategoryRequest,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  // Категории
  getCategories(): Observable<CategoryDto[]> {
    return this.http.get<CategoryDto[]>(`${this.api}/categories`);
  }
  createCategory(req: CategoryRequest): Observable<CategoryDto> {
    return this.http.post<CategoryDto>(`${this.api}/categories`, req);
  }
  updateCategory(id: number, req: CategoryRequest): Observable<CategoryDto> {
    return this.http.put<CategoryDto>(`${this.api}/categories/${id}`, req);
  }
  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/categories/${id}`);
  }

  // Бренды
  getBrands(): Observable<BrandDto[]> {
    return this.http.get<BrandDto[]>(`${this.api}/brands`);
  }
  createBrand(req: BrandRequest): Observable<BrandDto> {
    return this.http.post<BrandDto>(`${this.api}/brands`, req);
  }
  updateBrand(id: number, req: BrandRequest): Observable<BrandDto> {
    return this.http.put<BrandDto>(`${this.api}/brands/${id}`, req);
  }
  deleteBrand(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/brands/${id}`);
  }

  // Автомобили
  getMakes(): Observable<CarMakeDto[]> {
    return this.http.get<CarMakeDto[]>(`${this.api}/cars/makes`);
  }
  createMake(req: CarMakeRequest): Observable<CarMakeDto> {
    return this.http.post<CarMakeDto>(`${this.api}/cars/makes`, req);
  }
  updateMake(id: number, req: CarMakeRequest): Observable<CarMakeDto> {
    return this.http.put<CarMakeDto>(`${this.api}/cars/makes/${id}`, req);
  }
  deleteMake(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/cars/makes/${id}`);
  }
  getModelsByMake(makeId: number): Observable<CarModelDto[]> {
    return this.http.get<CarModelDto[]>(`${this.api}/cars/makes/${makeId}/models`);
  }
  getAllModels(): Observable<CarModelDto[]> {
    return this.http.get<CarModelDto[]>(`${this.api}/cars/models`);
  }
  createModel(req: CarModelRequest): Observable<CarModelDto> {
    return this.http.post<CarModelDto>(`${this.api}/cars/models`, req);
  }
  updateModel(id: number, req: CarModelRequest): Observable<CarModelDto> {
    return this.http.put<CarModelDto>(`${this.api}/cars/models/${id}`, req);
  }
  deleteModel(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/cars/models/${id}`);
  }
}

/** Строит дерево категорий из плоского списка (по parentId) и сортирует по имени. */
export function buildCategoryTree(list: CategoryDto[]): CategoryNode[] {
  const nodes = new Map<number, CategoryNode>();
  list.forEach((c) => nodes.set(c.id, { ...c, children: [], depth: 0 }));
  const roots: CategoryNode[] = [];
  nodes.forEach((n) => {
    const parent = n.parentId != null ? nodes.get(n.parentId) : undefined;
    if (parent) parent.children.push(n);
    else roots.push(n);
  });
  const sortRec = (arr: CategoryNode[], depth: number) => {
    arr.sort((a, b) => a.name.localeCompare(b.name, 'ru'));
    arr.forEach((n) => {
      n.depth = depth;
      sortRec(n.children, depth + 1);
    });
  };
  sortRec(roots, 0);
  return roots;
}

/** Разворачивает дерево в плоский список (для select с отступами). */
export function flattenTree(tree: CategoryNode[]): CategoryNode[] {
  const out: CategoryNode[] = [];
  const walk = (arr: CategoryNode[]) =>
    arr.forEach((n) => {
      out.push(n);
      walk(n.children);
    });
  walk(tree);
  return out;
}

/** Опции для select категорий: имя с отступом по глубине. */
export function categoryOptions(list: CategoryDto[]): { id: number; label: string; depth: number }[] {
  return flattenTree(buildCategoryTree(list)).map((c) => ({
    id: c.id,
    depth: c.depth,
    label: `${'\u00A0\u00A0\u00A0\u00A0'.repeat(c.depth)}${c.depth ? '└ ' : ''}${c.name}`,
  }));
}
