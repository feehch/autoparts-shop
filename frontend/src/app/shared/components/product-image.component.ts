import { Component, input, linkedSignal } from '@angular/core';

/** Изображение товара с заглушкой, если imageUrl отсутствует или не загрузилось. */
@Component({
  selector: 'app-product-image',
  template: `
    @if (src() && !failed()) {
      <img [src]="src()" [alt]="alt()" loading="lazy" (error)="failed.set(true)" />
    } @else {
      <div class="img-placeholder" [attr.aria-label]="alt()">
        <svg viewBox="0 0 24 24" width="40%" height="40%" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="9" />
          <circle cx="12" cy="12" r="3" />
          <path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M5.6 18.4l2.1-2.1M16.3 7.7l2.1-2.1" />
        </svg>
      </div>
    }
  `,
  host: { class: 'product-image' },
})
export class ProductImageComponent {
  readonly src = input<string | null>(null);
  readonly alt = input<string>('');
  /** Сбрасывается при смене src, чтобы новое изображение попыталось загрузиться. */
  protected readonly failed = linkedSignal({ source: this.src, computation: () => false });
}
