import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

/** Выводит сообщение об ошибке валидации поля формы (после touched). */
@Component({
  selector: 'app-field-error',
  template: `
    @if (message(); as m) {
      <div class="field-error">{{ m }}</div>
    }
  `,
})
export class FieldErrorComponent {
  readonly control = input.required<AbstractControl | null>();

  message(): string | null {
    const c = this.control();
    if (!c || !c.errors || !(c.touched || c.dirty)) return null;
    const e = c.errors;
    if (e['required']) return 'Обязательное поле';
    if (e['email']) return 'Некорректный email';
    if (e['minlength']) return `Минимум ${e['minlength'].requiredLength} символов`;
    if (e['maxlength']) return `Максимум ${e['maxlength'].requiredLength} символов`;
    if (e['min']) return `Значение не меньше ${e['min'].min}`;
    if (e['max']) return `Значение не больше ${e['max'].max}`;
    if (e['phone']) return 'Белорусский номер: +375 (XX) XXX-XX-XX';
    if (e['pattern']) return 'Неверный формат';
    if (e['mismatch']) return 'Пароли не совпадают';
    if (e['server']) return e['server'];
    return 'Некорректное значение';
  }
}
