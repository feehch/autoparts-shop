import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

const NO_REFRESH_URLS = ['/auth/login', '/auth/refresh', '/auth/register', '/auth/logout'];

function withToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}

/**
 * Добавляет access-токен к запросам к API. При ответе 401 один раз пытается обновить
 * пару токенов (единый запрос refresh для всех параллельных запросов) и повторяет исходный запрос.
 * Если обновление не удалось — выполняет локальный выход и перенаправляет на страницу входа.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notify = inject(NotificationService);

  const isApi = req.url.startsWith('/api');
  if (!isApi) return next(req);

  const skipRefresh = NO_REFRESH_URLS.some((u) => req.url.includes(u));

  return next(withToken(req, auth.accessToken)).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401 || skipRefresh) {
        return throwError(() => err);
      }
      if (!auth.refreshToken) {
        return throwError(() => err);
      }
      return auth.refresh().pipe(
        catchError((refreshErr: unknown) => {
          auth.clear();
          notify.info('Сессия истекла. Войдите снова.');
          router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
          return throwError(() => refreshErr);
        }),
        switchMap((r) => next(withToken(req, r.accessToken))),
      );
    }),
  );
};
