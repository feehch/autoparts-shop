import { FormGroup } from '@angular/forms';
import { extractApiError } from '../../core/services/notification.service';

/** Переносит fieldErrors из ApiError в соответствующие контролы формы (ошибка `server`). */
export function applyServerErrors(form: FormGroup, err: unknown): void {
  const api = extractApiError(err);
  api?.fieldErrors?.forEach((fe) => {
    const control = form.get(fe.field);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), server: fe.message });
      control.markAsTouched();
    }
  });
}
