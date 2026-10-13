import { Directive, ElementRef, HostListener, inject } from '@angular/core';
import { NgControl } from '@angular/forms';
import { formatBelarusPhone, nationalDigits } from '../utils/phone';

/**
 * Маска ввода белорусского номера: «+375 (XX) XXX-XX-XX».
 * При фокусе подставляет префикс +375, при потере фокуса без цифр — очищает поле.
 */
@Directive({
  selector: 'input[appPhoneMask]',
  host: { inputmode: 'tel', maxlength: '19' },
})
export class PhoneMaskDirective {
  private readonly el = inject<ElementRef<HTMLInputElement>>(ElementRef);
  private readonly control = inject(NgControl, { optional: true });

  @HostListener('focus')
  onFocus(): void {
    if (!this.el.nativeElement.value) this.set('+375 (');
  }

  @HostListener('input')
  onInput(): void {
    const input = this.el.nativeElement;
    const atEnd = input.selectionStart === input.value.length;
    const formatted = nationalDigits(input.value).length ? formatBelarusPhone(input.value) : '+375 (';
    this.set(formatted);
    if (atEnd) input.setSelectionRange(formatted.length, formatted.length);
  }

  @HostListener('blur')
  onBlur(): void {
    if (nationalDigits(this.el.nativeElement.value).length === 0) this.set('');
  }

  private set(value: string): void {
    this.el.nativeElement.value = value;
    this.control?.control?.setValue(value, { emitEvent: true });
  }
}
