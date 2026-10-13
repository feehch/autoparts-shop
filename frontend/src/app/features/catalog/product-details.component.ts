import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProductDto } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { ProductService } from '../../core/services/product.service';
import { ProductImageComponent } from '../../shared/components/product-image.component';
import { BynPipe } from '../../shared/pipes/byn.pipe';
import { YearsPipe } from '../../shared/pipes/years.pipe';
import { AddToCartService } from './add-to-cart.service';
import { StockBadgeComponent } from './stock-badge.component';

@Component({
  selector: 'app-product-details',
  imports: [RouterLink, FormsModule, DatePipe, BynPipe, YearsPipe, ProductImageComponent, StockBadgeComponent],
  template: `
    <div class="container">
      <nav class="breadcrumbs">
        <a routerLink="/catalog">Каталог</a>
        @if (product(); as p) {
          <span>/</span>
          <a routerLink="/catalog" [queryParams]="{ categoryId: p.categoryId }">{{ p.categoryName }}</a>
          <span>/</span>
          <span class="muted">{{ p.name }}</span>
        }
      </nav>

      @if (loading()) {
        <div class="card product-details skeleton-card tall"></div>
      } @else if (product(); as p) {
        <div class="card product-details">
          <div class="pd-image">
            <app-product-image [src]="p.imageUrl" [alt]="p.name" />
          </div>
          <div class="pd-info">
            @if (!p.active) {
              <div class="alert alert-warning">Товар снят с продажи (виден только менеджерам)</div>
            }
            <h1>{{ p.name }}</h1>
            <dl class="props">
              <dt>Производитель</dt><dd>{{ p.brandName }}</dd>
              <dt>Артикул</dt><dd class="mono">{{ p.sku }}</dd>
              @if (p.oemNumber) {
                <dt>OEM-номер</dt><dd class="mono">{{ p.oemNumber }}</dd>
              }
              <dt>Категория</dt><dd>{{ p.categoryName }}</dd>
            </dl>

            <div class="pd-buy">
              <div class="price big">{{ p.price | byn }}</div>
              <app-stock-badge [quantity]="p.stockQuantity" [active]="p.active" />
              @if (canBuy()) {
                <div class="buy-row">
                  <div class="qty">
                    <button type="button" (click)="qty.set(qty() - 1)" [disabled]="qty() <= 1">−</button>
                    <input
                      type="number"
                      min="1"
                      [max]="p.stockQuantity"
                      [ngModel]="qty()"
                      (ngModelChange)="setQty($event)"
                      aria-label="Количество"
                    />
                    <button type="button" (click)="qty.set(qty() + 1)" [disabled]="qty() >= p.stockQuantity">+</button>
                  </div>
                  <button type="button" class="btn btn-primary" [disabled]="busy()" (click)="add(p)">
                    В корзину
                  </button>
                </div>
              }
              @if (auth.isManager()) {
                <a [routerLink]="['/admin/products', p.id]" class="btn btn-ghost btn-sm">Редактировать</a>
              }
            </div>
          </div>
        </div>

        @if (p.description) {
          <section class="card section">
            <h2>Описание</h2>
            <p class="pre-line">{{ p.description }}</p>
          </section>
        }

        <section class="card section">
          <h2>Применимость</h2>
          @if (p.compatibleModels.length) {
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr><th>Марка</th><th>Модель</th><th>Годы выпуска</th><th></th></tr>
                </thead>
                <tbody>
                  @for (m of p.compatibleModels; track m.id) {
                    <tr>
                      <td>{{ m.makeName }}</td>
                      <td>{{ m.name }}</td>
                      <td>{{ m | years }}</td>
                      <td class="right">
                        <a routerLink="/catalog" [queryParams]="{ makeId: m.makeId, modelId: m.id }" class="small nowrap">
                          Подобрать запчасти →
                        </a>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <p class="muted">Универсальный товар или данные о совместимости отсутствуют.</p>
          }
          <p class="muted small">Обновлено: {{ p.updatedAt | date: 'dd.MM.yyyy HH:mm' }}</p>
        </section>
      } @else {
        <div class="empty-state">
          <h3>Товар не найден</h3>
          <a routerLink="/catalog" class="btn btn-primary">Вернуться в каталог</a>
        </div>
      }
    </div>
  `,
})
export class ProductDetailsComponent {
  /** Параметр маршрута :id (withComponentInputBinding). */
  readonly id = input.required<string>();

  protected readonly auth = inject(AuthService);
  private readonly products = inject(ProductService);
  private readonly addToCart = inject(AddToCartService);

  protected readonly product = signal<ProductDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly qty = signal(1);
  protected readonly canBuy = computed(() => {
    const p = this.product();
    return !!p && p.active && p.stockQuantity > 0;
  });

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.loading.set(true);
      this.qty.set(1);
      this.products.get(id).subscribe({
        next: (p) => {
          this.product.set(p);
          this.loading.set(false);
        },
        error: () => {
          this.product.set(null);
          this.loading.set(false);
        },
      });
    });
  }

  setQty(v: number): void {
    const max = this.product()?.stockQuantity ?? 1;
    this.qty.set(Math.max(1, Math.min(max, Math.floor(Number(v) || 1))));
  }

  add(p: ProductDto): void {
    this.busy.set(true);
    this.addToCart.add(p.id, p.name, this.qty(), () => this.busy.set(false));
  }
}
