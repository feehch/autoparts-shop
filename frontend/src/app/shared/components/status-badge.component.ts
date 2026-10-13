import { Component, input } from '@angular/core';
import { OrderStatus } from '../../core/models/api.models';
import { OrderStatusPipe } from '../pipes/labels.pipes';

@Component({
  selector: 'app-status-badge',
  imports: [OrderStatusPipe],
  template: `<span class="badge status-{{ status().toLowerCase() }}">{{ status() | orderStatus }}</span>`,
})
export class StatusBadgeComponent {
  readonly status = input.required<OrderStatus>();
}
