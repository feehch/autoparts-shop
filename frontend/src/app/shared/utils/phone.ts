import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Коды мобильных операторов (25, 29, 33, 44) и городов (17 — Минск, 15x, 16x, 21x–23x). */
const AREA_CODES = ['17', '25', '29', '33', '44', '15', '16', '21', '22', '23'];

/** Национальная часть номера (9 цифр после 375), извлечённая из произвольной записи. */
export function nationalDigits(raw: string | null | undefined): string {
  let digits = (raw ?? '').replace(/\D/g, '');
  if (digits.startsWith('80') && digits.length === 11) digits = '375' + digits.slice(2);
  if (digits.startsWith('375')) digits = digits.slice(3);
  return digits.slice(0, 9);
}

/** Форматирует номер в вид «+375 (29) 123-45-67» (допускается неполный номер — для маски ввода). */
export function formatBelarusPhone(raw: string | null | undefined): string {
  const d = nationalDigits(raw);
  let out = '+375';
  if (d.length > 0) out += ' (' + d.slice(0, 2);
  if (d.length >= 2) out += ')';
  if (d.length > 2) out += ' ' + d.slice(2, 5);
  if (d.length > 5) out += '-' + d.slice(5, 7);
  if (d.length > 7) out += '-' + d.slice(7, 9);
  return out;
}

/** Значение для API: «+375XXXXXXXXX» или null, если номер не введён. */
export function phoneOrNull(raw: string | null | undefined): string | null {
  const d = nationalDigits(raw);
  return d.length === 0 ? null : '+375' + d;
}

/** Валидатор белорусского номера. Пустое значение (или только префикс +375) считается отсутствием номера. */
export function belarusPhoneValidator(control: AbstractControl): ValidationErrors | null {
  const d = nationalDigits(control.value);
  if (d.length === 0) return null;
  return d.length === 9 && AREA_CODES.includes(d.slice(0, 2)) ? null : { phone: true };
}

/** Обязательный белорусский номер. */
export function requiredBelarusPhone(control: AbstractControl): ValidationErrors | null {
  return nationalDigits(control.value).length === 0 ? { required: true } : belarusPhoneValidator(control);
}
