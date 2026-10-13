import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="admin-bar">
      <div class="container admin-nav">
        <span class="admin-title">Управление</span>
        <nav>
          <a routerLink="/admin" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Обзор</a>
          <a routerLink="/admin/products" routerLinkActive="active">Товары</a>
          <a routerLink="/admin/categories" routerLinkActive="active">Категории</a>
          <a routerLink="/admin/brands" routerLinkActive="active">Бренды</a>
          <a routerLink="/admin/cars" routerLinkActive="active">Автомобили</a>
          <a routerLink="/admin/orders" routerLinkActive="active">Заказы</a>
          @if (auth.isAdmin()) {
            <a routerLink="/admin/users" routerLinkActive="active">Пользователи</a>
          }
        </nav>
      </div>
    </div>
    <div class="container admin-content">
      <router-outlet />
    </div>
  `,
})
export class AdminLayoutComponent {
  protected readonly auth = inject(AuthService);
}
