import { Component, computed, input, output } from '@angular/core';

/** Пагинация. Страницы нумеруются с 0 (как в API). */
@Component({
  selector: 'app-pagination',
  template: `
    @if (totalPages() > 1) {
      <nav class="pagination" aria-label="Страницы">
        <button type="button" class="page-btn" [disabled]="page() === 0" (click)="go(page() - 1)">‹</button>
        @for (p of pages(); track $index) {
          @if (p === -1) {
            <span class="page-gap">…</span>
          } @else {
            <button type="button" class="page-btn" [class.active]="p === page()" (click)="go(p)">{{ p + 1 }}</button>
          }
        }
        <button type="button" class="page-btn" [disabled]="page() >= totalPages() - 1" (click)="go(page() + 1)">›</button>
      </nav>
    }
  `,
})
export class PaginationComponent {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly pageChange = output<number>();

  /** Номера страниц с многоточиями (-1). */
  readonly pages = computed(() => {
    const total = this.totalPages();
    const cur = this.page();
    const set = new Set<number>([0, total - 1, cur - 1, cur, cur + 1].filter((p) => p >= 0 && p < total));
    const sorted = [...set].sort((a, b) => a - b);
    const out: number[] = [];
    sorted.forEach((p, i) => {
      if (i > 0 && p - sorted[i - 1] > 1) out.push(-1);
      out.push(p);
    });
    return out;
  });

  go(p: number): void {
    if (p >= 0 && p < this.totalPages() && p !== this.page()) this.pageChange.emit(p);
  }
}
