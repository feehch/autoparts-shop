import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';
import { SKIP_ERROR_TOAST } from './http-context';

/** Глобальная обработка ошибок HTTP: показывает ApiError.message во всплывающем уведомлении. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notify = inject(NotificationService);
  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && !req.context.get(SKIP_ERROR_TOAST)) {
        // 401 обрабатывается authInterceptor (refresh / редирект), кроме ошибок входа
        const isAuthCall = req.url.includes('/auth/login') || req.url.includes('/auth/register');
        if (err.status !== 401 || isAuthCall) {
          const fallback =
            err.status === 403 ? 'Недостаточно прав' : err.status === 404 ? 'Не найдено' : 'Произошла ошибка';
          notify.httpError(err, fallback);
        }
      }
      return throwError(() => err);
    }),
  );
};
