import { Pipe, PipeTransform } from '@angular/core';
import { formatBelarusPhone, nationalDigits } from '../utils/phone';

/** «+375291234567» → «+375 (29) 123-45-67». */
@Pipe({ name: 'phone' })
export class PhonePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '';
    return nationalDigits(value).length === 9 ? formatBelarusPhone(value) : value;
  }
}
