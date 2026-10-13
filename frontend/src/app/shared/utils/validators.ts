import { AbstractControl, ValidationErrors } from '@angular/forms';


/** Валидатор совпадения пароля и подтверждения (на уровне группы). */
export function passwordsMatch(passKey: string, confirmKey: string) {
  return (group: AbstractControl): ValidationErrors | null => {
    const pass = group.get(passKey);
    const confirm = group.get(confirmKey);
    if (!pass || !confirm) return null;
    const mismatch = !!confirm.value && pass.value !== confirm.value;
    const errors = { ...(confirm.errors ?? {}) };
    if (mismatch) errors['mismatch'] = true;
    else delete errors['mismatch'];
    confirm.setErrors(Object.keys(errors).length ? errors : null, { emitEvent: false });
    return null;
  };
}
