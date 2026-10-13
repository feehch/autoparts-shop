import { HttpContext, HttpContextToken } from '@angular/common/http';

/** Не показывать toast с ошибкой для запроса (компонент обрабатывает ошибку сам). */
export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);

export const silent = () => new HttpContext().set(SKIP_ERROR_TOAST, true);
