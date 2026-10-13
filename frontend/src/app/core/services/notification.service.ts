import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { ApiError } from '../models/api.models';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  type: ToastType;
  text: string;
  details?: string[];
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private seq = 0;
  readonly toasts = signal<Toast[]>([]);

  success(text: string): void {
    this.push('success', text);
  }

  info(text: string): void {
    this.push('info', text);
  }

  error(text: string, details?: string[]): void {
    this.push('error', text, details, 7000);
  }

  /** Показывает ошибку HTTP: ApiError.message и ошибки полей. */
  httpError(err: unknown, fallback = 'Произошла ошибка'): void {
    const api = extractApiError(err);
    if (api) {
      const details = api.fieldErrors?.map((f) => `${f.field}: ${f.message}`);
      this.error(api.message || fallback, details?.length ? details : undefined);
    } else if (err instanceof HttpErrorResponse && err.status === 0) {
      this.error('Сервер недоступен. Проверьте подключение.');
    } else {
      this.error(fallback);
    }
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  private push(type: ToastType, text: string, details?: string[], ttl = 4000): void {
    const id = ++this.seq;
    this.toasts.update((list) => [...list, { id, type, text, details }]);
    setTimeout(() => this.dismiss(id), ttl);
  }
}

export function extractApiError(err: unknown): ApiError | null {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object' && 'message' in err.error) {
    return err.error as ApiError;
  }
  return null;
}
