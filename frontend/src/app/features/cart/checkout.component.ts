import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PAYMENT_METHODS, PaymentMethod } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { NotificationService } from '../../core/services/notification.service';
import { OrderService } from '../../core/services/order.service';
import { FieldErrorComponent } from '../../shared/components/field-error.component';
import { PaymentMethodPipe } from '../../shared/pipes/labels.pipes';
import { BynPipe } from '../../shared/pipes/byn.pipe';
import { applyServerErrors } from '../../shared/utils/form-errors';
import { formatBelarusPhone, phoneOrNull, requiredBelarusPhone } from '../../shared/utils/phone';
import { PhoneMaskDirective } from '../../shared/directives/phone-mask.directive';

@Component({
  selector: 'app-checkout',
  imports: [ReactiveFormsModule, RouterLink, FieldErrorComponent, PaymentMethodPipe, BynPipe, PhoneMaskDirective],
  template: `
    <div class="container">
      <h1>Оформление заказа</h1>
      @if (!loading() && !cart().items.length) {
        <div class="empty-state">
          <h3>Корзина пуста</h3>
          <a routerLink="/catalog" class="btn btn-primary">Перейти в каталог</a>
        </div>
      } @else {
        <div class="cart-layout">
          <form class="card form-card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <h3>Доставка</h3>
            <label class="field">
              <span>Адрес доставки</span>
              <textarea formControlName="deliveryAddress" rows="2" placeholder="г. Минск, пр. Независимости, д. 1, кв. 10"></textarea>
              <app-field-error [control]="form.controls.deliveryAddress" />
            </label>
            <label class="field">
              <span>Контактный телефон</span>
              <input type="tel" formControlName="contactPhone" appPhoneMask placeholder="+375 (29) 123-45-67" />
              <app-field-error [control]="form.controls.contactPhone" />
            </label>
            <label class="field">
              <span>Комментарий к заказу</span>
              <textarea formControlName="comment" rows="2" placeholder="Например: позвонить заранее"></textarea>
              <app-field-error [control]="form.controls.comment" />
            </label>

            <h3>Способ оплаты</h3>
            <div class="radio-group">
              @for (m of paymentMethods; track m) {
                <label class="radio-card" [class.selected]="form.controls.paymentMethod.value === m">
                  <input type="radio" formControlName="paymentMethod" [value]="m" />
                  <span>{{ m | paymentMethod }}</span>
                </label>
              }
            </div>

            <button type="submit" class="btn btn-primary btn-block" [disabled]="submitting()">
              {{ submitting() ? 'Оформляем…' : 'Подтвердить заказ' }}
            </button>
          </form>

          <aside class="card cart-summary">
            <h3>Ваш заказ</h3>
            @for (i of cart().items; track i.productId) {
              <div class="summary-row small">
                <span>{{ i.name }} × {{ i.quantity }}</span><span>{{ i.lineTotal | byn }}</span>
              </div>
            }
            <div class="summary-row total"><span>Итого</span><span>{{ cart().totalAmount | byn }}</span></div>
            <a routerLink="/cart" class="btn btn-ghost btn-block">Изменить корзину</a>
          </aside>
        </div>
      }
    </div>
  `,
})
export class CheckoutComponent {
  private readonly fb = inject(FormBuilder);
  private readonly cartService = inject(CartService);
  private readonly orders = inject(OrderService);
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  protected readonly cart = this.cartService.cart;
  protected readonly paymentMethods = PAYMENT_METHODS;
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    deliveryAddress: ['', [Validators.required, Validators.maxLength(500)]],
    contactPhone: [formatBelarusPhone(this.auth.currentUser()?.phone) === '+375' ? '' : formatBelarusPhone(this.auth.currentUser()?.phone), [requiredBelarusPhone]],
    comment: ['', [Validators.maxLength(1000)]],
    paymentMethod: ['CASH_ON_DELIVERY' as PaymentMethod, [Validators.required]],
  });

  constructor() {
    this.cartService.load().subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false),
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.submitting.set(true);
    this.orders.place({ ...v, contactPhone: phoneOrNull(v.contactPhone) ?? '', comment: v.comment.trim() || null }).subscribe({
      next: (order) => {
        this.cartService.reset();
        this.notify.success(`Заказ ${order.number} оформлен`);
        this.router.navigate(['/orders', order.id]);
      },
      error: (err) => {
        this.submitting.set(false);
        applyServerErrors(this.form, err);
        this.cartService.refresh();
      },
    });
  }
}
