import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ORDER_TRANSITIONS, OrderDto, OrderStatus } from '../../../core/models/api.models';
import { NotificationService } from '../../../core/services/notification.service';
import { OrderService } from '../../../core/services/order.service';
import { OrderStatusPipe } from '../../../shared/pipes/labels.pipes';
import { OrderViewComponent } from '../../orders/order-view.component';

@Component({
  selector: 'app-admin-order-details',
  imports: [RouterLink, OrderViewComponent, OrderStatusPipe],
  template: `
    <nav class="breadcrumbs"><a routerLink="/admin/orders">Заказы</a><span>/</span><span class="muted">Заказ</span></nav>
    @if (order(); as o) {
      <div class="page-head">
        <h1>Заказ {{ o.number }}</h1>
        <div class="btn-row">
          @for (s of nextStatuses(); track s) {
            <button
              type="button"
              class="btn"
              [class.btn-primary]="s !== 'CANCELLED'"
              [class.btn-danger]="s === 'CANCELLED'"
              [disabled]="busy()"
              (click)="change(o, s)"
            >
              {{ actionLabel(s) }}
            </button>
          } @empty {
            <span class="muted small">Статус «{{ o.status | orderStatus }}» — окончательный</span>
          }
        </div>
      </div>
      <app-order-view [order]="o" />
    } @else if (!loading()) {
      <div class="empty-state"><h3>Заказ не найден</h3></div>
    }
  `,
})
export class AdminOrderDetailsComponent {
  readonly id = input.required<string>();
  private readonly orders = inject(OrderService);
  private readonly notify = inject(NotificationService);
  private readonly statusPipe = new OrderStatusPipe();

  protected readonly order = signal<OrderDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly nextStatuses = computed(() => {
    const o = this.order();
    return o ? ORDER_TRANSITIONS[o.status] : [];
  });

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.loading.set(true);
      this.orders.get(id).subscribe({
        next: (o) => {
          this.order.set(o);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
    });
  }

  actionLabel(s: OrderStatus): string {
    switch (s) {
      case 'CONFIRMED':
        return 'Подтвердить';
      case 'SHIPPED':
        return 'Отправить';
      case 'DELIVERED':
        return 'Отметить доставленным';
      case 'CANCELLED':
        return 'Отменить';
      default:
        return this.statusPipe.transform(s);
    }
  }

  change(o: OrderDto, s: OrderStatus): void {
    if (!confirm(`Изменить статус заказа ${o.number} на «${this.statusPipe.transform(s)}»?`)) return;
    this.busy.set(true);
    this.orders.changeStatus(o.id, s).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.busy.set(false);
        this.notify.success(`Статус изменён: ${this.statusPipe.transform(updated.status)}`);
      },
      error: () => this.busy.set(false),
    });
  }
}
