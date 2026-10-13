import { Pipe, PipeTransform } from '@angular/core';

const fmt = new Intl.NumberFormat('ru-BY', {
  style: 'currency',
  currency: 'BYN',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** Форматирует сумму в белорусских рублях: 1234.5 → «1 234,50 Br». */
@Pipe({ name: 'byn' })
export class BynPipe implements PipeTransform {
  transform(value: number | string | null | undefined): string {
    if (value === null || value === undefined || value === '') return '';
    return fmt.format(Number(value));
  }
}
