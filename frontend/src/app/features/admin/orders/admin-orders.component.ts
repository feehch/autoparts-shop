import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ORDER_STATUSES, OrderDto, OrderStatus, Page } from '../../../core/models/api.models';
import { OrderService } from '../../../core/services/order.service';
import { PaginationComponent } from '../../../shared/components/pagination.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge.component';
import { OrderStatusPipe } from '../../../shared/pipes/labels.pipes';
import { BynPipe } from '../../../shared/pipes/byn.pipe';

@Component({
  selector: 'app-admin-orders',
  imports: [FormsModule, RouterLink, DatePipe, BynPipe, OrderStatusPipe, StatusBadgeComponent, PaginationComponent],
  template: `
    <h1>Заказы</h1>
    <form class="card toolbar-card" (ngSubmit)="load(0)">
      <input type="search" name="q" [(ngModel)]="q" placeholder="Номер заказа или email клиента" class="grow" />
      <select name="status" [(ngModel)]="status" (ngModelChange)="load(0)">
        <option [ngValue]="null">Все статусы</option>
        @for (s of statuses; track s) {
          <option [ngValue]="s">{{ s | orderStatus }}</option>
        }
      </select>
      <button type="submit" class="btn btn-ghost">Найти</button>
    </form>

    @if (page(); as p) {
      <div class="card table-wrap" [class.dimmed]="loading()">
        <table class="table table-hover">
          <thead>
            <tr><th>Номер</th><th>Дата</th><th>Клиент</th><th>Статус</th><th class="right">Сумма</th><th></th></tr>
          </thead>
          <tbody>
            @for (o of p.content; track o.id) {
              <tr>
                <td><a [routerLink]="['/admin/orders', o.id]" class="strong">{{ o.number }}</a></td>
                <td class="nowrap">{{ o.createdAt | date: 'dd.MM.yyyy HH:mm' }}</td>
                <td>{{ o.customerName }}<br /><span class="muted small">{{ o.customerEmail }}</span></td>
                <td><app-status-badge [status]="o.status" /></td>
                <td class="right nowrap strong">{{ o.totalAmount | byn }}</td>
                <td class="right"><a [routerLink]="['/admin/orders', o.id]" class="btn btn-ghost btn-xs">Открыть</a></td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="muted center">Заказы не найдены</td></tr>
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
export class AdminOrdersComponent {
  private readonly orders = inject(OrderService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly statuses = ORDER_STATUSES;
  protected q = '';
  protected status: OrderStatus | null = null;
  protected readonly page = signal<Page<OrderDto> | null>(null);
  protected readonly loading = signal(false);

  constructor() {
    const s = this.route.snapshot.queryParamMap.get('status');
    if (s && (ORDER_STATUSES as string[]).includes(s)) this.status = s as OrderStatus;
    this.q = this.route.snapshot.queryParamMap.get('q') ?? '';
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { status: this.status, q: this.q.trim() || null },
      replaceUrl: true,
    });
    this.orders.all({ status: this.status, q: this.q.trim() || null, page, size: 20 }).subscribe({
      next: (p) => {
        this.page.set(p);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
