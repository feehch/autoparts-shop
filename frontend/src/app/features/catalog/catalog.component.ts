import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router, RouterLink } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import {
  BrandDto,
  CarMakeDto,
  CarModelDto,
  CategoryDto,
  CategoryNode,
  Page,
  ProductSearchParams,
  ProductSummaryDto,
} from '../../core/models/api.models';
import { CatalogService, buildCategoryTree } from '../../core/services/catalog.service';
import { ProductService } from '../../core/services/product.service';
import { PaginationComponent } from '../../shared/components/pagination.component';
import { ProductCardComponent } from './product-card.component';

export const SORT_OPTIONS = [
  { value: 'name,asc', label: 'По названию (А–Я)' },
  { value: 'name,desc', label: 'По названию (Я–А)' },
  { value: 'price,asc', label: 'Сначала дешёвые' },
  { value: 'price,desc', label: 'Сначала дорогие' },
  { value: 'createdAt,desc', label: 'Сначала новые' },
];

const PAGE_SIZE = 12;

function num(v: string | null): number | null {
  if (v === null || v === '') return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

@Component({
  selector: 'app-catalog',
  imports: [FormsModule, RouterLink, NgTemplateOutlet, PaginationComponent, ProductCardComponent],
  templateUrl: './catalog.component.html',
})
export class CatalogComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalog = inject(CatalogService);
  private readonly products = inject(ProductService);

  protected readonly sortOptions = SORT_OPTIONS;

  /** Параметры поиска берутся из query-параметров URL. */
  protected readonly params = toSignal(
    this.route.queryParamMap.pipe(
      map(
        (q): ProductSearchParams => ({
          q: q.get('q'),
          categoryId: num(q.get('categoryId')),
          brandId: num(q.get('brandId')),
          makeId: num(q.get('makeId')),
          modelId: num(q.get('modelId')),
          minPrice: num(q.get('minPrice')),
          maxPrice: num(q.get('maxPrice')),
          inStock: q.get('inStock') === 'true',
          sort: q.get('sort') || 'name,asc',
          page: num(q.get('page')) ?? 0,
          size: PAGE_SIZE,
        }),
      ),
    ),
    { requireSync: true },
  );

  protected readonly loading = signal(true);
  protected readonly result = signal<Page<ProductSummaryDto> | null>(null);
  protected readonly filtersOpen = signal(false);

  protected readonly categories = toSignal(this.catalog.getCategories().pipe(catchError(() => of([] as CategoryDto[]))), {
    initialValue: [] as CategoryDto[],
  });
  protected readonly brands = toSignal(this.catalog.getBrands().pipe(catchError(() => of([] as BrandDto[]))), {
    initialValue: [] as BrandDto[],
  });
  protected readonly makes = toSignal(this.catalog.getMakes().pipe(catchError(() => of([] as CarMakeDto[]))), {
    initialValue: [] as CarMakeDto[],
  });
  protected readonly models = signal<CarModelDto[]>([]);

  protected readonly tree = computed(() => buildCategoryTree(this.categories()));
  protected readonly expanded = signal<Set<number>>(new Set());

  protected readonly selectedCategory = computed(() =>
    this.categories().find((c) => c.id === this.params().categoryId),
  );
  protected readonly selectedBrand = computed(() => this.brands().find((b) => b.id === this.params().brandId));
  protected readonly selectedMake = computed(() => this.makes().find((m) => m.id === this.params().makeId));
  protected readonly selectedModel = computed(() => this.models().find((m) => m.id === this.params().modelId));

  protected readonly hasFilters = computed(() => {
    const p = this.params();
    return !!(p.q || p.categoryId || p.brandId || p.makeId || p.modelId || p.minPrice != null || p.maxPrice != null || p.inStock);
  });

  // Локальные значения полей цены (применяются кнопкой)
  protected minPrice: number | null = null;
  protected maxPrice: number | null = null;

  constructor() {
    // Загрузка товаров при изменении параметров
    toObservable(this.params)
      .pipe(
        tap(() => this.loading.set(true)),
        switchMap((p) => this.products.search(p).pipe(catchError(() => of(null)))),
        takeUntilDestroyed(),
      )
      .subscribe((page) => {
        this.result.set(page);
        this.loading.set(false);
      });

    // Модели выбранной марки
    toObservable(computed(() => this.params().makeId))
      .pipe(
        switchMap((makeId) =>
          makeId ? this.catalog.getModelsByMake(makeId).pipe(catchError(() => of([]))) : of([]),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((list) => this.models.set(list));

    // Синхронизация полей цены и раскрытие ветки выбранной категории
    effect(() => {
      const p = this.params();
      this.minPrice = p.minPrice ?? null;
      this.maxPrice = p.maxPrice ?? null;
    });
    effect(() => {
      const id = this.params().categoryId;
      const cats = this.categories();
      if (!id || !cats.length) return;
      const byId = new Map(cats.map((c) => [c.id, c]));
      // untracked: иначе эффект подписывается на expanded и бесконечно перезапускает сам себя
      const next = new Set(untracked(this.expanded));
      let cur = byId.get(id);
      while (cur) {
        next.add(cur.id);
        cur = cur.parentId != null ? byId.get(cur.parentId) : undefined;
      }
      this.expanded.set(next);
    });
  }

  /** Обновляет query-параметры; при смене фильтра страница сбрасывается. */
  protected update(patch: Params, resetPage = true): void {
    const queryParams: Params = { ...patch };
    if (resetPage) queryParams['page'] = null;
    for (const k of Object.keys(queryParams)) {
      if (queryParams[k] === '' || queryParams[k] === false || queryParams[k] === undefined) queryParams[k] = null;
    }
    this.router.navigate([], { relativeTo: this.route, queryParams, queryParamsHandling: 'merge' });
  }

  protected selectCategory(node: CategoryNode | null): void {
    this.update({ categoryId: node?.id ?? null });
    this.filtersOpen.set(false);
  }

  protected toggleNode(id: number, e: Event): void {
    e.stopPropagation();
    const next = new Set(this.expanded());
    if (next.has(id)) next.delete(id);
    else next.add(id);
    this.expanded.set(next);
  }

  protected onMakeChange(value: string): void {
    this.update({ makeId: value || null, modelId: null });
  }

  protected applyPrice(): void {
    this.update({ minPrice: this.minPrice ?? null, maxPrice: this.maxPrice ?? null });
  }

  protected resetAll(): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  protected goToPage(page: number): void {
    this.update({ page: page || null }, false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
