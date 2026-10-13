import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-stock-badge',
  template: `<span class="badge {{ view().cls }}">{{ view().text }}</span>`,
})
export class StockBadgeComponent {
  readonly quantity = input.required<number>();
  readonly active = input(true);

  protected readonly view = computed(() => {
    const q = this.quantity();
    if (!this.active()) return { cls: 'badge-muted', text: 'Снят с продажи' };
    if (q <= 0) return { cls: 'badge-danger', text: 'Нет в наличии' };
    if (q < 5) return { cls: 'badge-warning', text: `Мало: ${q} шт.` };
    return { cls: 'badge-success', text: `В наличии: ${q} шт.` };
  });
}
