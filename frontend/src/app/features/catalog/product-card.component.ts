import { Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductSummaryDto } from '../../core/models/api.models';
import { ProductImageComponent } from '../../shared/components/product-image.component';
import { BynPipe } from '../../shared/pipes/byn.pipe';
import { AddToCartService } from './add-to-cart.service';
import { StockBadgeComponent } from './stock-badge.component';

@Component({
  selector: 'app-product-card',
  imports: [RouterLink, BynPipe, ProductImageComponent, StockBadgeComponent],
  template: `
    @let p = product();
    <article class="card product-card" [class.inactive]="!p.active">
      <a [routerLink]="['/products', p.id]" class="product-card-img">
        <app-product-image [src]="p.imageUrl" [alt]="p.name" />
      </a>
      <div class="product-card-body">
        <div class="product-meta">
          <span class="brand">{{ p.brandName }}</span>
          <span class="sku">{{ p.sku }}</span>
        </div>
        <a [routerLink]="['/products', p.id]" class="product-name">{{ p.name }}</a>
        <div class="muted small">{{ p.categoryName }}</div>
        <div class="product-card-foot">
          <div class="product-card-price">
            <div class="price">{{ p.price | byn }}</div>
            <app-stock-badge [quantity]="p.stockQuantity" [active]="p.active" />
          </div>
          <button
            type="button"
            class="btn btn-primary btn-sm btn-block"
            [disabled]="busy() || p.stockQuantity < 1 || !p.active"
            (click)="add()"
          >
            В корзину
          </button>
        </div>
      </div>
    </article>
  `,
})
export class ProductCardComponent {
  readonly product = input.required<ProductSummaryDto>();
  private readonly addToCart = inject(AddToCartService);
  protected readonly busy = signal(false);

  add(): void {
    const p = this.product();
    this.busy.set(true);
    this.addToCart.add(p.id, p.name, 1, () => this.busy.set(false));
  }
}
