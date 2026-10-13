import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OrderDto } from '../../core/models/api.models';
import { NotificationService } from '../../core/services/notification.service';
import { OrderService } from '../../core/services/order.service';
import { OrderViewComponent } from './order-view.component';

@Component({
  selector: 'app-order-details',
  imports: [RouterLink, OrderViewComponent],
  template: `
    <div class="container">
      <nav class="breadcrumbs"><a routerLink="/orders">Мои заказы</a><span>/</span><span class="muted">Заказ</span></nav>
      @if (loading()) {
        <div class="card skeleton-card"></div>
      } @else if (order(); as o) {
        <div class="page-head">
          <h1>Заказ {{ o.number }}</h1>
          @if (o.status === 'NEW') {
            <button type="button" class="btn btn-danger" [disabled]="busy()" (click)="cancel(o)">Отменить заказ</button>
          }
        </div>
        <app-order-view [order]="o" />
      } @else {
        <div class="empty-state">
          <h3>Заказ не найден</h3>
          <a routerLink="/orders" class="btn btn-primary">К списку заказов</a>
        </div>
      }
    </div>
  `,
})
export class OrderDetailsComponent {
  readonly id = input.required<string>();
  private readonly orders = inject(OrderService);
  private readonly notify = inject(NotificationService);

  protected readonly order = signal<OrderDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.loading.set(true);
      this.orders.get(id).subscribe({
        next: (o) => {
          this.order.set(o);
          this.loading.set(false);
        },
        error: () => {
          this.order.set(null);
          this.loading.set(false);
        },
      });
    });
  }

  cancel(o: OrderDto): void {
    if (!confirm(`Отменить заказ ${o.number}?`)) return;
    this.busy.set(true);
    this.orders.cancel(o.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.busy.set(false);
        this.notify.success('Заказ отменён');
      },
      error: () => this.busy.set(false),
    });
  }
}
