import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Page, ProductSummaryDto } from '../../../core/models/api.models';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductService } from '../../../core/services/product.service';
import { PaginationComponent } from '../../../shared/components/pagination.component';
import { BynPipe } from '../../../shared/pipes/byn.pipe';

@Component({
  selector: 'app-admin-products',
  imports: [FormsModule, RouterLink, BynPipe, PaginationComponent],
  template: `
    <div class="page-head">
      <h1>Товары</h1>
      <a routerLink="/admin/products/new" class="btn btn-primary">+ Новый товар</a>
    </div>

    <form class="card toolbar-card" (ngSubmit)="load(0)">
      <input type="search" name="q" [(ngModel)]="q" placeholder="Название, артикул или OEM-номер" class="grow" />
      <label class="checkbox">
        <input type="checkbox" name="includeInactive" [(ngModel)]="includeInactive" (ngModelChange)="load(0)" />
        <span>Показать неактивные</span>
      </label>
      <button type="submit" class="btn btn-ghost">Найти</button>
    </form>

    @if (page(); as p) {
      <div class="card table-wrap" [class.dimmed]="loading()">
        <table class="table">
          <thead>
            <tr>
              <th>Артикул</th><th>Название</th><th>Бренд</th><th>Категория</th>
              <th class="right">Цена</th><th>Остаток</th><th>Статус</th><th></th>
            </tr>
          </thead>
          <tbody>
            @for (item of p.content; track item.id) {
              <tr [class.row-muted]="!item.active">
                <td class="mono small">{{ item.sku }}</td>
                <td><a [routerLink]="['/products', item.id]">{{ item.name }}</a></td>
                <td>{{ item.brandName }}</td>
                <td>{{ item.categoryName }}</td>
                <td class="right nowrap">{{ item.price | byn }}</td>
                <td>
                  @if (editingStock() === item.id) {
                    <form class="inline-form" (ngSubmit)="saveStock(item)">
                      <input type="number" min="0" name="stock" [(ngModel)]="stockValue" class="input-sm" />
                      <button type="submit" class="btn btn-primary btn-xs">✓</button>
                      <button type="button" class="btn btn-ghost btn-xs" (click)="editingStock.set(null)">✕</button>
                    </form>
                  } @else {
                    <button type="button" class="link stock-link" [class.low]="item.stockQuantity < 5" (click)="editStock(item)" title="Изменить остаток">
                      {{ item.stockQuantity }} шт. ✎
                    </button>
                  }
                </td>
                <td>
                  @if (item.active) {
                    <span class="badge badge-success">Активен</span>
                  } @else {
                    <span class="badge badge-muted">Неактивен</span>
                  }
                </td>
                <td class="right nowrap">
                  <a [routerLink]="['/admin/products', item.id]" class="btn btn-ghost btn-xs">Изменить</a>
                  @if (item.active) {
                    <button type="button" class="btn btn-danger-ghost btn-xs" (click)="deactivate(item)">Снять</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="8" class="muted center">Товары не найдены</td></tr>
            }
          </tbody>
        </table>
      </div>
      <div class="table-foot">
        <span class="muted small">Всего: {{ p.totalElements }}</span>
        <app-pagination [page]="p.page" [totalPages]="p.totalPages" (pageChange)="load($event)" />
      </div>
    }
  `,
})
export class AdminProductsComponent {
  private readonly products = inject(ProductService);
  private readonly notify = inject(NotificationService);

  protected q = '';
  protected includeInactive = true;
  protected stockValue = 0;
  protected readonly page = signal<Page<ProductSummaryDto> | null>(null);
  protected readonly loading = signal(false);
  protected readonly editingStock = signal<number | null>(null);

  constructor() {
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.products
      .search({ q: this.q.trim() || null, includeInactive: this.includeInactive, page, size: 20, sort: 'name,asc' })
      .subscribe({
        next: (p) => {
          this.page.set(p);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  editStock(item: ProductSummaryDto): void {
    this.stockValue = item.stockQuantity;
    this.editingStock.set(item.id);
  }

  saveStock(item: ProductSummaryDto): void {
    const value = Math.floor(Number(this.stockValue));
    if (!Number.isFinite(value) || value < 0) {
      this.notify.error('Остаток должен быть неотрицательным числом');
      return;
    }
    this.products.updateStock(item.id, value).subscribe((updated) => {
      this.editingStock.set(null);
      this.replace(item.id, { stockQuantity: updated.stockQuantity });
      this.notify.success('Остаток обновлён');
    });
  }

  deactivate(item: ProductSummaryDto): void {
    if (!confirm(`Снять с продажи «${item.name}»?`)) return;
    this.products.deactivate(item.id).subscribe(() => {
      this.notify.success('Товар снят с продажи');
      if (this.includeInactive) this.replace(item.id, { active: false });
      else this.load(this.page()?.page ?? 0);
    });
  }

  private replace(id: number, patch: Partial<ProductSummaryDto>): void {
    this.page.update((p) => (p ? { ...p, content: p.content.map((x) => (x.id === id ? { ...x, ...patch } : x)) } : p));
  }
}
