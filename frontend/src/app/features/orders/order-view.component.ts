import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OrderDto } from '../../core/models/api.models';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { PaymentMethodPipe } from '../../shared/pipes/labels.pipes';
import { BynPipe } from '../../shared/pipes/byn.pipe';
import { PhonePipe } from '../../shared/pipes/phone.pipe';

/** Отображение заказа (используется в личном кабинете и в разделе управления). */
@Component({
  selector: 'app-order-view',
  imports: [DatePipe, RouterLink, BynPipe, PaymentMethodPipe, StatusBadgeComponent, PhonePipe],
  template: `
    @let o = order();
    <div class="order-grid">
      <section class="card section">
        <h3>Состав заказа</h3>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr><th>Товар</th><th>Артикул</th><th class="right">Цена</th><th class="right">Кол-во</th><th class="right">Сумма</th></tr>
            </thead>
            <tbody>
              @for (i of o.items; track i.productId) {
                <tr>
                  <td><a [routerLink]="['/products', i.productId]">{{ i.name }}</a></td>
                  <td class="mono small">{{ i.sku }}</td>
                  <td class="right">{{ i.price | byn }}</td>
                  <td class="right">{{ i.quantity }}</td>
                  <td class="right">{{ i.lineTotal | byn }}</td>
                </tr>
              }
            </tbody>
            <tfoot>
              <tr><td colspan="4" class="right strong">Итого</td><td class="right strong">{{ o.totalAmount | byn }}</td></tr>
            </tfoot>
          </table>
        </div>
      </section>
      <section class="card section">
        <h3>Информация</h3>
        <dl class="props">
          <dt>Статус</dt><dd><app-status-badge [status]="o.status" /></dd>
          <dt>Создан</dt><dd>{{ o.createdAt | date: 'dd.MM.yyyy HH:mm' }}</dd>
          <dt>Обновлён</dt><dd>{{ o.updatedAt | date: 'dd.MM.yyyy HH:mm' }}</dd>
          <dt>Покупатель</dt><dd>{{ o.customerName }}<br /><span class="muted small">{{ o.customerEmail }}</span></dd>
          <dt>Телефон</dt><dd>{{ o.contactPhone | phone }}</dd>
          <dt>Адрес</dt><dd>{{ o.deliveryAddress }}</dd>
          <dt>Оплата</dt><dd>{{ o.paymentMethod | paymentMethod }}</dd>
          @if (o.comment) {
            <dt>Комментарий</dt><dd class="pre-line">{{ o.comment }}</dd>
          }
        </dl>
      </section>
    </div>
  `,
})
export class OrderViewComponent {
  readonly order = input.required<OrderDto>();
}
