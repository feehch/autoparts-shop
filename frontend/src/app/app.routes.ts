import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'catalog' },
  {
    path: 'catalog',
    title: 'Каталог — АвтоДеталь',
    loadComponent: () => import('./features/catalog/catalog.component').then((m) => m.CatalogComponent),
  },
  {
    path: 'products/:id',
    title: 'Товар — АвтоДеталь',
    loadComponent: () =>
      import('./features/catalog/product-details.component').then((m) => m.ProductDetailsComponent),
  },
  {
    path: 'login',
    title: 'Вход — АвтоДеталь',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    title: 'Регистрация — АвтоДеталь',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'cart',
    title: 'Корзина — АвтоДеталь',
    canActivate: [authGuard],
    loadComponent: () => import('./features/cart/cart.component').then((m) => m.CartComponent),
  },
  {
    path: 'checkout',
    title: 'Оформление заказа — АвтоДеталь',
    canActivate: [authGuard],
    loadComponent: () => import('./features/cart/checkout.component').then((m) => m.CheckoutComponent),
  },
  {
    path: 'orders',
    title: 'Мои заказы — АвтоДеталь',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/my-orders.component').then((m) => m.MyOrdersComponent),
  },
  {
    path: 'orders/:id',
    title: 'Заказ — АвтоДеталь',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/order-details.component').then((m) => m.OrderDetailsComponent),
  },
  {
    path: 'profile',
    title: 'Профиль — АвтоДеталь',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/profile.component').then((m) => m.ProfileComponent),
  },
  {
    path: 'admin',
    canActivate: [roleGuard],
    data: { roles: ['MANAGER'] },
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'forbidden',
    title: 'Доступ запрещён — АвтоДеталь',
    loadComponent: () => import('./features/errors/forbidden.component').then((m) => m.ForbiddenComponent),
  },
  {
    path: '**',
    title: 'Страница не найдена — АвтоДеталь',
    loadComponent: () => import('./features/errors/not-found.component').then((m) => m.NotFoundComponent),
  },
];
