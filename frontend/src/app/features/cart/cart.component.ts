import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartItemDto } from '../../core/models/api.models';
import { CartService } from '../../core/services/cart.service';
import { NotificationService } from '../../core/services/notification.service';
import { ProductImageComponent } from '../../shared/components/product-image.component';
import { BynPipe } from '../../shared/pipes/byn.pipe';

@Component({
  selector: 'app-cart',
  imports: [RouterLink, BynPipe, ProductImageComponent],
  template: `
    <div class="container">
      <div class="page-head">
        <h1>Корзина</h1>
        @if (cart().items.length) {
          <button type="button" class="btn btn-ghost btn-sm" (click)="clear()" [disabled]="busy()">Очистить корзину</button>
        }
      </div>

      @if (loading()) {
        <div class="card skeleton-card"></div>
      } @else if (!cart().items.length) {
        <div class="empty-state">
          <h3>Корзина пуста</h3>
          <p class="muted">Добавьте товары из каталога, чтобы оформить заказ.</p>
          <a routerLink="/catalog" class="btn btn-primary">Перейти в каталог</a>
        </div>
      } @else {
        <div class="cart-layout">
          <div class="card cart-list">
            @for (item of cart().items; track item.productId) {
              <div class="cart-item" [class.unavailable]="!item.available">
                <a [routerLink]="['/products', item.productId]" class="cart-thumb">
                  <app-product-image [src]="item.imageUrl" [alt]="item.name" />
                </a>
                <div class="cart-info">
                  <a [routerLink]="['/products', item.productId]" class="strong">{{ item.name }}</a>
                  <div class="muted small mono">{{ item.sku }}</div>
                  <div class="small">{{ item.price | byn }} за шт.</div>
                  @if (!item.available) {
                    <div class="field-error">
                      Товар недоступен в таком количестве (на складе: {{ item.stockQuantity }} шт.)
                    </div>
                  }
                </div>
                <div class="qty">
                  <button type="button" (click)="setQty(item, item.quantity - 1)" [disabled]="busy() || item.quantity <= 1">−</button>
                  <input
                    type="number"
                    min="1"
                    [value]="item.quantity"
                    (change)="setQty(item, $any($event.target).valueAsNumber)"
                    aria-label="Количество"
                  />
                  <button type="button" (click)="setQty(item, item.quantity + 1)" [disabled]="busy() || item.quantity >= item.stockQuantity">+</button>
                </div>
                <div class="cart-line-total">{{ item.lineTotal | byn }}</div>
                <button type="button" class="icon-btn danger" aria-label="Удалить" (click)="remove(item)" [disabled]="busy()">×</button>
              </div>
            }
          </div>

          <aside class="card cart-summary">
            <h3>Итого</h3>
            <div class="summary-row"><span>Товаров</span><span>{{ cart().totalQuantity }} шт.</span></div>
            <div class="summary-row total"><span>Сумма</span><span>{{ cart().totalAmount | byn }}</span></div>
            @if (hasUnavailable()) {
              <div class="alert alert-warning small">
                В корзине есть недоступные товары. Уменьшите количество или удалите их, чтобы оформить заказ.
              </div>
            }
            <button type="button" class="btn btn-primary btn-block" [disabled]="hasUnavailable()" (click)="checkout()">
              Оформить заказ
            </button>
            <a routerLink="/catalog" class="btn btn-ghost btn-block">Продолжить покупки</a>
          </aside>
        </div>
      }
    </div>
  `,
})
export class CartComponent {
  private readonly cartService = inject(CartService);
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly cart = this.cartService.cart;
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly hasUnavailable = computed(() => this.cart().items.some((i) => !i.available));

  constructor() {
    this.cartService.load().subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false),
    });
  }

  setQty(item: CartItemDto, quantity: number): void {
    if (!Number.isFinite(quantity) || quantity < 1) {
      quantity = 1;
    }
    quantity = Math.floor(quantity);
    if (quantity === item.quantity) return;
    this.busy.set(true);
    this.cartService.setQuantity(item.productId, quantity).subscribe({
      next: () => this.busy.set(false),
      // при ошибке (например, 409 — нехватка на складе) перечитываем корзину
      error: () => this.cartService.load().subscribe({ complete: () => this.busy.set(false), error: () => this.busy.set(false) }),
    });
  }

  remove(item: CartItemDto): void {
    this.busy.set(true);
    this.cartService.remove(item.productId).subscribe({
      next: () => this.busy.set(false),
      error: () => this.busy.set(false),
    });
  }

  clear(): void {
    if (!confirm('Очистить корзину?')) return;
    this.busy.set(true);
    this.cartService.clear().subscribe({
      next: () => {
        this.busy.set(false);
        this.notify.info('Корзина очищена');
      },
      error: () => this.busy.set(false),
    });
  }

  checkout(): void {
    this.router.navigate(['/checkout']);
  }
}
