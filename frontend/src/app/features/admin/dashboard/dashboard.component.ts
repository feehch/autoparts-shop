import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardStats, ORDER_STATUSES } from '../../../core/models/api.models';
import { StatsService } from '../../../core/services/stats.service';
import { StatusBadgeComponent } from '../../../shared/components/status-badge.component';
import { BynPipe } from '../../../shared/pipes/byn.pipe';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, BynPipe, StatusBadgeComponent],
  template: `
    <h1>Обзор</h1>
    @if (stats(); as s) {
      <div class="stat-grid">
        <a class="card stat" routerLink="/admin/products">
          <div class="stat-label">Товаров</div>
          <div class="stat-value">{{ s.productsCount }}</div>
          <div class="muted small">активных: {{ s.activeProductsCount }}</div>
        </a>
        <a class="card stat" routerLink="/admin/products" [class.warn]="s.lowStockCount > 0">
          <div class="stat-label">Заканчиваются</div>
          <div class="stat-value">{{ s.lowStockCount }}</div>
          <div class="muted small">остаток меньше 5 шт.</div>
        </a>
        <a class="card stat" routerLink="/admin/orders">
          <div class="stat-label">Заказов</div>
          <div class="stat-value">{{ s.ordersCount }}</div>
          <div class="muted small">новых: {{ s.ordersByStatus['NEW'] ?? 0 }}</div>
        </a>
        <div class="card stat">
          <div class="stat-label">Выручка</div>
          <div class="stat-value">{{ s.revenue | byn }}</div>
          <div class="muted small">по доставленным заказам</div>
        </div>
        <div class="card stat">
          <div class="stat-label">Покупателей</div>
          <div class="stat-value">{{ s.customersCount }}</div>
        </div>
      </div>

      <section class="card section">
        <h3>Заказы по статусам</h3>
        <div class="bars">
          @for (row of byStatus(); track row.status) {
            <a class="bar-row" routerLink="/admin/orders" [queryParams]="{ status: row.status }">
              <app-status-badge [status]="row.status" />
              <div class="bar-track">
                <div class="bar-fill status-{{ row.status.toLowerCase() }}" [style.width.%]="row.percent"></div>
              </div>
              <span class="bar-value">{{ row.count }}</span>
            </a>
          }
        </div>
      </section>
    } @else if (loading()) {
      <div class="stat-grid">
        @for (i of [1, 2, 3, 4]; track i) {
          <div class="card skeleton-card short"></div>
        }
      </div>
    }
  `,
})
export class DashboardComponent {
  private readonly statsService = inject(StatsService);
  protected readonly stats = signal<DashboardStats | null>(null);
  protected readonly loading = signal(true);

  protected readonly byStatus = computed(() => {
    const s = this.stats();
    if (!s) return [];
    const max = Math.max(1, ...ORDER_STATUSES.map((st) => s.ordersByStatus[st] ?? 0));
    return ORDER_STATUSES.map((status) => {
      const count = s.ordersByStatus[status] ?? 0;
      return { status, count, percent: (count / max) * 100 };
    });
  });

  constructor() {
    this.statsService.dashboard().subscribe({
      next: (s) => {
        this.stats.set(s);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
