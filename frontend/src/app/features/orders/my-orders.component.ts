import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OrderDto, Page } from '../../core/models/api.models';
import { OrderService } from '../../core/services/order.service';
import { PaginationComponent } from '../../shared/components/pagination.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { BynPipe } from '../../shared/pipes/byn.pipe';

@Component({
  selector: 'app-my-orders',
  imports: [RouterLink, DatePipe, BynPipe, StatusBadgeComponent, PaginationComponent],
  template: `
    <div class="container">
      <h1>Мои заказы</h1>
      @if (loading() && !page()) {
        <div class="card skeleton-card"></div>
      } @else if (page(); as p) {
        @if (p.content.length) {
          <div class="card table-wrap">
            <table class="table table-hover">
              <thead>
                <tr><th>Номер</th><th>Дата</th><th>Статус</th><th class="right">Товаров</th><th class="right">Сумма</th></tr>
              </thead>
              <tbody>
                @for (o of p.content; track o.id) {
                  <tr class="clickable" [routerLink]="['/orders', o.id]">
                    <td><a [routerLink]="['/orders', o.id]" class="strong">{{ o.number }}</a></td>
                    <td>{{ o.createdAt | date: 'dd.MM.yyyy HH:mm' }}</td>
                    <td><app-status-badge [status]="o.status" /></td>
                    <td class="right">{{ count(o) }}</td>
                    <td class="right strong">{{ o.totalAmount | byn }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-pagination [page]="p.page" [totalPages]="p.totalPages" (pageChange)="load($event)" />
        } @else {
          <div class="empty-state">
            <h3>Заказов пока нет</h3>
            <a routerLink="/catalog" class="btn btn-primary">Перейти в каталог</a>
          </div>
        }
      }
    </div>
  `,
})
export class MyOrdersComponent {
  private readonly orders = inject(OrderService);
  protected readonly page = signal<Page<OrderDto> | null>(null);
  protected readonly loading = signal(false);

  constructor() {
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.orders.my(page, 10).subscribe({
      next: (p) => {
        this.page.set(p);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  count(o: OrderDto): number {
    return o.items.reduce((s, i) => s + i.quantity, 0);
  }
}
