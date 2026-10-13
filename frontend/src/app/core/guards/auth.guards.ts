import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RoleName } from '../models/api.models';
import { AuthService } from '../services/auth.service';

/** Пускает только аутентифицированных пользователей. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? true : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/** Проверяет роли из route data `roles` с учётом иерархии ADMIN > MANAGER > CUSTOMER. */
export const roleGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  const roles = (route.data['roles'] as RoleName[] | undefined) ?? [];
  return auth.hasAnyRole(roles) ? true : router.createUrlTree(['/forbidden']);
};

/** Для страниц входа/регистрации: аутентифицированного пользователя отправляет в каталог. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? router.createUrlTree(['/catalog']) : true;
};
