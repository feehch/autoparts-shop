import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/auth.guards';
import { AdminLayoutComponent } from './admin-layout.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      {
        path: '',
        title: 'Управление — АвтоДеталь',
        loadComponent: () => import('./dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'products',
        title: 'Товары — Управление',
        loadComponent: () => import('./products/admin-products.component').then((m) => m.AdminProductsComponent),
      },
      {
        path: 'products/new',
        title: 'Новый товар — Управление',
        loadComponent: () => import('./products/product-form.component').then((m) => m.ProductFormComponent),
      },
      {
        path: 'products/:id',
        title: 'Редактирование товара — Управление',
        loadComponent: () => import('./products/product-form.component').then((m) => m.ProductFormComponent),
      },
      {
        path: 'categories',
        title: 'Категории — Управление',
        loadComponent: () => import('./catalog/admin-categories.component').then((m) => m.AdminCategoriesComponent),
      },
      {
        path: 'brands',
        title: 'Бренды — Управление',
        loadComponent: () => import('./catalog/admin-brands.component').then((m) => m.AdminBrandsComponent),
      },
      {
        path: 'cars',
        title: 'Автомобили — Управление',
        loadComponent: () => import('./catalog/admin-cars.component').then((m) => m.AdminCarsComponent),
      },
      {
        path: 'orders',
        title: 'Заказы — Управление',
        loadComponent: () => import('./orders/admin-orders.component').then((m) => m.AdminOrdersComponent),
      },
      {
        path: 'orders/:id',
        title: 'Заказ — Управление',
        loadComponent: () => import('./orders/admin-order-details.component').then((m) => m.AdminOrderDetailsComponent),
      },
      {
        path: 'users',
        title: 'Пользователи — Управление',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./users/admin-users.component').then((m) => m.AdminUsersComponent),
      },
    ],
  },
];
